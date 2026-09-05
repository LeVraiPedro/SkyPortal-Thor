// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led.bifrost

import com.skyportalthor.app.portal.led.LedOutputFrame
import com.skyportalthor.app.portal.led.PortalRgb
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BifrostSessionTest {
    private val frame = LedOutputFrame(PortalRgb(10, 20, 30), PortalRgb(40, 50, 60), 128)

    @Test
    fun acceptanceRequiresExactRequestEcho() {
        assertEquals(BifrostReply.ACCEPTED_UNCONFIRMED, mapBifrostReply(0, "test-1", "test-1"))
        for (echo in listOf(null, "", "test-2")) {
            assertEquals(BifrostReply.NO_RESPONSE, mapBifrostReply(0, echo, "test-1"))
        }
        assertEquals(BifrostReply.NO_RESPONSE, mapBifrostReply(0, "", ""))
        assertEquals(BifrostReply.NO_RESPONSE, mapBifrostReply(0, "x".repeat(65), "x".repeat(65)))
    }

    @Test
    fun allApiRejectionsAreMappedWithoutInventingSuccess() {
        val expected = mapOf(
            -1 to BifrostReply.CONTROL_DISABLED,
            -2 to BifrostReply.VERSION_REJECTED,
            -3 to BifrostReply.INVALID_COMMAND,
            -4 to BifrostReply.UNAUTHORIZED,
            -5 to BifrostReply.RATE_LIMITED,
            -6 to BifrostReply.INVALID_COMMAND
        )
        for ((code, reply) in expected) assertEquals(reply, mapBifrostReply(code, null, "test"))
        for (code in listOf(BIFROST_NO_RESULT, 1, 42, -7)) {
            assertEquals(BifrostReply.NO_RESPONSE, mapBifrostReply(code, "test", "test"))
        }
    }

    @Test
    fun unchangedColorsStillRenewLeaseEveryFiveHundredMilliseconds() = runBlocking {
        val transport = FakeTransport()
        val session = BifrostSession(transport)
        for (now in listOf(0L, 1L, 499L, 500L, 999L, 1_000L)) session.tick(frame, now)
        assertEquals(listOf(frame, frame, frame), transport.frames)
        assertEquals(BifrostSessionState.ACCEPTED_UNCONFIRMED, session.status.value.state)
        assertTrue(session.status.value.message.contains("non confirmé"))
    }

    @Test
    fun changedFrameUsesNewestColorsWithoutBurstOrCatchUp() = runBlocking {
        val transport = FakeTransport()
        val session = BifrostSession(transport)
        val second = frame.copy(left = PortalRgb(255, 0, 0))
        val third = frame.copy(right = PortalRgb(0, 0, 255))
        session.tick(frame, 0)
        session.tick(second, 100)
        session.tick(third, 10_000)
        session.tick(second, 10_001)
        assertEquals(listOf(frame, third), transport.frames)
    }

    @Test
    fun everyRejectionBacksOffWithoutBlockingCleanup() = runBlocking {
        for (reply in BifrostReply.entries.filter { it != BifrostReply.ACCEPTED_UNCONFIRMED }) {
            val transport = FakeTransport().apply { displayReply = reply }
            val session = BifrostSession(transport)
            session.tick(frame, 0)
            session.tick(frame, 499)
            session.tick(frame, 4_999)
            assertEquals(1, transport.frames.size)
            session.tick(null, 4_999)
            assertEquals(1, transport.clears)
            session.tick(frame, 5_000)
            assertEquals(2, transport.frames.size)
        }
    }

    @Test
    fun successfulRetryClearsBackoff() = runBlocking {
        val transport = FakeTransport().apply { displayReply = BifrostReply.RATE_LIMITED }
        val session = BifrostSession(transport)
        session.tick(frame, 0)
        transport.displayReply = BifrostReply.ACCEPTED_UNCONFIRMED
        session.tick(frame, 5_000)
        session.tick(frame, 5_500)
        assertEquals(3, transport.frames.size)
        assertEquals(BifrostSessionState.ACCEPTED_UNCONFIRMED, session.status.value.state)
    }

    @Test
    fun absentOrUnsupportedBifrostNeverReceivesDisplayOrClear() = runBlocking {
        val states = mapOf(
            BifrostAvailability.NOT_INSTALLED to BifrostSessionState.NOT_INSTALLED,
            BifrostAvailability.UNSUPPORTED_VERSION to BifrostSessionState.UNSUPPORTED_VERSION,
            BifrostAvailability.UNAVAILABLE to BifrostSessionState.UNAVAILABLE
        )
        for ((available, state) in states) {
            val transport = FakeTransport().apply { availabilityValue = available }
            val session = BifrostSession(transport)
            session.tick(frame, 0)
            session.release()
            assertTrue(transport.frames.isEmpty())
            assertEquals(0, transport.clears)
            assertEquals(state, session.status.value.state)
        }
    }

    @Test
    fun disappearanceIsDetectedEvenBetweenHeartbeatsAndReleasesOnlyOnce() = runBlocking {
        val transport = FakeTransport()
        val session = BifrostSession(transport)
        session.tick(frame, 0)
        transport.availabilityValue = BifrostAvailability.NOT_INSTALLED
        session.tick(frame, 1)
        session.tick(frame, 2)
        assertEquals(1, transport.frames.size)
        assertEquals(1, transport.clears)
        assertEquals(BifrostSessionState.NOT_INSTALLED, session.status.value.state)
        transport.availabilityValue = BifrostAvailability.AVAILABLE
        session.tick(frame, 500)
        assertEquals(2, transport.frames.size)
    }

    @Test
    fun nullFramesAndRepeatedReleaseNeverClearWithoutDisplayAttempt() = runBlocking {
        val transport = FakeTransport()
        val session = BifrostSession(transport)
        session.tick(null, 0)
        session.release()
        session.tick(frame, 1)
        session.tick(null, 2)
        session.tick(null, 3)
        session.release()
        session.release()
        assertEquals(1, transport.clears)
        assertEquals(BifrostSessionState.RELEASED_UNCONFIRMED, session.status.value.state)
        assertTrue(session.status.value.message.contains("non confirmé"))
    }

    @Test
    fun timeoutOrTransportExceptionStillRequiresOneClear() = runBlocking {
        val transport = FakeTransport().apply { displayFailure = IllegalStateException("transport failed") }
        val session = BifrostSession(transport)
        session.tick(frame, 0)
        assertEquals(BifrostSessionState.NO_RESPONSE, session.status.value.state)
        session.release()
        session.release()
        assertEquals(1, transport.clears)
    }

    @Test
    fun cleanupFailureIsNotReportedAsSuccessfulRestorationOrRetriedForever() = runBlocking {
        val transport = FakeTransport().apply { clearReply = BifrostReply.NO_RESPONSE }
        val session = BifrostSession(transport)
        session.tick(frame, 0)
        session.release()
        session.release()
        assertEquals(1, transport.clears)
        assertEquals(BifrostSessionState.NO_RESPONSE, session.status.value.state)
    }

    @Test
    fun availabilityExceptionDegradesSafely() = runBlocking {
        val transport = FakeTransport().apply { availabilityFailure = IllegalStateException("not available") }
        val session = BifrostSession(transport)
        session.tick(frame, 0)
        assertEquals(BifrostSessionState.UNAVAILABLE, session.status.value.state)
        assertTrue(transport.frames.isEmpty())
    }

    @Test
    fun regressingClockCannotCauseBurst() = runBlocking {
        val transport = FakeTransport()
        val session = BifrostSession(transport)
        for (now in listOf(1_000L, 0L, 500L, 1_499L, 1_500L)) session.tick(frame, now)
        assertEquals(2, transport.frames.size)
    }

    @Test
    fun largeClockValuesDoNotOverflowRateLimit() = runBlocking {
        val transport = FakeTransport()
        val session = BifrostSession(transport)
        session.tick(frame, Long.MAX_VALUE - 500)
        session.tick(frame, Long.MAX_VALUE - 1)
        session.tick(frame, Long.MAX_VALUE)
        assertEquals(2, transport.frames.size)
    }

    @Test
    fun concurrentTicksAndReleaseAreSerialized() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val transport = FakeTransport().apply {
            beforeDisplay = { entered.complete(Unit); finish.await() }
        }
        val session = BifrostSession(transport)
        val first = launch(start = CoroutineStart.UNDISPATCHED) { session.tick(frame, 0) }
        entered.await()
        val second = launch(start = CoroutineStart.UNDISPATCHED) { session.tick(frame, 0) }
        val release = launch(start = CoroutineStart.UNDISPATCHED) { session.release() }
        assertEquals(0, transport.clears)
        assertFalse(second.isCompleted)
        assertFalse(release.isCompleted)
        finish.complete(Unit)
        first.join()
        second.join()
        release.join()
        assertEquals(1, transport.frames.size)
        assertEquals(1, transport.clears)
    }

    @Test
    fun cancelledDisplayPropagatesCancellationButFinallyCanClear() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val transport = FakeTransport().apply {
            beforeDisplay = { entered.complete(Unit); awaitCancellation() }
        }
        val session = BifrostSession(transport)
        var cancellationObserved = false
        val worker = launch {
            try {
                session.tick(frame, 0)
            } catch (cancelled: CancellationException) {
                cancellationObserved = true
                throw cancelled
            } finally {
                session.release()
            }
        }
        entered.await()
        worker.cancelAndJoin()
        assertTrue(cancellationObserved)
        assertEquals(1, transport.clears)
        session.release()
        assertEquals(1, transport.clears)
    }

    private class FakeTransport : BifrostTransport {
        var availabilityValue = BifrostAvailability.AVAILABLE
        var availabilityFailure: Exception? = null
        var displayReply = BifrostReply.ACCEPTED_UNCONFIRMED
        var clearReply = BifrostReply.ACCEPTED_UNCONFIRMED
        var displayFailure: Exception? = null
        var beforeDisplay: suspend () -> Unit = {}
        val frames = mutableListOf<LedOutputFrame>()
        var clears = 0

        override fun availability(): BifrostAvailability {
            availabilityFailure?.let { throw it }
            return availabilityValue
        }

        override suspend fun display(frame: LedOutputFrame): BifrostReply {
            frames += frame
            beforeDisplay()
            displayFailure?.let { throw it }
            return displayReply
        }

        override suspend fun clear(): BifrostReply {
            clears++
            return clearReply
        }
    }
}

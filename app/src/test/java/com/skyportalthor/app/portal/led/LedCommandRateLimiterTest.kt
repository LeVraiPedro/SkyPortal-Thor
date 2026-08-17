// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led

import org.junit.Assert.assertEquals
import org.junit.Test

class LedCommandRateLimiterTest {
    private val red = LedOutputFrame(
        left = PortalRgb(255, 0, 0),
        right = PortalRgb(255, 0, 0)
    )
    private val blue = LedOutputFrame(
        left = PortalRgb(0, 0, 255),
        right = PortalRgb(0, 0, 255)
    )

    @Test
    fun firstFrameCanBeEmittedImmediately() {
        val limiter = LedCommandRateLimiter()

        assertEquals(LedEmissionDecision.Emit, limiter.evaluate(red, nowMillis = 1_000L))
    }

    @Test
    fun unchangedFrameIsSuppressedEvenAfterInterval() {
        val limiter = LedCommandRateLimiter()
        limiter.markEmitted(red, nowMillis = 1_000L)

        assertEquals(LedEmissionDecision.Unchanged, limiter.evaluate(red, nowMillis = 5_000L))
    }

    @Test
    fun changedFrameIsDeferredUntilIntervalExpires() {
        val limiter = LedCommandRateLimiter(minIntervalMillis = 250L)
        limiter.markEmitted(red, nowMillis = 1_000L)

        assertEquals(
            LedEmissionDecision.Deferred(retryAfterMillis = 150L),
            limiter.evaluate(blue, nowMillis = 1_100L)
        )
        assertEquals(LedEmissionDecision.Emit, limiter.evaluate(blue, nowMillis = 1_250L))
    }

    @Test
    fun repeatEventStillRespectsHardwareRateLimit() {
        val limiter = LedCommandRateLimiter(minIntervalMillis = 250L)
        limiter.markEmitted(red, nowMillis = 1_000L)

        assertEquals(
            LedEmissionDecision.Deferred(retryAfterMillis = 200L),
            limiter.evaluate(red, nowMillis = 1_050L, allowRepeat = true)
        )
        assertEquals(
            LedEmissionDecision.Emit,
            limiter.evaluate(red, nowMillis = 1_250L, allowRepeat = true)
        )
    }

    @Test
    fun resetAndClockRollbackAllowARecoveryEmission() {
        val limiter = LedCommandRateLimiter()
        limiter.markEmitted(red, nowMillis = 1_000L)

        assertEquals(LedEmissionDecision.Emit, limiter.evaluate(blue, nowMillis = 900L))

        limiter.reset()
        assertEquals(LedEmissionDecision.Emit, limiter.evaluate(red, nowMillis = 0L))
    }
}

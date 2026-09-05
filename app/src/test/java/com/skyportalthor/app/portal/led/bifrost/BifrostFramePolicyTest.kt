// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led.bifrost

import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.data.SmartPortalReadiness
import com.skyportalthor.app.portal.PortalState
import com.skyportalthor.app.portal.led.LedEffect
import com.skyportalthor.app.portal.led.PortalLedState
import com.skyportalthor.app.portal.led.PortalRgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Deterministic output-policy checks; no Android transport or physical LEDs are exercised. */
class BifrostFramePolicyTest {
    private val led = PortalLedState(
        schemaVersion = 1,
        active = true,
        sequence = 42,
        left = PortalRgb(250, 10, 20),
        right = PortalRgb(30, 40, 240),
        trap = PortalRgb(60, 180, 70)
    )
    private val ready = PortalState(
        connected = true,
        apiVersion = 4,
        readiness = SmartPortalReadiness.READY,
        emulationState = EmulationState.RUNNING,
        portalLedState = led
    )
    private val enabled = LightingSettings(enabled = true)

    @Test
    fun disabledByDefaultRequiresExplicitOptIn() {
        assertFalse(LightingSettings().enabled)
        assertNull(frame(settings = LightingSettings()))
        assertNotNull(frame())
    }

    @Test
    fun absentOrLegacyApiCannotProduceAnOutputFrame() {
        for (api in listOf(null, 1, 2, 3)) {
            assertNull("API $api", frame(ready.copy(apiVersion = api)))
        }
    }

    @Test
    fun disconnectedAndEveryNonReadyStateDisableOutput() {
        assertNull(frame(ready.copy(connected = false)))
        for (readiness in SmartPortalReadiness.entries.filter { it != SmartPortalReadiness.READY }) {
            assertNull(readiness.name, frame(ready.copy(readiness = readiness)))
        }
    }

    @Test
    fun pauseStopStartAndAbsentEmulationDisableOutput() {
        for (emulation in EmulationState.entries.filter { it != EmulationState.RUNNING }) {
            assertNull(emulation.name, frame(ready.copy(emulationState = emulation)))
        }
    }

    @Test
    fun absentOrInactivePortalLightsDisableOutputEvenWithFreshTimestamp() {
        assertNull(frame(ready.copy(portalLedState = null)))
        assertNull(frame(ready.copy(portalLedState = led.copy(active = false))))
    }

    @Test
    fun ledErrorsAndWarningsDisableOutputInsteadOfReplayingLastColors() {
        assertNull(frame(ready.copy(portalLedError = "Dolphin indisponible")))
        assertNull(frame(ready.copy(portalLedWarnings = listOf("État lumineux obsolète"))))
    }

    @Test
    fun freshnessAcceptsOnlyConfirmedMonotonicTimesUpToTheInclusiveLimit() {
        assertNull(frame(lastConfirmedAtMs = null))
        assertNull(frame(lastConfirmedAtMs = 10_001L))
        assertNull(frame(lastConfirmedAtMs = 8_499L))
        assertNotNull(frame(lastConfirmedAtMs = 8_500L))
        assertNotNull(frame(lastConfirmedAtMs = 9_999L))
        assertNotNull(frame(lastConfirmedAtMs = 10_000L))
        assertNotNull(frame(lastConfirmedAtMs = 0L, nowMs = 0L))
    }

    @Test
    fun brightnessIsConvertedWithoutChangingTheIndependentSourceColors() {
        for ((percent, intensity) in listOf(0 to 0, 35 to 89, 100 to 255)) {
            val output = requireNotNull(frame(settings = enabled.copy(brightnessPercent = percent)))

            assertEquals(intensity, output.intensity)
            assertEquals(led.left, output.left)
            assertEquals(led.right, output.right)
            assertEquals(LedEffect.STATIC, output.effect)
        }
    }

    @Test
    fun changingOneSideDoesNotAffectTheOtherAndTrapNeverDrivesJoystickOutput() {
        val original = requireNotNull(frame())
        val leftOnly = requireNotNull(frame(ready.copy(portalLedState = led.copy(left = PortalRgb.Black))))
        val rightOnly = requireNotNull(frame(ready.copy(portalLedState = led.copy(right = PortalRgb.Black))))

        assertEquals(PortalRgb.Black, leftOnly.left)
        assertEquals(original.right, leftOnly.right)
        assertEquals(original.left, rightOnly.left)
        assertEquals(PortalRgb.Black, rightOnly.right)
        assertEquals(original, frame(ready.copy(portalLedState = led.copy(trap = null))))
        assertEquals(original, frame(ready.copy(portalLedState = led.copy(trap = PortalRgb(255, 255, 255)))))
    }

    private fun frame(
        state: PortalState = ready,
        settings: LightingSettings = enabled,
        lastConfirmedAtMs: Long? = 10_000L,
        nowMs: Long = 10_000L
    ) = BifrostFramePolicy.frame(state, settings, lastConfirmedAtMs, nowMs)
}

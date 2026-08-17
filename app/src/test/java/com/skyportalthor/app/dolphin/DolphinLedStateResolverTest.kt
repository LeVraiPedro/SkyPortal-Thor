// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.dolphin

import com.skyportalthor.app.portal.led.PortalLedState
import com.skyportalthor.app.portal.led.PortalRgb
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DolphinLedStateResolverTest {
    @Test
    fun unavailableRuntimeClearsPreviousLedState() {
        val resolved = DolphinLedStateResolver.unavailable()

        assertNull(resolved.state)
        assertTrue(resolved.warnings.isEmpty())
        assertNull(resolved.error)
    }

    @Test
    fun apiThreeClearsCapabilityWithoutError() {
        val resolved = DolphinLedStateResolver.resolve(
            apiVersion = 3,
            current = state(sequence = 8)
        )

        assertNull(resolved.state)
        assertTrue(resolved.warnings.isEmpty())
        assertNull(resolved.error)
    }

    @Test
    fun apiFourAcceptsFirstPayload() {
        val resolved = DolphinLedStateResolver.resolve(
            apiVersion = 4,
            current = null,
            payloadJson = payload(sequence = 1, red = 90)
        )

        assertEquals(1L, resolved.state?.sequence)
        assertEquals(PortalRgb(90, 20, 30), resolved.state?.left)
        assertNull(resolved.error)
    }

    @Test
    fun stalePayloadPreservesNewerState() {
        val current = state(sequence = 10, red = 150)
        val resolved = DolphinLedStateResolver.resolve(
            apiVersion = 4,
            current = current,
            payloadJson = payload(sequence = 9, red = 20)
        )

        assertEquals(current, resolved.state)
        assertTrue(resolved.warnings.any { "obsolète" in it })
        assertNull(resolved.error)
    }

    @Test
    fun conflictingPayloadPreservesCurrentAndReportsError() {
        val current = state(sequence = 5, red = 100)
        val resolved = DolphinLedStateResolver.resolve(
            apiVersion = 4,
            current = current,
            payloadJson = payload(sequence = 5, red = 101)
        )

        assertEquals(current, resolved.state)
        assertTrue(resolved.error.orEmpty().contains("Conflit"))
    }

    @Test
    fun malformedPayloadPreservesCurrent() {
        val current = state(sequence = 3)
        val resolved = DolphinLedStateResolver.resolve(
            apiVersion = 4,
            current = current,
            payloadJson = "{bad-json"
        )

        assertEquals(current, resolved.state)
        assertTrue(resolved.error.orEmpty().contains("JSON"))
    }

    @Test
    fun transportFailurePreservesCurrent() {
        val current = state(sequence = 4)
        val resolved = DolphinLedStateResolver.resolve(
            apiVersion = 4,
            current = current,
            transportFailure = "DeadObjectException: service arrêté"
        )

        assertEquals(current, resolved.state)
        assertTrue(resolved.error.orEmpty().contains("DeadObjectException"))
    }

    private fun state(sequence: Long, red: Int = 10) = PortalLedState(
        schemaVersion = 1,
        active = true,
        sequence = sequence,
        left = PortalRgb(red, 20, 30),
        right = PortalRgb(40, 50, 60),
        trap = PortalRgb(70, 80, 90)
    )

    private fun payload(sequence: Long, red: Int): String = JSONObject()
        .put("schemaVersion", 1)
        .put("active", true)
        .put("sequence", sequence)
        .put("left", color(red, 20, 30))
        .put("right", color(40, 50, 60))
        .put("trap", color(70, 80, 90))
        .toString()

    private fun color(red: Int, green: Int, blue: Int) = JSONObject()
        .put("r", red)
        .put("g", green)
        .put("b", blue)
}

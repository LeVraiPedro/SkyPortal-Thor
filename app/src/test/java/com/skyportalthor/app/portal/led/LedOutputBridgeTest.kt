// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LedOutputBridgeTest {
    @Test
    fun activePortalStateCreatesAnOutputFrame() {
        val state = PortalLedState(
            schemaVersion = 1,
            active = true,
            sequence = 3,
            left = PortalRgb(10, 20, 30),
            right = PortalRgb(40, 50, 60)
        )

        val frame = LedOutputFrame.fromPortalState(
            state = state,
            intensity = 153,
            effect = LedEffect.FADE_TRANSITION
        )

        assertEquals(PortalRgb(10, 20, 30), frame?.left)
        assertEquals(PortalRgb(40, 50, 60), frame?.right)
        assertEquals(153, frame?.intensity)
        assertEquals(LedEffect.FADE_TRANSITION, frame?.effect)
    }

    @Test
    fun inactivePortalStateRequestsNoDisplayFrame() {
        assertNull(LedOutputFrame.fromPortalState(PortalLedState.off(sequence = 4)))
    }

    @Test
    fun noOpBridgeReportsUnavailableWithoutThrowing() {
        val bridge = NoOpLedOutputBridge("Bifrost absent")
        val frame = LedOutputFrame(PortalRgb.Black, PortalRgb.Black)

        assertEquals(LedOutputAvailability.UNSUPPORTED, bridge.availability())
        val display = bridge.display(frame)
        val clear = bridge.clear()
        assertTrue(display is LedOutputResult.Unavailable)
        assertTrue(clear is LedOutputResult.Unavailable)
        assertEquals("Bifrost absent", (display as LedOutputResult.Unavailable).message)
    }

    @Test
    fun rgbConversionIsStable() {
        val color = PortalRgb(0x12, 0x34, 0x56)

        assertEquals("#123456", color.toHex())
        assertEquals(0x7F123456, color.toArgb(alpha = 0x7F))
    }
}

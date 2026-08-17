// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui.portal

import com.skyportalthor.app.data.SmartPortalReadiness
import com.skyportalthor.app.portal.PortalState
import com.skyportalthor.app.portal.led.PortalLedState
import com.skyportalthor.app.portal.led.PortalRgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedPortalStateTest {
    @Test
    fun disconnectedStateUsesNeutralFallback() {
        val visual = AnimatedPortalStateMapper.from(PortalState())

        assertEquals(PortalVisualMode.DISCONNECTED, visual.mode)
        assertEquals(PortalVisualTone.NEUTRAL, visual.tone)
        assertFalse(visual.active)
        assertNull(visual.sequence)
    }

    @Test
    fun conflictOverridesAValidLedSnapshot() {
        val live = PortalLedState(
            schemaVersion = 1,
            active = true,
            sequence = 41,
            left = PortalRgb(1, 2, 3),
            right = PortalRgb(4, 5, 6),
            trap = PortalRgb(7, 8, 9)
        )
        val visual = AnimatedPortalStateMapper.from(
            PortalState(
                connected = true,
                apiVersion = 4,
                readiness = SmartPortalReadiness.PORTAL_CONFLICT,
                portalLedState = live
            )
        )

        assertEquals(PortalVisualMode.CONFLICT, visual.mode)
        assertEquals(PortalVisualTone.ERROR, visual.tone)
        assertTrue(visual.active)
        assertNull(visual.sequence)
        assertNull(visual.trap)
    }

    @Test
    fun apiThreeKeepsAReadableLegacyMode() {
        val visual = AnimatedPortalStateMapper.from(
            PortalState(
                connected = true,
                apiVersion = 3,
                readiness = SmartPortalReadiness.READY,
                portalEnabled = true
            )
        )

        assertEquals(PortalVisualMode.LEGACY, visual.mode)
        assertEquals(PortalVisualTone.SUCCESS, visual.tone)
        assertTrue(visual.active)
        assertTrue("API 3" in visual.detail)
    }

    @Test
    fun apiFourWaitsForTheFirstSnapshotWithoutFailingThePortal() {
        val visual = AnimatedPortalStateMapper.from(
            PortalState(
                connected = true,
                apiVersion = 4,
                readiness = SmartPortalReadiness.READY,
                portalEnabled = true
            )
        )

        assertEquals(PortalVisualMode.CONNECTING, visual.mode)
        assertEquals(PortalVisualTone.INFO, visual.tone)
        assertTrue(visual.active)
        assertNull(visual.sequence)
    }

    @Test
    fun activeApiFourSnapshotKeepsExactColorsAndSequence() {
        val led = PortalLedState(
            schemaVersion = 1,
            active = true,
            sequence = 72,
            left = PortalRgb(160, 64, 255),
            right = PortalRgb(12, 100, 220),
            trap = PortalRgb(255, 40, 0)
        )
        val visual = AnimatedPortalStateMapper.from(
            PortalState(
                connected = true,
                apiVersion = 4,
                readiness = SmartPortalReadiness.READY,
                portalEnabled = true,
                portalLedState = led
            )
        )

        assertEquals(PortalVisualMode.READY_ACTIVE, visual.mode)
        assertEquals(PortalVisualTone.SUCCESS, visual.tone)
        assertEquals(led.left, visual.left)
        assertEquals(led.right, visual.right)
        assertEquals(led.trap, visual.trap)
        assertEquals(72L, visual.sequence)
        assertTrue(visual.active)
    }

    @Test
    fun retainedSnapshotShowsTransportWarningWithoutLosingColors() {
        val led = PortalLedState(
            schemaVersion = 1,
            active = true,
            sequence = 12,
            left = PortalRgb(24, 48, 72),
            right = PortalRgb(80, 100, 120)
        )
        val visual = AnimatedPortalStateMapper.from(
            PortalState(
                connected = true,
                apiVersion = 4,
                readiness = SmartPortalReadiness.READY,
                portalEnabled = true,
                portalLedState = led,
                portalLedError = "lecture temporairement indisponible"
            )
        )

        assertEquals(PortalVisualMode.READY_ACTIVE, visual.mode)
        assertEquals(PortalVisualTone.WARNING, visual.tone)
        assertEquals(led.left, visual.left)
        assertEquals(led.right, visual.right)
        assertEquals("lecture temporairement indisponible", visual.warning)
    }

    @Test
    fun inactiveSnapshotProducesAStableIdlePortal() {
        val led = PortalLedState.off(sequence = 9)
        val visual = AnimatedPortalStateMapper.from(
            PortalState(
                connected = true,
                apiVersion = 4,
                readiness = SmartPortalReadiness.READY,
                portalEnabled = true,
                portalLedState = led
            )
        )

        assertEquals(PortalVisualMode.READY_IDLE, visual.mode)
        assertFalse(visual.active)
        assertFalse(visual.pulse)
        assertEquals(9L, visual.sequence)
    }
}

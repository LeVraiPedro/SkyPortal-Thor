package com.skyportalthor.app.portal

import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.data.SkylandersGame
import com.skyportalthor.app.data.SmartPortalReadiness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalStateReducerTest {
    @Test
    fun binderDeathClearsEveryRemoteFactAndLogicalSlot() {
        val connected = PortalState(
            connected = true,
            apiVersion = 3,
            connectedPackage = "org.dolphin.debug",
            serviceState = DolphinServiceState.READY,
            slots = List(8) { PortalSlotState(it, actualPortalSlot = it) },
            emulationState = EmulationState.RUNNING,
            gameId = "SSPP52",
            skylandersGame = SkylandersGame.SPYROS_ADVENTURE,
            portalEnabled = true,
            portalActivated = true,
            portalProtocolActivated = true,
            portalUsbPresent = true,
            portalUsbAttached = true,
            portalUsbHandshakeSeen = true,
            conflictingUsbDevices = listOf("DISNEY_INFINITY_BASE"),
            portalUsbStatusValid = true,
            portalRestartRequired = true,
            nativeSlots = listOf(NativePortalSlotState(0, true, 1, 16, 0))
        )

        val disconnected = PortalStateReducer.disconnected(
            connected,
            "Binder mort",
            SmartPortalReadiness.DOLPHIN_DETECTED,
            connected.connectedPackage
        )

        assertFalse(disconnected.connected)
        assertNull(disconnected.apiVersion)
        assertNull(disconnected.gameId)
        assertNull(disconnected.portalEnabled)
        assertNull(disconnected.portalProtocolActivated)
        assertNull(disconnected.portalUsbPresent)
        assertNull(disconnected.portalUsbAttached)
        assertNull(disconnected.portalUsbHandshakeSeen)
        assertTrue(disconnected.conflictingUsbDevices.isEmpty())
        assertFalse(disconnected.portalUsbStatusValid)
        assertFalse(disconnected.portalRestartRequired)
        assertTrue(disconnected.slots.all { it.actualPortalSlot == -1 })
        assertTrue(disconnected.nativeSlots.isEmpty())
        assertEquals(EmulationState.NONE, disconnected.emulationState)
        assertEquals(DolphinServiceState.UNKNOWN, disconnected.serviceState)
    }
}

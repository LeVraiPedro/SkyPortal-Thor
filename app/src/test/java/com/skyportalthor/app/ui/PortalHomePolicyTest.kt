// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui

import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.data.SmartPortalReadiness
import com.skyportalthor.app.portal.PortalState
import org.junit.Assert.*
import org.junit.Test

class PortalHomePolicyTest {
    private val ready = PortalState(connected = true, apiVersion = 4, readiness = SmartPortalReadiness.READY)

    @Test fun `solo never keeps a hidden player two target`() {
        assertEquals(0, PortalHomePolicy.selectedPlayer(1, false))
        assertEquals(1, PortalHomePolicy.selectedPlayer(1, true))
        assertEquals(0, PortalHomePolicy.selectedPlayer(9, true))
    }

    @Test fun `ready portal needs no technical action on home`() {
        val status = PortalHomePolicy.status(ready)
        assertTrue(status.isReady)
        assertEquals(HomeRecovery.NONE, status.recovery)
    }

    @Test fun `disconnect never keeps a ready label`() {
        val status = PortalHomePolicy.status(ready.copy(connected = false))
        assertFalse(status.isReady)
        assertEquals(HomeRecovery.RECONNECT, status.recovery)
    }

    @Test fun `conflict and error stay visible even with old ready snapshot`() {
        listOf(SmartPortalReadiness.PORTAL_CONFLICT, SmartPortalReadiness.ERROR,
            SmartPortalReadiness.PORTAL_RESTART_REQUIRED).forEach {
            val status = PortalHomePolicy.status(ready.copy(readiness = it))
            assertTrue(status.isError)
            assertFalse(status.isReady)
            assertEquals(HomeRecovery.HELP, status.recovery)
        }
    }

    @Test fun `activation only offered when API allows it`() {
        val disabled = ready.copy(readiness = SmartPortalReadiness.PORTAL_DISABLED)
        assertEquals(HomeRecovery.DOLPHIN, PortalHomePolicy.status(disabled).recovery)
        assertEquals(HomeRecovery.ACTIVATE, PortalHomePolicy.status(disabled.copy(canSetPortalEnabled = true)).recovery)
    }

    @Test fun `legacy APIs do not pretend portal verified`() {
        listOf(1, 2).forEach {
            val status = PortalHomePolicy.status(ready.copy(apiVersion = it))
            assertFalse(status.isReady)
            assertEquals(HomeRecovery.HELP, status.recovery)
        }
        assertTrue(PortalHomePolicy.status(ready.copy(apiVersion = 3)).isReady)
    }

    @Test fun `initializing service and no game are distinct from ready`() {
        assertFalse(PortalHomePolicy.status(ready.copy(serviceState = DolphinServiceState.INITIALIZING)).isReady)
        assertEquals(HomeRecovery.DOLPHIN, PortalHomePolicy.status(ready.copy(readiness = SmartPortalReadiness.NO_GAME)).recovery)
    }
}

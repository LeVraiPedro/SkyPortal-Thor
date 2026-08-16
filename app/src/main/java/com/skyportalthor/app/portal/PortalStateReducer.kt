// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal

import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.data.SmartPortalReadiness

internal object PortalStateReducer {
    fun disconnected(
        previous: PortalState,
        message: String,
        readiness: SmartPortalReadiness,
        connectedPackage: String?
    ): PortalState = previous.copy(
        connected = false,
        apiVersion = null,
        message = message,
        connectedPackage = connectedPackage,
        slots = List(previous.slots.size.coerceAtLeast(DEFAULT_LOGICAL_SLOTS)) { PortalSlotState(it) },
        readiness = readiness,
        serviceState = DolphinServiceState.UNKNOWN,
        emulationState = EmulationState.NONE,
        gameId = null,
        gameTitle = null,
        skylandersGame = null,
        portalEnabled = null,
        portalActivated = null,
        portalProtocolActivated = null,
        portalUsbPresent = null,
        portalUsbAttached = null,
        portalUsbHandshakeSeen = null,
        conflictingUsbDevices = emptyList(),
        portalUsbStatusValid = false,
        portalRestartRequired = false,
        canSetPortalEnabled = false,
        nativeSlotSchemaVersion = 0,
        nativeSlots = emptyList(),
        figureCatalog = emptyMap()
    )

    private const val DEFAULT_LOGICAL_SLOTS = 8
}

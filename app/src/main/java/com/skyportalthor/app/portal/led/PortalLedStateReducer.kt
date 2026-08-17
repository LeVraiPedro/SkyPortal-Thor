// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led

enum class PortalLedUpdateDisposition {
    INITIAL,
    ADVANCED,
    DUPLICATE,
    STALE,
    CONFLICT
}

data class PortalLedReduction(
    val state: PortalLedState,
    val disposition: PortalLedUpdateDisposition
)

object PortalLedStateReducer {
    fun reduce(current: PortalLedState?, incoming: PortalLedState): PortalLedReduction {
        if (current == null) {
            return PortalLedReduction(incoming, PortalLedUpdateDisposition.INITIAL)
        }

        return when {
            incoming.sequence > current.sequence ->
                PortalLedReduction(incoming, PortalLedUpdateDisposition.ADVANCED)

            incoming.sequence < current.sequence ->
                PortalLedReduction(current, PortalLedUpdateDisposition.STALE)

            incoming == current ->
                PortalLedReduction(current, PortalLedUpdateDisposition.DUPLICATE)

            else ->
                PortalLedReduction(current, PortalLedUpdateDisposition.CONFLICT)
        }
    }
}

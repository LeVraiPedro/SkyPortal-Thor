// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui.portal

import com.skyportalthor.app.portal.PortalSlotState
import com.skyportalthor.app.portal.PortalState

/**
 * Identity of the mount whose action sheet was opened. Presentation changes do not invalidate it.
 * This guards UI callbacks against an already-observed state change. Bridge validation and
 * serialization remain necessary; this check is not an atomic Binder compare-and-act operation.
 */
@ConsistentCopyVisibility
internal data class SlotActionTarget private constructor(
    val logicalSlot: Int,
    private val actualPortalSlot: Int,
    private val sourceUri: String?,
    private val figureId: Int?,
    private val variantId: Int?,
    private val fallbackLabel: String?,
    private val dolphinPackage: String?
) {
    fun resolve(state: PortalState): PortalSlotState? {
        if (!state.connected || state.connectedPackage != dolphinPackage) return null
        val slot = state.slots.singleOrNull { it.logicalSlot == logicalSlot } ?: return null
        return slot.takeIf { capture(state, it) == this }
    }

    companion object {
        fun capture(state: PortalState, slot: PortalSlotState): SlotActionTarget? {
            if (!state.connected) return null
            if (slot.actualPortalSlot < 0 && slot.figure == null && slot.label.isNullOrBlank()) {
                return null
            }
            val native = state.nativeSlots.singleOrNull {
                it.slot == slot.actualPortalSlot
            }
            if (native != null && !native.occupied) return null
            val figureId = native?.figureId ?: slot.figure?.figureId
            val variantId = native?.variantId ?: slot.figure?.variantId
            return SlotActionTarget(
                logicalSlot = slot.logicalSlot,
                actualPortalSlot = slot.actualPortalSlot,
                sourceUri = slot.sourceUri,
                figureId = figureId,
                variantId = variantId,
                fallbackLabel = slot.label.takeIf { slot.sourceUri == null && figureId == null },
                dolphinPackage = state.connectedPackage
            )
        }
    }
}

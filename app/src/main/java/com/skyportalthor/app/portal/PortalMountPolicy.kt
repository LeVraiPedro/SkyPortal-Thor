// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal

internal object PortalMountPolicy {
    fun unidentifiedMountReason(
        apiVersion: Int,
        requestedLogicalSlot: Int,
        logicalSlots: List<PortalSlotState>,
        nativeSlots: List<NativePortalSlotState>
    ): String? {
        val unidentifiedLogical = logicalSlots.firstOrNull { slot ->
            slot.logicalSlot != requestedLogicalSlot &&
                PortalProtocol.isValidActualSlot(slot.actualPortalSlot) &&
                slot.sourceUri == null
        }
        if (unidentifiedLogical != null) {
            return "Le slot ${unidentifiedLogical.logicalSlot + 1} est occupé, mais cette API Dolphin n’expose pas son fichier."
        }

        if (apiVersion >= 3) {
            val claimedNativeSlots = logicalSlots
                .mapNotNull { it.actualPortalSlot.takeIf(PortalProtocol::isValidActualSlot) }
                .toSet()
            val unclaimedNative = nativeSlots.firstOrNull { it.occupied && it.slot !in claimedNativeSlots }
            if (unclaimedNative != null) {
                return "Le slot natif #${unclaimedNative.slot} est occupé directement dans le gestionnaire Dolphin."
            }
        }
        return null
    }
}

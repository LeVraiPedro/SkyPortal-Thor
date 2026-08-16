// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal

import org.junit.Assert.assertEquals
import org.junit.Test

class PortalTargetSwitchPolicyTest {
    @Test
    fun targetSwitchRequiresVerifiedClearForReliableApi3Ownership() {
        assertEquals(
            PortalTargetSwitchDecision.CLEAR_AND_VERIFY,
            PortalTargetSwitchPolicy.decide(
                apiVersion = 3,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                hasOwnedUris = true,
                logicalSlots = listOf(PortalSlotState(0, actualPortalSlot = 2, sourceUri = "content://fixture/spyro")),
                nativeSlots = listOf(NativePortalSlotState(2, true, 2, 16, 0))
            )
        )
    }

    @Test
    fun targetSwitchRefusesUnverifiableOwnershipButAllowsEmptyTarget() {
        assertEquals(
            PortalTargetSwitchDecision.REFUSE_UNVERIFIED,
            PortalTargetSwitchPolicy.decide(
                apiVersion = 3,
                nativeSlotSchemaVersion = 0,
                hasOwnedUris = true,
                logicalSlots = emptyList(),
                nativeSlots = emptyList()
            )
        )
        assertEquals(
            PortalTargetSwitchDecision.REFUSE_UNVERIFIED,
            PortalTargetSwitchPolicy.decide(
                apiVersion = 2,
                nativeSlotSchemaVersion = 0,
                hasOwnedUris = false,
                logicalSlots = listOf(PortalSlotState(0, actualPortalSlot = 4)),
                nativeSlots = emptyList()
            )
        )
        assertEquals(
            PortalTargetSwitchDecision.REFUSE_UNVERIFIED,
            PortalTargetSwitchPolicy.decide(
                apiVersion = 2,
                nativeSlotSchemaVersion = 0,
                hasOwnedUris = false,
                logicalSlots = List(8) { PortalSlotState(it) },
                nativeSlots = emptyList()
            )
        )
        assertEquals(
            PortalTargetSwitchDecision.SWITCH_WITHOUT_CLEAR,
            PortalTargetSwitchPolicy.decide(
                apiVersion = 3,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                hasOwnedUris = false,
                logicalSlots = List(8) { PortalSlotState(it) },
                nativeSlots = List(PortalProtocol.MAX_PORTAL_SLOTS) {
                    NativePortalSlotState(it, false, 0)
                }
            )
        )
    }
}

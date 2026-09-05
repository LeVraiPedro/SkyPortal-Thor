// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui.portal

import com.skyportalthor.app.portal.NativePortalSlotState
import com.skyportalthor.app.portal.PortalSlotState
import com.skyportalthor.app.portal.PortalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure UI-identity tests, not an atomic Binder-operation or Compose-lifecycle test. */
class SlotActionTargetTest {
    private val mounted = PortalSlotState(
        logicalSlot = 0,
        actualPortalSlot = 3,
        label = "Spyro",
        sourceUri = "content://test-fixtures/spyro.sky"
    )
    private val native = NativePortalSlotState(3, true, 1, 16, 0)
    private val connected = PortalState(
        connected = true,
        connectedPackage = "org.dolphinemu.dolphinemu",
        slots = listOf(mounted),
        nativeSlots = listOf(native)
    )

    @Test
    fun disconnectOrDolphinTargetChangeInvalidatesActions() {
        val target = requireNotNull(SlotActionTarget.capture(connected, mounted))

        assertNull(target.resolve(connected.copy(connected = false)))
        assertNull(target.resolve(connected.copy(connectedPackage = "org.dolphinemu.dolphinemu.debug")))
        assertNull(SlotActionTarget.capture(connected.copy(connected = false), mounted))
    }

    @Test
    fun removalOrMissingLogicalSlotInvalidatesActions() {
        val target = requireNotNull(SlotActionTarget.capture(connected, mounted))

        assertNull(target.resolve(connected.copy(slots = listOf(PortalSlotState(0)))))
        assertNull(target.resolve(connected.copy(slots = emptyList())))
        assertNull(target.resolve(connected.copy(nativeSlots = listOf(native.copy(occupied = false)))))
        assertNull(SlotActionTarget.capture(connected, PortalSlotState(0)))
    }

    @Test
    fun differentFileOrNativeMappingInvalidatesActionsEvenWithSameName() {
        val target = requireNotNull(SlotActionTarget.capture(connected, mounted))

        assertNull(target.resolve(connected.copy(slots = listOf(mounted.copy(
            sourceUri = "content://test-fixtures/another-spyro.sky"
        )))))
        assertNull(target.resolve(connected.copy(slots = listOf(mounted.copy(actualPortalSlot = 4)))))
    }

    @Test
    fun nativeFigureAndVariantChangesInvalidateActions() {
        val target = requireNotNull(SlotActionTarget.capture(connected, mounted))

        assertNull(target.resolve(connected.copy(nativeSlots = listOf(native.copy(figureId = 17)))))
        assertNull(target.resolve(connected.copy(nativeSlots = listOf(native.copy(variantId = 0x1000)))))
    }

    @Test
    fun presentationAndProtocolStatusChangesKeepCurrentIdentity() {
        val target = requireNotNull(SlotActionTarget.capture(connected, mounted))
        val relabelled = mounted.copy(label = "Spyro — favori")
        val updated = connected.copy(
            slots = listOf(relabelled),
            nativeSlots = listOf(native.copy(status = 2)),
            message = "Portail prêt",
            portalLedWarnings = listOf("Test presentation change")
        )

        assertEquals(relabelled, target.resolve(updated))
    }

    @Test
    fun legacyApiWithoutNativeSnapshotUsesFileIdentity() {
        for (api in 1..2) {
            val legacy = connected.copy(apiVersion = api, nativeSlots = emptyList())
            val target = requireNotNull(SlotActionTarget.capture(legacy, mounted))

            assertEquals(mounted, target.resolve(legacy))
            assertNull(target.resolve(legacy.copy(slots = listOf(mounted.copy(
                sourceUri = "content://test-fixtures/replacement.sky"
            )))))
        }
    }

    @Test
    fun unknownSourceCanUseNativeIdentityWithoutInventingAFile() {
        val unknownSource = mounted.copy(sourceUri = null)
        val state = connected.copy(slots = listOf(unknownSource))
        val target = requireNotNull(SlotActionTarget.capture(state, unknownSource))

        assertNotNull(target.resolve(state.copy(slots = listOf(unknownSource.copy(label = "Dolphin: Spyro")))))
        assertNull(target.resolve(state.copy(nativeSlots = listOf(native.copy(figureId = 17)))))
    }

    @Test
    fun unidentifiedLegacyMountFallsBackToItsLabel() {
        val unknown = mounted.copy(sourceUri = null)
        val legacy = connected.copy(slots = listOf(unknown), nativeSlots = emptyList())
        val target = requireNotNull(SlotActionTarget.capture(legacy, unknown))

        assertNotNull(target.resolve(legacy))
        assertNull(target.resolve(legacy.copy(slots = listOf(unknown.copy(label = "Autre personnage")))))
    }
}

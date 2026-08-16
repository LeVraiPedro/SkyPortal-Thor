// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.dolphin

import com.skyportalthor.app.portal.PortalProtocol
import com.skyportalthor.app.portal.PortalSlotState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSlotTransitionContractTest {
    @Test
    fun mountedReplacementIsConfirmedAcrossProtocolTransitions() {
        listOf(REMOVED, READY, REMOVING, ADDED).forEach { protocolStatus ->
            val snapshot = DolphinStatusParser.parse(statusJson(protocolStatus, mounted = true))
            val native = snapshot.nativeSlots.single { it.slot == ACTUAL_SLOT }

            assertTrue("status=$protocolStatus", native.occupied)
            assertEquals(protocolStatus, native.status)
            assertTrue(
                "A mounted replacement must not disappear while protocol status=$protocolStatus",
                PortalProtocol.isConfirmedLoad(
                    apiVersion = 3,
                    refreshSucceeded = true,
                    expectedActualSlot = ACTUAL_SLOT,
                    logicalActualSlot = ACTUAL_SLOT,
                    nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                    nativeSnapshotSize = snapshot.nativeSlots.size,
                    nativeOccupied = native.occupied,
                    expectedFigureId = FIGURE_ID,
                    expectedVariantId = VARIANT_ID,
                    nativeFigureId = native.figureId,
                    nativeVariantId = native.variantId,
                    requirePortalReady = false,
                    portalReady = false
                )
            )
        }
    }

    @Test
    fun cleanupRequiresFileUnmountEvidenceInsteadOfProtocolStatus() {
        val stillMounted = DolphinStatusParser.parse(statusJson(REMOVING, mounted = true))
        val mountedNative = stillMounted.nativeSlots.single { it.slot == ACTUAL_SLOT }
        assertFalse(
            PortalProtocol.isConfirmedRemoval(
                apiVersion = 3,
                refreshSucceeded = true,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                expectedActualSlot = ACTUAL_SLOT,
                logicalActualSlot = -1,
                nativeSnapshotSize = stillMounted.nativeSlots.size,
                nativeOccupied = mountedNative.occupied
            )
        )

        val unmounted = DolphinStatusParser.parse(statusJson(REMOVING, mounted = false))
        val unmountedNative = unmounted.nativeSlots.single { it.slot == ACTUAL_SLOT }
        assertTrue(
            PortalProtocol.isConfirmedRemoval(
                apiVersion = 3,
                refreshSucceeded = true,
                expectedActualSlot = ACTUAL_SLOT,
                logicalActualSlot = -1,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                nativeSnapshotSize = unmounted.nativeSlots.size,
                nativeOccupied = unmountedNative.occupied
            )
        )
        assertFalse(
            PortalProtocol.isConfirmedRemoval(
                apiVersion = 3,
                refreshSucceeded = true,
                expectedActualSlot = ACTUAL_SLOT,
                logicalActualSlot = -1,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                nativeSnapshotSize = PortalProtocol.MAX_PORTAL_SLOTS - 1,
                nativeOccupied = false
            )
        )
    }

    @Test
    fun api3LoadCannotBeConfirmedFromAnIncompleteNativeSnapshot() {
        val snapshot = DolphinStatusParser.parse(statusJson(ADDED, mounted = true))
        val native = snapshot.nativeSlots.single { it.slot == ACTUAL_SLOT }

        assertFalse(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 3,
                refreshSucceeded = true,
                expectedActualSlot = ACTUAL_SLOT,
                logicalActualSlot = ACTUAL_SLOT,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                nativeSnapshotSize = PortalProtocol.MAX_PORTAL_SLOTS - 1,
                nativeOccupied = native.occupied,
                expectedFigureId = FIGURE_ID,
                expectedVariantId = VARIANT_ID,
                nativeFigureId = native.figureId,
                nativeVariantId = native.variantId,
                requirePortalReady = false,
                portalReady = false
            )
        )
    }

    @Test
    fun clearNeedsACompleteSnapshotWithEveryFileUnmounted() {
        val mounted = DolphinStatusParser.parse(statusJson(REMOVING, mounted = true))
        assertFalse(
            PortalProtocol.isConfirmedClear(
                apiVersion = 3,
                refreshSucceeded = true,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                logicalSlots = List(8) { PortalSlotState(it) },
                nativeSlots = mounted.nativeSlots
            )
        )

        val unmounted = DolphinStatusParser.parse(statusJson(REMOVING, mounted = false))
        assertTrue(
            PortalProtocol.isConfirmedClear(
                apiVersion = 3,
                refreshSucceeded = true,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                logicalSlots = List(8) { PortalSlotState(it) },
                nativeSlots = unmounted.nativeSlots
            )
        )
        assertFalse(
            PortalProtocol.isConfirmedClear(
                apiVersion = 3,
                refreshSucceeded = true,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                logicalSlots = List(8) { PortalSlotState(it) },
                nativeSlots = unmounted.nativeSlots.dropLast(1)
            )
        )
        assertFalse(
            PortalProtocol.isConfirmedClear(
                apiVersion = 3,
                refreshSucceeded = true,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                logicalSlots = listOf(PortalSlotState(0, actualPortalSlot = ACTUAL_SLOT)),
                nativeSlots = unmounted.nativeSlots
            )
        )
    }

    @Test
    fun api3PortalFullIsTreatedAsPotentiallyDestructiveReplacementFailure() {
        assertTrue(PortalProtocol.mayHaveRemovedPreviousMount(PortalProtocol.ERROR_PORTAL_FULL))
        assertFalse(PortalProtocol.mayHaveRemovedPreviousMount(PortalProtocol.NATIVE_NO_SLOT))
        assertFalse(PortalProtocol.mayHaveRemovedPreviousMount(PortalProtocol.ERROR_PORTAL_USB_NOT_READY))
    }

    @Test
    fun oldApi3CannotConfirmMountOwnershipButLegacyApisRemainSupported() {
        val legacyPayload = JSONObject(statusJson(READY, mounted = true))
        legacyPayload.remove("nativeSlotSchemaVersion")
        val snapshot = DolphinStatusParser.parse(legacyPayload.toString())
        val native = snapshot.nativeSlots.single { it.slot == ACTUAL_SLOT }

        assertEquals(0, snapshot.nativeSlotSchemaVersion)
        assertFalse(PortalProtocol.hasReliableNativeMountSchema(3, snapshot.nativeSlotSchemaVersion))
        assertTrue(PortalProtocol.hasReliableNativeMountSchema(2, 0))
        assertFalse(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 3,
                refreshSucceeded = true,
                expectedActualSlot = ACTUAL_SLOT,
                logicalActualSlot = ACTUAL_SLOT,
                nativeSlotSchemaVersion = snapshot.nativeSlotSchemaVersion,
                nativeSnapshotSize = snapshot.nativeSlots.size,
                nativeOccupied = native.occupied,
                expectedFigureId = FIGURE_ID,
                expectedVariantId = VARIANT_ID,
                nativeFigureId = native.figureId,
                nativeVariantId = native.variantId,
                requirePortalReady = false,
                portalReady = false
            )
        )
    }

    private fun statusJson(protocolStatus: Int, mounted: Boolean): String {
        val nativeSlots = JSONArray()
        repeat(PortalProtocol.MAX_PORTAL_SLOTS) { slot ->
            nativeSlots.put(
                JSONObject()
                    .put("slot", slot)
                    .put("occupied", slot == ACTUAL_SLOT && mounted)
                    .put("status", if (slot == ACTUAL_SLOT) protocolStatus else REMOVED)
                    .put("id", if (slot == ACTUAL_SLOT && mounted) FIGURE_ID else -1)
                    .put("variant", if (slot == ACTUAL_SLOT && mounted) VARIANT_ID else -1)
            )
        }
        return JSONObject()
            .put("apiVersion", 3)
            .put("nativeSlotSchemaVersion", PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA)
            .put(
                "slots",
                JSONArray().put(
                    JSONObject()
                        .put("logicalSlot", 0)
                        .put("actualSlot", ACTUAL_SLOT)
                        .put("uri", "content://fixture/replacement.sky")
                )
            )
            .put("nativeSlots", nativeSlots)
            .toString()
    }

    private companion object {
        const val REMOVED = 0
        const val READY = 1
        const val REMOVING = 2
        const val ADDED = 3
        const val ACTUAL_SLOT = 3
        const val FIGURE_ID = 16
        const val VARIANT_ID = 0
    }
}

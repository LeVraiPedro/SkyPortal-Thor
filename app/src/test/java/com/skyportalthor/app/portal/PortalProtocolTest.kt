// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalProtocolTest {
    @Test
    fun acceptsOnlyRealNativeSlots() {
        assertTrue(PortalProtocol.isValidActualSlot(0))
        assertTrue(PortalProtocol.isValidActualSlot(15))
        assertFalse(PortalProtocol.isValidActualSlot(-1))
        assertFalse(PortalProtocol.isValidActualSlot(16))
        assertFalse(PortalProtocol.isValidActualSlot(255))
    }

    @Test
    fun recognizesLegacyAndV3PortalFullSignals() {
        assertTrue(PortalProtocol.isPortalFull(255))
        assertTrue(PortalProtocol.isPortalFull(-6))
        assertFalse(PortalProtocol.isPortalFull(-2))
        assertFalse(PortalProtocol.isPortalFull(PortalProtocol.ERROR_UNIDENTIFIED_NATIVE_MOUNT))
    }

    @Test
    fun mapsDolphinUsbRaceGuardsToActionableFrenchErrors() {
        val notReady = PortalProtocol.usbLoadFailure(-12)
        val conflict = PortalProtocol.usbLoadFailure(-13)

        assertEquals("DOLPHIN_PORTAL_USB_NOT_READY", notReady?.diagnosticCode)
        assertTrue(notReady?.recoveryHint?.contains("relance le jeu") == true)
        assertEquals("DOLPHIN_CONFLICTING_USB_DEVICE", conflict?.diagnosticCode)
        assertTrue(conflict?.message?.contains("Disney Infinity") == true)
        assertNull(PortalProtocol.usbLoadFailure(-2))
    }

    @Test
    fun requiresARefreshedLogicalSlotForEveryApiAndNativeIdentityForApi3() {
        assertFalse(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 2,
                refreshSucceeded = false,
                expectedActualSlot = 3,
                logicalActualSlot = 3,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                nativeSnapshotSize = PortalProtocol.MAX_PORTAL_SLOTS,
                nativeOccupied = null,
                expectedFigureId = 10,
                expectedVariantId = 20,
                nativeFigureId = null,
                nativeVariantId = null,
                requirePortalReady = false,
                portalReady = false
            )
        )
        assertTrue(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 2,
                refreshSucceeded = true,
                expectedActualSlot = 3,
                logicalActualSlot = 3,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                nativeSnapshotSize = PortalProtocol.MAX_PORTAL_SLOTS,
                nativeOccupied = null,
                expectedFigureId = 10,
                expectedVariantId = 20,
                nativeFigureId = null,
                nativeVariantId = null,
                requirePortalReady = false,
                portalReady = false
            )
        )
        assertFalse(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 3,
                refreshSucceeded = true,
                expectedActualSlot = 3,
                logicalActualSlot = 3,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                nativeSnapshotSize = PortalProtocol.MAX_PORTAL_SLOTS,
                nativeOccupied = true,
                expectedFigureId = 10,
                expectedVariantId = 20,
                nativeFigureId = 10,
                nativeVariantId = 21,
                requirePortalReady = true,
                portalReady = true
            )
        )
        assertTrue(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 3,
                refreshSucceeded = true,
                expectedActualSlot = 3,
                logicalActualSlot = 3,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                nativeSnapshotSize = PortalProtocol.MAX_PORTAL_SLOTS,
                nativeOccupied = true,
                expectedFigureId = 10,
                expectedVariantId = 20,
                nativeFigureId = 10,
                nativeVariantId = 20,
                requirePortalReady = true,
                portalReady = true
            )
        )
    }

    @Test
    fun postLoadConfirmationRequiresPortalToRemainReadyForApi3Game() {
        assertFalse(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 3,
                refreshSucceeded = true,
                expectedActualSlot = 3,
                logicalActualSlot = 3,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                nativeSnapshotSize = PortalProtocol.MAX_PORTAL_SLOTS,
                nativeOccupied = true,
                expectedFigureId = 10,
                expectedVariantId = 20,
                nativeFigureId = 10,
                nativeVariantId = 20,
                requirePortalReady = true,
                portalReady = false
            )
        )
        assertTrue(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 3,
                refreshSucceeded = true,
                expectedActualSlot = 3,
                logicalActualSlot = 3,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                nativeSnapshotSize = PortalProtocol.MAX_PORTAL_SLOTS,
                nativeOccupied = true,
                expectedFigureId = 10,
                expectedVariantId = 20,
                nativeFigureId = 10,
                nativeVariantId = 20,
                requirePortalReady = false,
                portalReady = false
            )
        )
    }

    @Test
    fun gameAppearingDuringApi3PreloadRequiresReadyPortalBeforeSuccess() {
        val requirePortalReady = PortalProtocol.requiresPortalReadyAfterLoad(
            apiVersion = 3,
            skylandersGameDetectedBefore = false,
            skylandersGameDetectedAfter = true
        )

        assertTrue(requirePortalReady)
        assertFalse(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 3,
                refreshSucceeded = true,
                expectedActualSlot = 3,
                logicalActualSlot = 3,
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                nativeSnapshotSize = PortalProtocol.MAX_PORTAL_SLOTS,
                nativeOccupied = true,
                expectedFigureId = 10,
                expectedVariantId = 20,
                nativeFigureId = 10,
                nativeVariantId = 20,
                requirePortalReady = requirePortalReady,
                portalReady = false
            )
        )
    }

    @Test
    fun api2NeverRequiresUsbReadinessForLegacyPreload() {
        assertFalse(
            PortalProtocol.requiresPortalReadyAfterLoad(
                apiVersion = 2,
                skylandersGameDetectedBefore = false,
                skylandersGameDetectedAfter = true
            )
        )
    }

    @Test
    fun uncertainApi3MountNeedsCompleteNativeIdentityEvidence() {
        fun reconciled(
            snapshotSize: Int,
            occupied: Boolean?,
            nativeId: Int? = 10,
            nativeVariant: Int? = 20
        ) = PortalProtocol.isUncertainMountReconciled(
            apiVersion = 3,
            remoteActualSlot = 3,
            remoteUriWasReported = true,
            remoteUri = "content://fixture/spyro",
            expectedUri = "content://fixture/spyro",
            nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
            nativeSnapshotSize = snapshotSize,
            nativeOccupied = occupied,
            expectedFigureId = 10,
            expectedVariantId = 20,
            nativeFigureId = nativeId,
            nativeVariantId = nativeVariant
        )

        assertFalse(reconciled(snapshotSize = 0, occupied = null))
        assertFalse(reconciled(snapshotSize = 15, occupied = true))
        assertFalse(reconciled(snapshotSize = 16, occupied = false))
        assertFalse(reconciled(snapshotSize = 16, occupied = true, nativeVariant = 21))
        assertFalse(
            PortalProtocol.isUncertainMountReconciled(
                apiVersion = 3,
                remoteActualSlot = 3,
                remoteUriWasReported = true,
                remoteUri = "content://fixture/spyro",
                expectedUri = "content://fixture/spyro",
                nativeSlotSchemaVersion = PortalProtocol.RELIABLE_NATIVE_SLOT_SCHEMA,
                nativeSnapshotSize = 16,
                nativeOccupied = true,
                expectedFigureId = null,
                expectedVariantId = null,
                nativeFigureId = 10,
                nativeVariantId = 20
            )
        )
        assertTrue(reconciled(snapshotSize = 16, occupied = true))
    }

    @Test
    fun dispatchTimeoutMessageNeverClaimsAbsenceOrSafeCleanup() {
        val uncertain = PortalProtocol.dispatchedLoadFailure(mountReconciled = false)
        val mounted = PortalProtocol.dispatchedLoadFailure(mountReconciled = true)

        assertEquals("LOAD_DISPATCH_RESULT_UNCERTAIN", uncertain.diagnosticCode)
        assertTrue(uncertain.recoveryHint.contains("Retirer ou Vider"))
        assertEquals("LOAD_DISPATCH_RECONCILED_MOUNTED", mounted.diagnosticCode)
        assertTrue(mounted.message.contains("monté"))
    }

    @Test
    fun uncertainCleanupNeverUsesTheSafeRollbackMessage() {
        val uncertain = PortalProtocol.loadConfirmationFailure(
            cleanupConfirmed = false,
            portalReadinessRequired = true,
            portalReady = false
        )
        val rolledBack = PortalProtocol.loadConfirmationFailure(
            cleanupConfirmed = true,
            portalReadinessRequired = true,
            portalReady = false
        )

        assertEquals("LOAD_CLEANUP_UNCERTAIN", uncertain.diagnosticCode)
        assertTrue(uncertain.message.contains("incertain"))
        assertEquals("LOAD_PORTAL_NOT_READY_ROLLED_BACK", rolledBack.diagnosticCode)
        assertTrue(rolledBack.message.contains("annulée en sécurité"))
    }
}

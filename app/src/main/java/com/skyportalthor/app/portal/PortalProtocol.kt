// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal

internal object PortalProtocol {
    const val MAX_PORTAL_SLOTS = 16
    const val RELIABLE_NATIVE_SLOT_SCHEMA = 2
    const val NATIVE_NO_SLOT = 255
    const val ERROR_PORTAL_FULL = -6
    const val ERROR_DOLPHIN_NOT_READY = -10
    const val ERROR_UNIDENTIFIED_NATIVE_MOUNT = -11
    const val ERROR_PORTAL_USB_NOT_READY = -12
    const val ERROR_CONFLICTING_USB_DEVICE = -13

    fun isValidActualSlot(value: Int): Boolean = value in 0 until MAX_PORTAL_SLOTS

    fun isPortalFull(value: Int): Boolean = value == NATIVE_NO_SLOT || value == ERROR_PORTAL_FULL

    fun mayHaveRemovedPreviousMount(value: Int): Boolean = value == ERROR_PORTAL_FULL

    fun hasReliableNativeMountSchema(apiVersion: Int, nativeSlotSchemaVersion: Int): Boolean =
        apiVersion < 3 || nativeSlotSchemaVersion >= RELIABLE_NATIVE_SLOT_SCHEMA

    fun requiresPortalReadyAfterLoad(
        apiVersion: Int,
        skylandersGameDetectedBefore: Boolean,
        skylandersGameDetectedAfter: Boolean
    ): Boolean = apiVersion >= 3 &&
        (skylandersGameDetectedBefore || skylandersGameDetectedAfter)

    fun isUncertainMountReconciled(
        apiVersion: Int,
        remoteActualSlot: Int,
        remoteUriWasReported: Boolean,
        remoteUri: String?,
        expectedUri: String,
        nativeSlotSchemaVersion: Int,
        nativeSnapshotSize: Int,
        nativeOccupied: Boolean?,
        expectedFigureId: Int?,
        expectedVariantId: Int?,
        nativeFigureId: Int?,
        nativeVariantId: Int?
    ): Boolean {
        if (!isValidActualSlot(remoteActualSlot) || !remoteUriWasReported || remoteUri != expectedUri) {
            return false
        }
        if (apiVersion < 3) return true
        if (!hasReliableNativeMountSchema(apiVersion, nativeSlotSchemaVersion)) return false
        if (nativeSnapshotSize != MAX_PORTAL_SLOTS || nativeOccupied != true) return false
        return expectedFigureId != null && expectedVariantId != null &&
            nativeFigureId == expectedFigureId && nativeVariantId == expectedVariantId
    }

    fun usbLoadFailure(value: Int): PortalUsbLoadFailure? = when (value) {
        ERROR_PORTAL_USB_NOT_READY -> PortalUsbLoadFailure(
            message = "Le jeu n’a pas encore détecté le Portal of Power",
            diagnosticCode = "DOLPHIN_PORTAL_USB_NOT_READY",
            recoveryHint = "Arrête complètement l’émulation, vérifie le portail puis relance le jeu."
        )
        ERROR_CONFLICTING_USB_DEVICE -> PortalUsbLoadFailure(
            message = "La base Disney Infinity empêche le jeu de détecter le Portal of Power",
            diagnosticCode = "DOLPHIN_CONFLICTING_USB_DEVICE",
            recoveryHint = "Désactive la base Disney Infinity, arrête complètement l’émulation puis relance le jeu."
        )
        else -> null
    }

    fun dispatchedLoadFailure(mountReconciled: Boolean): LoadConfirmationFailure =
        if (mountReconciled) {
            LoadConfirmationFailure(
                message = "Dolphin a monté le fichier, mais la réponse du chargement a été perdue",
                diagnosticCode = "LOAD_DISPATCH_RECONCILED_MOUNTED",
                recoveryHint = "Vérifie le slot puis utilise Retirer avant toute nouvelle tentative."
            )
        } else {
            LoadConfirmationFailure(
                message = "Le résultat du chargement reste incertain",
                diagnosticCode = "LOAD_DISPATCH_RESULT_UNCERTAIN",
                recoveryHint = "Ne recharge pas ce fichier. Vérifie le slot puis utilise Retirer ou Vider le portail."
            )
        }

    fun knownMountRollbackFailure(cleanupConfirmed: Boolean): LoadConfirmationFailure =
        if (cleanupConfirmed) {
            LoadConfirmationFailure(
                message = "La connexion a changé ; le chargement a été annulé en sécurité",
                diagnosticCode = "LOAD_CONNECTION_CHANGED_ROLLED_BACK",
                recoveryHint = "Reconnecte Dolphin puis vérifie le portail avant de réessayer."
            )
        } else {
            LoadConfirmationFailure(
                message = "La connexion a changé et le retrait du fichier monté reste incertain",
                diagnosticCode = "LOAD_CONNECTION_CHANGED_CLEANUP_UNCERTAIN",
                recoveryHint = "Ne recharge pas ce fichier. Vérifie l’ancien portail puis utilise Retirer ou Vider."
            )
        }

    fun isConfirmedLoad(
        apiVersion: Int,
        refreshSucceeded: Boolean,
        expectedActualSlot: Int,
        logicalActualSlot: Int?,
        nativeSlotSchemaVersion: Int,
        nativeSnapshotSize: Int,
        nativeOccupied: Boolean?,
        expectedFigureId: Int,
        expectedVariantId: Int,
        nativeFigureId: Int?,
        nativeVariantId: Int?,
        requirePortalReady: Boolean,
        portalReady: Boolean
    ): Boolean {
        if (!refreshSucceeded || logicalActualSlot != expectedActualSlot) return false
        if (requirePortalReady && !portalReady) return false
        if (apiVersion < 3) return true
        return hasReliableNativeMountSchema(apiVersion, nativeSlotSchemaVersion) &&
            nativeSnapshotSize == MAX_PORTAL_SLOTS &&
            nativeOccupied == true &&
            nativeFigureId == expectedFigureId &&
            nativeVariantId == expectedVariantId
    }

    fun isConfirmedRemoval(
        apiVersion: Int,
        refreshSucceeded: Boolean,
        expectedActualSlot: Int,
        logicalActualSlot: Int?,
        nativeSlotSchemaVersion: Int,
        nativeSnapshotSize: Int,
        nativeOccupied: Boolean?
    ): Boolean {
        if (apiVersion < 3) return refreshSucceeded
        return refreshSucceeded &&
            hasReliableNativeMountSchema(apiVersion, nativeSlotSchemaVersion) &&
            isValidActualSlot(expectedActualSlot) &&
            !isValidActualSlot(logicalActualSlot ?: -1) &&
            nativeSnapshotSize == MAX_PORTAL_SLOTS &&
            nativeOccupied == false
    }

    fun isConfirmedClear(
        apiVersion: Int,
        refreshSucceeded: Boolean,
        nativeSlotSchemaVersion: Int,
        logicalSlots: List<PortalSlotState>,
        nativeSlots: List<NativePortalSlotState>
    ): Boolean {
        if (apiVersion < 3) return refreshSucceeded
        return refreshSucceeded &&
            hasReliableNativeMountSchema(apiVersion, nativeSlotSchemaVersion) &&
            nativeSlots.size == MAX_PORTAL_SLOTS &&
            logicalSlots.none { isValidActualSlot(it.actualPortalSlot) } &&
            nativeSlots.none { it.occupied }
    }

    fun loadConfirmationFailure(
        cleanupConfirmed: Boolean,
        portalReadinessRequired: Boolean,
        portalReady: Boolean
    ): LoadConfirmationFailure = when {
        !cleanupConfirmed -> LoadConfirmationFailure(
            message = "Le chargement n’est pas confirmé et le retrait de sécurité reste incertain",
            diagnosticCode = "LOAD_CLEANUP_UNCERTAIN",
            recoveryHint = "Ne recharge pas ce fichier. Attends la reconnexion, vérifie le slot puis utilise Retirer avant de réessayer."
        )
        portalReadinessRequired && !portalReady -> LoadConfirmationFailure(
            message = "Le portail USB n’était plus prêt après le chargement ; l’opération a été annulée en sécurité",
            diagnosticCode = "LOAD_PORTAL_NOT_READY_ROLLED_BACK",
            recoveryHint = "Rétablis l’état Portail prêt puis réessaie."
        )
        else -> LoadConfirmationFailure(
            message = "Dolphin n’a pas confirmé le chargement ; l’opération a été annulée en sécurité",
            diagnosticCode = "LOAD_NOT_CONFIRMED_ROLLED_BACK",
            recoveryHint = "Vérifie l’état du portail puis réessaie."
        )
    }
}

internal data class PortalUsbLoadFailure(
    val message: String,
    val diagnosticCode: String,
    val recoveryHint: String
)

internal data class LoadConfirmationFailure(
    val message: String,
    val diagnosticCode: String,
    val recoveryHint: String
)

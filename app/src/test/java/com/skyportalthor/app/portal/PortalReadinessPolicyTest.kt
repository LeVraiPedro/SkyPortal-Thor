// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal

import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.data.SkylandersGame
import com.skyportalthor.app.data.SmartPortalReadiness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalReadinessPolicyTest {
    @Test
    fun autoActivationRequiresTheNewVerifiedApi3UsbSchema() {
        fun allowed(validSchema: Boolean, conflicts: List<String> = emptyList()) =
            PortalReadinessPolicy.canAutoActivate(
                apiVersion = 3,
                readiness = SmartPortalReadiness.PORTAL_DISABLED,
                canSetPortalEnabled = true,
                portalUsbStatusValid = validSchema,
                conflictingUsbDevices = conflicts
            )

        assertFalse(allowed(validSchema = false))
        assertFalse(allowed(validSchema = true, conflicts = listOf("DISNEY_INFINITY_BASE")))
        assertTrue(allowed(validSchema = true))
        assertFalse(
            PortalReadinessPolicy.canAutoActivate(
                apiVersion = 2,
                readiness = SmartPortalReadiness.PORTAL_DISABLED,
                canSetPortalEnabled = true,
                portalUsbStatusValid = true,
                conflictingUsbDevices = emptyList()
            )
        )
    }

    @Test
    fun api3NeedsAllThreeConsistentUsbSignalsToBeReady() {
        val ready = decision(present = true, attached = true, handshake = true)

        assertEquals(SmartPortalReadiness.READY, ready.readiness)
        assertFalse(ready.restartRequired)
    }

    @Test
    fun oldApi3StatusIsUnverifiedEvenWhenLegacyActivatedIsImplicitlyTrue() {
        val decision = decision(present = null, attached = null, handshake = null)

        assertEquals(SmartPortalReadiness.PORTAL_UNVERIFIED, decision.readiness)
    }

    @Test
    fun impossibleHandshakeWithoutAttachIsNeverReady() {
        val decision = decision(present = true, attached = false, handshake = true)

        assertEquals(SmartPortalReadiness.PORTAL_UNVERIFIED, decision.readiness)
    }

    @Test
    fun partialApi3UsbPayloadIsUnverifiedRatherThanRestartRequired() {
        val decision = decision(present = true, attached = null, handshake = null)

        assertEquals(SmartPortalReadiness.PORTAL_UNVERIFIED, decision.readiness)
        assertFalse(decision.restartRequired)
    }

    @Test
    fun malformedSchemaCannotBecomeReadyEvenWithThreeTrueSignals() {
        val decision = PortalReadinessPolicy.evaluate(
            apiVersion = 3,
            serviceState = DolphinServiceState.READY,
            emulationState = EmulationState.RUNNING,
            game = SkylandersGame.SPYROS_ADVENTURE,
            portalEnabled = true,
            portalUsbPresent = true,
            portalUsbAttached = true,
            portalUsbHandshakeSeen = true,
            conflictingUsbDevices = emptyList(),
            portalUsbStatusValid = false
        )

        assertEquals(SmartPortalReadiness.PORTAL_UNVERIFIED, decision.readiness)
    }

    @Test
    fun unknownServiceCanNeverBecomeReady() {
        val decision = PortalReadinessPolicy.evaluate(
            apiVersion = 3,
            serviceState = DolphinServiceState.UNKNOWN,
            emulationState = EmulationState.RUNNING,
            game = SkylandersGame.SPYROS_ADVENTURE,
            portalEnabled = true,
            portalUsbPresent = true,
            portalUsbAttached = true,
            portalUsbHandshakeSeen = true,
            conflictingUsbDevices = emptyList(),
            portalUsbStatusValid = true
        )

        assertEquals(SmartPortalReadiness.PORTAL_UNVERIFIED, decision.readiness)
    }

    @Test
    fun unknownEmulationCanNeverBecomeReady() {
        val decision = PortalReadinessPolicy.evaluate(
            apiVersion = 3,
            serviceState = DolphinServiceState.READY,
            emulationState = EmulationState.UNKNOWN,
            game = SkylandersGame.SPYROS_ADVENTURE,
            portalEnabled = true,
            portalUsbPresent = true,
            portalUsbAttached = true,
            portalUsbHandshakeSeen = true,
            conflictingUsbDevices = emptyList(),
            portalUsbStatusValid = true
        )

        assertEquals(SmartPortalReadiness.PORTAL_UNVERIFIED, decision.readiness)
    }

    @Test
    fun unknownEmulationIsUnverifiedEvenWhenGameMetadataIsMissing() {
        val decision = PortalReadinessPolicy.evaluate(
            apiVersion = 3,
            serviceState = DolphinServiceState.READY,
            emulationState = EmulationState.UNKNOWN,
            game = null,
            portalEnabled = true,
            portalUsbPresent = true,
            portalUsbAttached = true,
            portalUsbHandshakeSeen = true,
            conflictingUsbDevices = emptyList(),
            portalUsbStatusValid = true
        )

        assertEquals(SmartPortalReadiness.PORTAL_UNVERIFIED, decision.readiness)
    }

    @Test
    fun pausedEmulationMayBeReadyWithCompleteEvidence() {
        val decision = PortalReadinessPolicy.evaluate(
            apiVersion = 3,
            serviceState = DolphinServiceState.READY,
            emulationState = EmulationState.PAUSED,
            game = SkylandersGame.SPYROS_ADVENTURE,
            portalEnabled = true,
            portalUsbPresent = true,
            portalUsbAttached = true,
            portalUsbHandshakeSeen = true,
            conflictingUsbDevices = emptyList(),
            portalUsbStatusValid = true
        )

        assertEquals(SmartPortalReadiness.READY, decision.readiness)
    }

    @Test
    fun configuredPortalNotAttachedToRunningGameRequiresRestart() {
        val decision = decision(present = true, attached = false, handshake = false)

        assertEquals(SmartPortalReadiness.PORTAL_RESTART_REQUIRED, decision.readiness)
        assertTrue(decision.restartRequired)
    }

    @Test
    fun attachedPortalWaitsForProtocolHandshake() {
        val decision = decision(present = true, attached = true, handshake = false)

        assertEquals(SmartPortalReadiness.PORTAL_INITIALIZING, decision.readiness)
        assertFalse(decision.restartRequired)
    }

    @Test
    fun disneyInfinityConflictWinsOverOtherwiseValidEvidence() {
        val decision = decision(
            present = true,
            attached = true,
            handshake = true,
            conflicts = listOf("DISNEY_INFINITY_BASE")
        )

        assertEquals(SmartPortalReadiness.PORTAL_CONFLICT, decision.readiness)
        assertTrue(decision.restartRequired)
        assertEquals(
            "base Disney Infinity",
            PortalReadinessPolicy.conflictSummary(listOf("DISNEY_INFINITY_BASE"))
        )
    }

    @Test
    fun api3LoadGateBlocksBeforeNativeCallWhenPortalIsNotVerified() {
        val blocked = PortalReadinessPolicy.loadBlock(
            apiVersion = 3,
            gameDetected = true,
            readiness = SmartPortalReadiness.PORTAL_UNVERIFIED,
            conflictingUsbDevices = emptyList()
        )

        assertEquals("PORTAL_USB_UNVERIFIED", blocked?.diagnosticCode)
    }

    @Test
    fun conflictLoadGateNamesDisneyInfinityAndRequiresRestart() {
        val blocked = PortalReadinessPolicy.loadBlock(
            apiVersion = 3,
            gameDetected = true,
            readiness = SmartPortalReadiness.PORTAL_CONFLICT,
            conflictingUsbDevices = listOf("DISNEY_INFINITY_BASE")
        )

        assertTrue(blocked?.message?.contains("Disney Infinity") == true)
        assertTrue(blocked?.recoveryHint?.contains("relance le jeu") == true)
    }

    @Test
    fun legacyApi1And2KeepTheirDegradedLoadPath() {
        for (apiVersion in 1..2) {
            assertNull(
                PortalReadinessPolicy.loadBlock(
                    apiVersion = apiVersion,
                    gameDetected = true,
                    readiness = SmartPortalReadiness.PORTAL_UNVERIFIED,
                    conflictingUsbDevices = emptyList()
                )
            )
        }
    }

    @Test
    fun api2RunningGameIsExplicitlyUnverifiedButStillCompatible() {
        val decision = PortalReadinessPolicy.evaluate(
            apiVersion = 2,
            serviceState = DolphinServiceState.READY,
            emulationState = EmulationState.RUNNING,
            game = SkylandersGame.GIANTS,
            portalEnabled = null,
            portalUsbPresent = null,
            portalUsbAttached = null,
            portalUsbHandshakeSeen = null,
            conflictingUsbDevices = emptyList(),
            portalUsbStatusValid = false
        )

        assertEquals(SmartPortalReadiness.PORTAL_UNVERIFIED, decision.readiness)
    }

    @Test
    fun knownConflictIsReportedBeforeOfferingPortalActivation() {
        val decision = PortalReadinessPolicy.evaluate(
            apiVersion = 3,
            serviceState = DolphinServiceState.READY,
            emulationState = EmulationState.RUNNING,
            game = SkylandersGame.SPYROS_ADVENTURE,
            portalEnabled = false,
            portalUsbPresent = false,
            portalUsbAttached = false,
            portalUsbHandshakeSeen = false,
            conflictingUsbDevices = listOf("DISNEY_INFINITY_BASE"),
            portalUsbStatusValid = true
        )

        assertEquals(SmartPortalReadiness.PORTAL_CONFLICT, decision.readiness)
    }

    private fun decision(
        present: Boolean?,
        attached: Boolean?,
        handshake: Boolean?,
        conflicts: List<String> = emptyList()
    ): PortalReadinessDecision = PortalReadinessPolicy.evaluate(
        apiVersion = 3,
        serviceState = DolphinServiceState.READY,
        emulationState = EmulationState.RUNNING,
        game = SkylandersGame.SPYROS_ADVENTURE,
        portalEnabled = true,
        portalUsbPresent = present,
        portalUsbAttached = attached,
        portalUsbHandshakeSeen = handshake,
        conflictingUsbDevices = conflicts,
        portalUsbStatusValid = listOf(present, attached, handshake).all { it != null }
    )
}

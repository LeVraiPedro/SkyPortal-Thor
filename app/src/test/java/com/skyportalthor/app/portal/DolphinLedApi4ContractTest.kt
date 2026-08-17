// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DolphinLedApi4ContractTest {
    @Test
    fun dolphinServiceExportsVersionedLedStateAsApiFour() {
        val service = locate(
            "dolphin-patch/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/skyportal/SkyPortalService.kt",
            "../dolphin-patch/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/skyportal/SkyPortalService.kt"
        ).readText()

        assertTrue(service.contains("override fun getPortalLedStateJson()"))
        assertTrue(service.contains("SkylanderConfig.getPortalLedState()"))
        assertTrue(service.contains("const val API_VERSION = 4"))
        assertTrue(service.contains("PORTAL_LED_STATE_SIZE = 12"))
        assertTrue(service.contains("PORTAL_LED_SCHEMA_VERSION = 1"))
    }

    @Test
    fun incrementalNativePatchTracksAllPortalLedZonesAndSequence() {
        val patch = locate(
            "dolphin-patch/portal-led-api4.patch",
            "../dolphin-patch/portal-led-api4.patch"
        ).readText()

        assertTrue(patch.contains("GetLEDStateSnapshot"))
        assertTrue(patch.contains("m_led_sequence"))
        assertTrue(patch.contains("snapshot.left"))
        assertTrue(patch.contains("snapshot.right"))
        assertTrue(patch.contains("snapshot.trap"))
        assertTrue(patch.contains("side = 0x03"))
        assertTrue(patch.contains("side = 0x04"))
        assertTrue(patch.contains("side == 0x02 || side == 0x04"))
        assertTrue(patch.contains("LedSnapshotTracksOnlyVisibleChanges"))
    }

    @Test
    fun buildAndPatchToolsApplyApiFourAfterApiThreeBaseline() {
        val applyScript = locate(
            "tools/apply_dolphin_patch.py",
            "../tools/apply_dolphin_patch.py"
        ).readText()
        val coreIndex = applyScript.indexOf("smart-portal-core.patch")
        val ledIndex = applyScript.indexOf("portal-led-api4.patch")
        assertTrue(coreIndex >= 0 && ledIndex > coreIndex)
        assertTrue(applyScript.contains("SkyPortal API 4 patch applied successfully"))

        val workflow = locate(
            ".github/workflows/full-pair-build.yml",
            "../.github/workflows/full-pair-build.yml"
        ).readText()
        assertTrue(workflow.contains("SkyPortal_Thor_API4.apk"))
        assertTrue(workflow.contains("Dolphin_SkyPortal_API4.apk"))
        assertTrue(workflow.contains("portal-led-api4.patch"))
        assertFalse(workflow.contains("Dolphin_SkyPortal_API3"))
    }

    private fun locate(vararg candidates: String): File = candidates
        .asSequence()
        .map(::File)
        .firstOrNull(File::isFile)
        .also { assertTrue("Fichier API 4 introuvable depuis ${File(".").absolutePath}", it != null) }
        ?: error("Fichier API 4 introuvable")
}

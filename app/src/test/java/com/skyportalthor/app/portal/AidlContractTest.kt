// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AidlContractTest {
    @Test
    fun appAndDolphinCopiesAreIdenticalAndV4OnlyAppendsMethods() {
        val appAidl = locate(
            "app/src/main/aidl/com/skyportalthor/ipc/ISkylanderPortalService.aidl",
            "src/main/aidl/com/skyportalthor/ipc/ISkylanderPortalService.aidl"
        )
        val dolphinAidl = locate(
            "dolphin-patch/Source/Android/app/src/main/aidl/com/skyportalthor/ipc/ISkylanderPortalService.aidl",
            "../dolphin-patch/Source/Android/app/src/main/aidl/com/skyportalthor/ipc/ISkylanderPortalService.aidl"
        )
        val appText = normalize(appAidl.readText())
        val dolphinText = normalize(dolphinAidl.readText())
        assertEquals(appText, dolphinText)

        val methods = Regex("(?:int|boolean|void|String)\\s+(\\w+)\\s*\\(")
            .findAll(appText)
            .map { it.groupValues[1] }
            .toList()
        assertEquals(
            listOf(
                "getApiVersion",
                "ping",
                "load",
                "remove",
                "clear",
                "getStatusJson",
                "setPortalEnabled",
                "getFigureCatalogJson",
                "getPortalLedStateJson"
            ),
            methods
        )
    }

    @Test
    fun dolphinServiceReconcilesNativeIdentityBeforeUsingCachedSlots() {
        val service = locate(
            "dolphin-patch/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/skyportal/SkyPortalService.kt",
            "../dolphin-patch/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/skyportal/SkyPortalService.kt"
        ).readText()

        val removeStart = service.indexOf("override fun remove(logicalSlot: Int)")
        val loadBody = service.substring(service.indexOf("override fun load("), removeStart)
        val snapshotRead = loadBody.indexOf("val nativeSnapshot = SkylanderConfig.getPortalSnapshot()")
        val loadGuard = loadBody.indexOf("reconcileLogicalMappings(nativeSnapshot)")
        val unclaimedGuard = loadBody.indexOf("firstUnclaimedOccupiedNativeSlot(nativeSnapshot)")
        val previousSlotUse = loadBody.indexOf("val previousActual = logicalToActual[logicalSlot]")
        assertTrue(
            "Le remplacement doit valider le snapshot et les montages avant le slot précédent",
            snapshotRead >= 0 && snapshotRead < loadGuard && loadGuard < unclaimedGuard && unclaimedGuard < previousSlotUse
        )

        val clearStart = service.indexOf("override fun clear()")
        val removeBody = service.substring(removeStart, clearStart)
        assertTrue(removeBody.contains("val previouslyMapped = logicalToActual[logicalSlot]"))
        assertTrue(removeBody.contains("reconcileLogicalMappings(nativeSnapshot)"))
        assertTrue(removeBody.contains("firstUnclaimedOccupiedNativeSlot(nativeSnapshot)"))

        val toggleStart = service.indexOf("override fun setPortalEnabled")
        val clearBody = service.substring(clearStart, toggleStart)
        assertTrue(clearBody.contains("reconcileLogicalMappings(initialSnapshot)"))
        assertTrue(clearBody.contains("firstUnclaimedOccupiedNativeSlot(initialSnapshot)"))
        assertTrue(service.contains("figureIds[logical] == nativeId && variantIds[logical] == nativeVariant"))
        assertTrue(service.contains("firstUnclaimedOccupiedNativeSlot(nativeSnapshot)"))
        assertTrue(service.contains("ERROR_UNIDENTIFIED_NATIVE_MOUNT = -11"))
    }

    private fun locate(vararg candidates: String): File = candidates
        .asSequence()
        .map(::File)
        .firstOrNull(File::isFile)
        .also { assertTrue("Fichier AIDL introuvable depuis ${File(".").absolutePath}", it != null) }
        ?: error("AIDL introuvable")

    private fun normalize(value: String): String = value.replace("\r\n", "\n").trim()
}

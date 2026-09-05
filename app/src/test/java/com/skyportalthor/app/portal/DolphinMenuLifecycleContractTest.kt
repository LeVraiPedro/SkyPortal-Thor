// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// These are structural patch/distribution regression checks, not execution of Dolphin's
// native lifecycle or proof of hardware behavior. Native build and Thor tests remain required.
class DolphinMenuLifecycleContractTest {
    @Test
    fun allThreeMenuQueriesUseTheSameIdleOnlyReader() {
        val source = patchedExcerpt()
        for (query in listOf("isSystemMenuInstalled", "isSystemMenuvWii", "getSystemMenuVersion")) {
            val body = queryExcerpt(source, query)
            assertTrue("$query must use the guarded reader", body.contains("GetSystemMenuTMDWhenIdle()"))
            assertFalse("$query must not create its own IOS", body.contains("IOS::HLE::Kernel"))
        }
        assertEquals(3, Regex("const auto tmd = GetSystemMenuTMDWhenIdle\\(\\);").findAll(source).count())
        assertEquals(1, Regex("IOS::HLE::Kernel ios;").findAll(source).count())
    }

    @Test
    fun temporaryKernelRequiresAnIdleCheckBeforeAndInsideTheCoreGuard() {
        val source = patchedExcerpt()
        val helperStart = source.indexOf("static IOS::ES::TMDReader GetSystemMenuTMDWhenIdle()")
        assertTrue("Idle reader definition is missing", helperStart >= 0)
        val helper = source.substring(helperStart).substringBefore("extern \"C\"")
        val idleCheck = "if (!Core::IsUninitialized(system))"
        val fastCheck = helper.indexOf(idleCheck)
        val fastReturn = helper.indexOf("return {};", fastCheck)
        val guard = helper.indexOf("const Core::CPUThreadGuard guard(system);")
        val lockedCheck = helper.indexOf(idleCheck, fastCheck + idleCheck.length)
        val lockedReturn = helper.indexOf("return {};", lockedCheck)
        val kernel = helper.indexOf("IOS::HLE::Kernel ios;")
        val read = helper.indexOf("return ios.GetESCore().FindInstalledTMD(Titles::SYSTEM_MENU);")

        assertTrue("Busy fast path must return before taking the CPU guard", fastCheck >= 0 && fastReturn > fastCheck && guard > fastReturn)
        assertTrue("A boot racing the fast path must be rejected under the guard", lockedCheck > guard && lockedReturn > lockedCheck && kernel > lockedReturn)
        assertTrue("Read the TMD only after constructing the idle Kernel", read > kernel)
        assertFalse("Do not inspect an IOS being started or destroyed", helper.contains("GetIOS()"))
        assertFalse("Do not queue a menu query on an asynchronously changing CPU", helper.contains("RunOnCPUThread"))
        assertTrue(source.contains("#include \"Core/Core.h\""))
        assertTrue(source.contains("#include \"Core/System.h\""))
    }

    @Test
    fun anUnavailableTmdIsCheckedBeforeReadingItsFields() {
        val source = patchedExcerpt()
        assertTrue(queryExcerpt(source, "isSystemMenuInstalled").contains("return tmd.IsValid();"))
        assertTrue(
            "IsvWii reads raw TMD bytes and must short-circuit when the reader returns empty",
            queryExcerpt(source, "isSystemMenuvWii").contains("return tmd.IsValid() && tmd.IsvWii();")
        )
        assertTrue(queryExcerpt(source, "getSystemMenuVersion").contains("if (!tmd.IsValid())"))
    }

    @Test
    fun patchApplicationAndRebuildKitIncludeTheLifecycleCorrection() {
        val applyScript = locate("tools/apply_dolphin_patch.py").readText()
        val ledPatch = applyScript.indexOf("portal-led-api4.patch")
        val menuPatch = applyScript.indexOf("android-menu-lifecycle.patch")
        assertTrue("Apply the menu patch after the API 4 baseline", ledPatch >= 0 && menuPatch > ledPatch)
        assertTrue(applyScript.contains("apply_git_patch(repo, patch, label)"))

        val workflow = locate(".github/workflows/full-pair-build.yml").readText()
        assertTrue(workflow.contains("python3 tools/apply_dolphin_patch.py"))
        val sourceInfo = workflow.substringAfter("source_info=").substringBefore("full_parent=")
        assertTrue("Hash the lifecycle patch in corresponding-source information", sourceInfo.contains("dolphin-patch/android-menu-lifecycle.patch"))
        assertTrue(
            "Verify the lifecycle patch is present in the uploaded reconstruction kit",
            workflow.contains("Dolphin_SkyPortal_API4_Rebuild_Kit/dolphin-patch/android-menu-lifecycle.patch")
        )
    }

    // Keep only added and unchanged hunk lines; deleted unsafe code must not satisfy assertions.
    private fun patchedExcerpt(): String = locate("dolphin-patch/android-menu-lifecycle.patch")
        .readLines()
        .filter { it.startsWith(" ") || (it.startsWith("+") && !it.startsWith("+++")) }
        .joinToString("\n") { it.drop(1) }

    private fun queryExcerpt(source: String, query: String): String {
        val start = source.indexOf("Java_org_dolphinemu_dolphinemu_utils_WiiUtils_$query(")
        assertTrue("Missing patched JNI query: $query", start >= 0)
        return source.substring(start).substringBefore("\nJNIEXPORT")
    }

    private fun locate(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull(File::isFile)
        .also { assertTrue("Fichier de contrat Dolphin introuvable : $path", it != null) }
        ?: error("Fichier de contrat Dolphin introuvable : $path")
}

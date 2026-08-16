package com.skyportalthor.app.dolphin

import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.data.DolphinServiceState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DolphinStatusParserTest {
    @Test
    fun parsesApi1WithoutInventingUriOrSmartState() {
        val json = JSONObject()
            .put("slots", JSONArray().put(JSONObject().put("logicalSlot", 0).put("actualSlot", 3).put("label", "Spyro")))
            .toString()

        val parsed = DolphinStatusParser.parse(json)

        assertEquals(1, parsed.apiVersion)
        assertFalse(parsed.logicalSlots.single().uriWasReported)
        assertNull(parsed.portalEnabled)
        assertEquals(EmulationState.NONE, parsed.emulationState)
        assertEquals(DolphinServiceState.READY, parsed.serviceState)
    }

    @Test
    fun parsesApi2UriAndRejectsPortalFullAsARealSlot() {
        val json = JSONObject()
            .put("apiVersion", 2)
            .put(
                "slots",
                JSONArray()
                    .put(JSONObject().put("logicalSlot", 0).put("actualSlot", 2).put("uri", "content://fixture/spyro"))
                    .put(JSONObject().put("logicalSlot", 1).put("actualSlot", 255).put("uri", ""))
            )
            .toString()

        val parsed = DolphinStatusParser.parse(json)

        assertEquals("content://fixture/spyro", parsed.logicalSlots.first().sourceUri)
        assertTrue(parsed.issues.any { "255" in it })
    }

    @Test
    fun parsesApi3GamePortalAndAllSixteenNativeSlots() {
        val native = JSONArray()
        repeat(16) { slot ->
            native.put(
                JSONObject()
                    .put("slot", slot)
                    .put("occupied", slot == 4)
                    .put("status", if (slot == 4) 1 else 0)
                    .put("id", if (slot == 4) 16 else 0)
                    .put("variant", 0)
            )
        }
        val json = JSONObject()
            .put("apiVersion", 3)
            .put("nativeSlotSchemaVersion", 2)
            .put("slots", JSONArray())
            .put("nativeSlots", native)
            .put("emulationState", "RUNNING")
            .put("gameId", "SSPP52")
            .put("gameTitle", "Skylanders: Spyro's Adventure")
            .put("portalEnabled", true)
            .put("portalActivated", true)
            .put("portalProtocolActivated", true)
            .put("portalUsbPresent", true)
            .put("portalUsbAttached", true)
            .put("portalUsbHandshakeSeen", true)
            .put("conflictingUsbDevices", JSONArray())
            .put("canSetPortalEnabled", true)
            .toString()

        val parsed = DolphinStatusParser.parse(json)

        assertEquals(16, parsed.nativeSlots.size)
        assertEquals(2, parsed.nativeSlotSchemaVersion)
        assertEquals(EmulationState.RUNNING, parsed.emulationState)
        assertEquals(DolphinServiceState.READY, parsed.serviceState)
        assertEquals("SSPP52", parsed.gameId)
        assertEquals(true, parsed.portalEnabled)
        assertEquals(true, parsed.portalActivated)
        assertEquals(true, parsed.portalProtocolActivated)
        assertEquals(true, parsed.portalUsbPresent)
        assertEquals(true, parsed.portalUsbAttached)
        assertEquals(true, parsed.portalUsbHandshakeSeen)
        assertTrue(parsed.conflictingUsbDevices.isEmpty())
        assertTrue(parsed.portalUsbStatusValid)
        assertTrue(parsed.canSetPortalEnabled)
        assertTrue(parsed.issues.isEmpty())
    }

    @Test
    fun parsesInitializingApi3WithoutInventingPortalState() {
        val native = JSONArray()
        repeat(16) { slot ->
            native.put(JSONObject().put("slot", slot).put("occupied", false))
        }
        val json = JSONObject()
            .put("apiVersion", 3)
            .put("nativeSlotSchemaVersion", 2)
            .put("slots", JSONArray())
            .put("nativeSlots", native)
            .put("emulationState", "NONE")
            .put("portalEnabled", JSONObject.NULL)
            .put("portalActivated", JSONObject.NULL)
            .put("canSetPortalEnabled", false)
            .put("serviceState", "INITIALIZING")
            .toString()

        val parsed = DolphinStatusParser.parse(json)

        assertEquals(EmulationState.NONE, parsed.emulationState)
        assertEquals(DolphinServiceState.INITIALIZING, parsed.serviceState)
        assertNull(parsed.portalEnabled)
        assertNull(parsed.portalActivated)
        assertFalse(parsed.canSetPortalEnabled)
        assertEquals(16, parsed.nativeSlots.size)
        assertEquals(2, parsed.nativeSlotSchemaVersion)
        assertTrue(parsed.issues.isEmpty())
    }

    @Test
    fun reportsDuplicateAndIncompleteNativeSnapshots() {
        val native = JSONArray()
            .put(JSONObject().put("slot", 0).put("occupied", false))
            .put(JSONObject().put("slot", 0).put("occupied", true))
        val parsed = DolphinStatusParser.parse(
            JSONObject().put("apiVersion", 3).put("nativeSlots", native).toString()
        )

        assertEquals(1, parsed.nativeSlots.size)
        assertTrue(parsed.issues.any { "dupliqué" in it })
        assertTrue(parsed.issues.any { "incomplet" in it })
    }

    @Test
    fun oldApi3SnapshotKeepsUsbEvidenceUnknown() {
        val parsed = DolphinStatusParser.parse(
            JSONObject()
                .put("apiVersion", 3)
                .put("portalEnabled", true)
                .put("portalActivated", true)
                .toString()
        )

        assertNull(parsed.portalUsbPresent)
        assertNull(parsed.portalUsbAttached)
        assertNull(parsed.portalUsbHandshakeSeen)
        assertTrue(parsed.conflictingUsbDevices.isEmpty())
        assertFalse(parsed.portalUsbStatusValid)
        assertEquals(0, parsed.nativeSlotSchemaVersion)
    }

    @Test
    fun parsesAndNormalizesCompetingUsbDevices() {
        val parsed = DolphinStatusParser.parse(
            JSONObject()
                .put("apiVersion", 3)
                .put("conflictingUsbDevices", JSONArray()
                    .put("disney_infinity_base")
                    .put("DISNEY_INFINITY_BASE"))
                .toString()
        )

        assertEquals(listOf("DISNEY_INFINITY_BASE"), parsed.conflictingUsbDevices)
    }

    @Test
    fun reportsImpossibleUsbEvidenceInsteadOfTrustingIt() {
        val parsed = DolphinStatusParser.parse(
            JSONObject()
                .put("apiVersion", 3)
                .put("portalUsbPresent", false)
                .put("portalUsbAttached", false)
                .put("portalUsbHandshakeSeen", true)
                .toString()
        )

        assertTrue(parsed.issues.any { "handshake USB" in it })
    }

    @Test
    fun reportsPartialUsbEvidencePayload() {
        val parsed = DolphinStatusParser.parse(
            JSONObject()
                .put("apiVersion", 3)
                .put("portalUsbPresent", true)
                .toString()
        )

        assertTrue(parsed.issues.any { "état USB du portail incomplet" in it })
        assertFalse(parsed.portalUsbStatusValid)
    }

    @Test
    fun malformedConflictListInvalidatesOtherwiseCompleteUsbStatus() {
        val parsed = DolphinStatusParser.parse(
            JSONObject()
                .put("apiVersion", 3)
                .put("portalUsbPresent", true)
                .put("portalUsbAttached", true)
                .put("portalUsbHandshakeSeen", true)
                .put("conflictingUsbDevices", "DISNEY_INFINITY_BASE")
                .toString()
        )

        assertFalse(parsed.portalUsbStatusValid)
        assertTrue(parsed.issues.any { "schéma d’état USB" in it })
    }
}

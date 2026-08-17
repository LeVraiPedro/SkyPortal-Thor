// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalLedStateParserTest {
    @Test
    fun parsesIndependentColorsAndTrap() {
        val parsed = PortalLedStateParser.parse(
            JSONObject()
                .put("schemaVersion", 1)
                .put("active", true)
                .put("sequence", 42)
                .put("left", color(160, 64, 255))
                .put("right", color(12, 100, 220))
                .put("trap", color(255, 40, 0))
                .toString()
        )

        assertTrue(parsed is PortalLedParseResult.Success)
        parsed as PortalLedParseResult.Success
        assertEquals(42L, parsed.state.sequence)
        assertEquals(PortalRgb(160, 64, 255), parsed.state.left)
        assertEquals(PortalRgb(12, 100, 220), parsed.state.right)
        assertEquals(PortalRgb(255, 40, 0), parsed.state.trap)
        assertTrue(parsed.warnings.isEmpty())
    }

    @Test
    fun mirrorsLeftColorWhenRightIsAbsent() {
        val parsed = PortalLedStateParser.parse(
            JSONObject()
                .put("schemaVersion", 1)
                .put("active", true)
                .put("sequence", 7)
                .put("left", color(30, 60, 90))
                .toString()
        )

        assertTrue(parsed is PortalLedParseResult.Success)
        parsed as PortalLedParseResult.Success
        assertEquals(parsed.state.left, parsed.state.right)
        assertTrue(parsed.warnings.any { "couleur gauche" in it.lowercase() })
    }

    @Test
    fun offPayloadMayOmitAllColors() {
        val parsed = PortalLedStateParser.parse(
            JSONObject()
                .put("schemaVersion", 1)
                .put("active", false)
                .put("sequence", 9)
                .toString()
        )

        assertTrue(parsed is PortalLedParseResult.Success)
        parsed as PortalLedParseResult.Success
        assertEquals(PortalRgb.Black, parsed.state.left)
        assertEquals(PortalRgb.Black, parsed.state.right)
        assertEquals(null, parsed.state.trap)
        assertTrue(parsed.warnings.isEmpty())
    }

    @Test
    fun rejectsUnsupportedSchema() {
        val parsed = PortalLedStateParser.parse(
            JSONObject()
                .put("schemaVersion", 2)
                .put("active", false)
                .put("sequence", 0)
                .toString()
        )

        assertTrue(parsed is PortalLedParseResult.Failure)
        parsed as PortalLedParseResult.Failure
        assertEquals(PortalLedParseErrorCode.UNSUPPORTED_SCHEMA, parsed.code)
        assertEquals("schemaVersion", parsed.field)
    }

    @Test
    fun rejectsOutOfRangeColorChannel() {
        val parsed = PortalLedStateParser.parse(
            JSONObject()
                .put("schemaVersion", 1)
                .put("active", true)
                .put("sequence", 1)
                .put("left", color(300, 0, 0))
                .toString()
        )

        assertTrue(parsed is PortalLedParseResult.Failure)
        parsed as PortalLedParseResult.Failure
        assertEquals(PortalLedParseErrorCode.INVALID_FIELD, parsed.code)
        assertEquals("left.r", parsed.field)
    }

    @Test
    fun rejectsFractionalSequence() {
        val parsed = PortalLedStateParser.parse(
            JSONObject()
                .put("schemaVersion", 1)
                .put("active", false)
                .put("sequence", 1.5)
                .toString()
        )

        assertTrue(parsed is PortalLedParseResult.Failure)
        parsed as PortalLedParseResult.Failure
        assertEquals(PortalLedParseErrorCode.INVALID_FIELD, parsed.code)
        assertEquals("sequence", parsed.field)
    }

    @Test
    fun reportsMalformedJsonWithoutThrowing() {
        val parsed = PortalLedStateParser.parse("{not-json")

        assertTrue(parsed is PortalLedParseResult.Failure)
        parsed as PortalLedParseResult.Failure
        assertEquals(PortalLedParseErrorCode.MALFORMED_JSON, parsed.code)
    }

    private fun color(red: Int, green: Int, blue: Int): JSONObject = JSONObject()
        .put("r", red)
        .put("g", green)
        .put("b", blue)
}

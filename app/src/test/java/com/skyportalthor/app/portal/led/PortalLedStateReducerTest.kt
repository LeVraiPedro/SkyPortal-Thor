// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led

import org.junit.Assert.assertEquals
import org.junit.Test

class PortalLedStateReducerTest {
    @Test
    fun acceptsInitialAndNewerStates() {
        val initial = state(sequence = 1, red = 20)
        val first = PortalLedStateReducer.reduce(null, initial)
        val newer = state(sequence = 2, red = 40)
        val second = PortalLedStateReducer.reduce(first.state, newer)

        assertEquals(PortalLedUpdateDisposition.INITIAL, first.disposition)
        assertEquals(initial, first.state)
        assertEquals(PortalLedUpdateDisposition.ADVANCED, second.disposition)
        assertEquals(newer, second.state)
    }

    @Test
    fun ignoresStaleState() {
        val current = state(sequence = 8, red = 80)
        val stale = state(sequence = 7, red = 10)
        val result = PortalLedStateReducer.reduce(current, stale)

        assertEquals(PortalLedUpdateDisposition.STALE, result.disposition)
        assertEquals(current, result.state)
    }

    @Test
    fun recognizesExactDuplicate() {
        val current = state(sequence = 5, red = 60)
        val result = PortalLedStateReducer.reduce(current, current.copy())

        assertEquals(PortalLedUpdateDisposition.DUPLICATE, result.disposition)
        assertEquals(current, result.state)
    }

    @Test
    fun rejectsConflictingPayloadAtSameSequence() {
        val current = state(sequence = 5, red = 60)
        val conflict = state(sequence = 5, red = 200)
        val result = PortalLedStateReducer.reduce(current, conflict)

        assertEquals(PortalLedUpdateDisposition.CONFLICT, result.disposition)
        assertEquals(current, result.state)
    }

    private fun state(sequence: Long, red: Int): PortalLedState = PortalLedState(
        schemaVersion = 1,
        active = true,
        sequence = sequence,
        left = PortalRgb(red, 0, 0),
        right = PortalRgb(red, 0, 0)
    )
}

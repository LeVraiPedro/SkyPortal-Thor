// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionStateLogicTest {
    @Test
    fun favoritesToggleWithoutChangingUnrelatedEntries() {
        assertEquals(setOf("a", "b"), CollectionStateLogic.toggleFavorite(setOf("a"), "b"))
        assertEquals(setOf("a"), CollectionStateLogic.toggleFavorite(setOf("a", "b"), "b"))
    }

    @Test
    fun recentsAreDeduplicatedAndBounded() {
        val current = (1..12).map(Int::toString)
        val updated = CollectionStateLogic.recordRecent(current, "5", limit = 12)
        assertEquals("5", updated.first())
        assertEquals(12, updated.size)
        assertEquals(1, updated.count { it == "5" })
    }

    @Test
    fun reportsMissingQuickTeamFiles() {
        val team = QuickTeam("id", "Duo", "p1", "p2")
        assertEquals("Joueur 1", CollectionStateLogic.missingQuickTeamMember(team, emptySet()))
        assertEquals("Joueur 2", CollectionStateLogic.missingQuickTeamMember(team, setOf("p1")))
        assertNull(CollectionStateLogic.missingQuickTeamMember(team, setOf("p1", "p2")))
    }
}

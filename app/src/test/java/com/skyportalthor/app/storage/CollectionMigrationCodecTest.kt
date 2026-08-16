// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionMigrationCodecTest {
    @Test
    fun `valid payload preserves collection state`() {
        val result = CollectionMigrationCodec.decode(
            """
            {
              "schemaVersion": 1,
              "rootUri": "content://fixture/root",
              "dolphinPackage": "org.dolphinemu.dolphinemu",
              "playerTwoEnabled": true,
              "favoriteUris": ["content://fixture/spyro"],
              "recentUris": ["content://fixture/spyro", "content://fixture/bash"],
              "quickTeams": [{
                "id": "team-1",
                "name": "Équipe test",
                "playerOneUri": "content://fixture/spyro",
                "playerTwoUri": "content://fixture/bash"
              }]
            }
            """.trimIndent()
        ).getOrThrow()

        assertEquals("content://fixture/root", result.rootUri)
        assertEquals("org.dolphinemu.dolphinemu", result.dolphinPackage)
        assertTrue(result.playerTwoEnabled)
        assertEquals(setOf("content://fixture/spyro"), result.favoriteUris)
        assertEquals(2, result.recentUris.size)
        assertEquals("Équipe test", result.quickTeams.single().name)
    }

    @Test
    fun `unknown schema is rejected`() {
        assertTrue(CollectionMigrationCodec.decode("{\"schemaVersion\":2}").isFailure)
    }

    @Test
    fun `unexpected Dolphin target is rejected`() {
        val payload = """
            {"schemaVersion":1,"dolphinPackage":"example.invalid","favoriteUris":[],"recentUris":[]}
        """.trimIndent()
        assertTrue(CollectionMigrationCodec.decode(payload).isFailure)
    }

    @Test
    fun `non content URI is rejected`() {
        val payload = """
            {"schemaVersion":1,"favoriteUris":["file:///private.sky"],"recentUris":[]}
        """.trimIndent()
        assertTrue(CollectionMigrationCodec.decode(payload).isFailure)
    }
}

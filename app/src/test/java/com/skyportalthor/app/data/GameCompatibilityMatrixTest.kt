package com.skyportalthor.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCompatibilityMatrixTest {
    @Test
    fun detectsEveryKnownRegionalGameId() {
        SkylandersGame.entries.forEach { game ->
            game.gameIds.forEach { gameId ->
                assertEquals("$gameId doit détecter ${game.name}", game, SkylandersGame.detect(gameId, null))
            }
        }
        assertEquals(SkylandersGame.IMAGINATORS, SkylandersGame.detect(null, "Skylanders Imaginators"))
        assertEquals(null, SkylandersGame.detect("ABCD01", "Super Mario Galaxy 2"))
    }

    @Test
    fun blocksFutureGenerationsAndUnsupportedObjectTypes() {
        val future = check(4, FigureKind.CHARACTER, "Trap Master", SkylandersGame.SPYROS_ADVENTURE)
        assertFalse(future.compatible)
        assertTrue(future.reason.orEmpty().contains("génération plus récente"))

        assertFalse(check(4, FigureKind.TRAP, "Trap", SkylandersGame.SPYROS_ADVENTURE).compatible)
        assertFalse(check(5, FigureKind.VEHICLE, "Véhicule", SkylandersGame.TRAP_TEAM).compatible)
        assertFalse(check(6, FigureKind.CREATION_CRYSTAL, "Creation Crystal", SkylandersGame.SUPERCHARGERS).compatible)
        assertTrue(check(1, FigureKind.CHARACTER, "Skylander", SkylandersGame.SPYROS_ADVENTURE).compatible)
        assertTrue(check(1, FigureKind.ITEM, "Magic Item", SkylandersGame.SPYROS_ADVENTURE).compatible)
    }

    @Test
    fun distinguishesNativeCharactersAndPortalObjects() {
        assertEquals("Giant", DolphinFigureCatalog.decode(112, 0x1206, "Tree Rex", 1, 4, 2).typeLabel)
        assertEquals("SWAP Force", DolphinFigureCatalog.decode(3001, 0x2000, "Pop Thorn", 2, 1, 3).typeLabel)
        assertEquals("Trap Master", DolphinFigureCatalog.decode(462, 0x3000, "Snap Shot", 3, 7, 4).typeLabel)
        assertEquals(FigureKind.TRAP, DolphinFigureCatalog.decode(210, 0x3002, "Magic Log Holder", 3, 1, 9).kind)
        assertEquals("Magic Item", DolphinFigureCatalog.decode(200, 0, "Anvil Rain", 0, 11, 6).typeLabel)
        assertEquals("Adventure / Location", DolphinFigureCatalog.decode(300, 0, "Dragon's Peak", 0, 11, 6).typeLabel)
        assertEquals("Véhicule Land", DolphinFigureCatalog.decode(3224, 0x4000, "Hot Streak", 4, 2, 8).typeLabel)
        assertEquals("Véhicule Sky", DolphinFigureCatalog.decode(3220, 0x4000, "Jet Stream", 4, 3, 8).typeLabel)
        assertEquals("Véhicule Sea", DolphinFigureCatalog.decode(3222, 0x4000, "Reef Ripper", 4, 7, 8).typeLabel)
        assertEquals(FigureKind.TROPHY, DolphinFigureCatalog.decode(3500, 0x4000, "Sky Trophy", 4, 3, 7).kind)
        assertEquals("Sidekick", DolphinFigureCatalog.decode(505, 0, "Terrabite (Sidekick)", 0, 6, 5).typeLabel)
    }

    @Test
    fun rejectsUnknownIdAndUnknownVariantOnlyWhenApi3CatalogIsRequired() {
        val metadata = DolphinFigureCatalog.decode(16, 0, "Spyro", 0, 1, 1)
        val catalog = mapOf(metadata.key to metadata)

        assertTrue(NativeIdentityPolicy.check(65535, 65535, catalog, required = false).recognized)
        val unknownId = NativeIdentityPolicy.check(65535, 65535, catalog, required = true)
        val unknownVariant = NativeIdentityPolicy.check(16, 1234, catalog, required = true)

        assertFalse(unknownId.recognized)
        assertEquals("UNKNOWN_FIGURE_ID", unknownId.diagnosticCode)
        assertFalse(unknownVariant.recognized)
        assertEquals("UNKNOWN_FIGURE_VARIANT", unknownVariant.diagnosticCode)
        assertNotNull(NativeIdentityPolicy.check(16, 0, catalog, required = true).metadata)
    }

    @Test
    fun allCollectionShowsIncompatibleContentWithoutMakingItLoadable() {
        assertFalse(
            FigureFilterPolicy.visible(
                FigureKind.VEHICLE,
                charactersCategory = false,
                smartFilterEnabled = true,
                compatible = false
            )
        )
        assertTrue(
            FigureFilterPolicy.visible(
                FigureKind.VEHICLE,
                charactersCategory = false,
                smartFilterEnabled = false,
                compatible = false
            )
        )
        assertFalse(check(5, FigureKind.VEHICLE, "Véhicule", SkylandersGame.TRAP_TEAM).compatible)
    }

    private fun check(
        generation: Int,
        kind: FigureKind,
        label: String,
        game: SkylandersGame
    ) = FigureCompatibilityEngine.check(
        generation,
        kind,
        label,
        "Fixture",
        DolphinFigureCatalog.generationName(generation),
        game
    )
}

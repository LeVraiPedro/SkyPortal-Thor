package com.skyportalthor.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartPortalModelsTest {
    @Test fun detectsKnownIdsAndTitles() {
        assertEquals(SkylandersGame.SPYROS_ADVENTURE, SkylandersGame.detect("SSPE52", null))
        assertEquals(SkylandersGame.GIANTS, SkylandersGame.detect("SKYP52", null))
        assertEquals(SkylandersGame.SWAP_FORCE, SkylandersGame.detect("SVXE52", null))
        assertEquals(SkylandersGame.TRAP_TEAM, SkylandersGame.detect("SK8E52", null))
        assertEquals(SkylandersGame.SUPERCHARGERS, SkylandersGame.detect("SKNP52", null))
        assertEquals(SkylandersGame.IMAGINATORS, SkylandersGame.detect(null, "Skylanders Imaginators"))
    }

    @Test fun blocksForwardGenerationAndUnsupportedType() {
        assertFalse(check(4, FigureKind.TRAP, SkylandersGame.SPYROS_ADVENTURE))
        assertTrue(check(4, FigureKind.TRAP, SkylandersGame.TRAP_TEAM))
        assertTrue(check(1, FigureKind.CHARACTER, SkylandersGame.SUPERCHARGERS))
    }

    @Test fun decodesNativeFigureTypesForPickerSubcategories() {
        assertEquals("Trap Master", DolphinFigureCatalog.decode(1, 0, "Test", 3, 1, 4).typeLabel)
        assertEquals(FigureKind.VEHICLE, DolphinFigureCatalog.decode(2, 0, "Test", 4, 2, 8).kind)
        assertEquals(FigureKind.TROPHY, DolphinFigureCatalog.decode(3, 0, "Test", 4, 0, 7).kind)
    }

    private fun check(generation: Int, kind: FigureKind, game: SkylandersGame) =
        FigureCompatibilityEngine.check(
            generation, kind, kind.name, "Test", DolphinFigureCatalog.generationName(generation), game
        ).compatible
}

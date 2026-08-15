package com.skyportalthor.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SkylanderPathParserTest {
    @Test
    fun parsesSpyroFromStructuredCollection() {
        val result = SkylanderPathParser.parse(
            fileName = "SSA_Spyro_S1.sky",
            segments = listOf("01_Spyros_Adventure", "Magic", "Spyro")
        )

        assertEquals("Spyro", result.name)
        assertEquals("Magic", result.element)
        assertEquals("Spyro's Adventure", result.generation)
        assertEquals(FigureKind.CHARACTER, result.kind)
    }

    @Test
    fun distinguishesVehicleFromPlayableCharacter() {
        val result = SkylanderPathParser.parse(
            fileName = "SC_Hot_Streak_Vehicle_Land.sky",
            segments = listOf("05_SuperChargers", "Vehicules", "Fire")
        )

        assertEquals("Hot Streak", result.name)
        assertEquals("SuperChargers", result.generation)
        assertEquals(FigureKind.VEHICLE, result.kind)
    }

    @Test
    fun parsesTrapMasterType() {
        val result = SkylanderPathParser.parse(
            fileName = "TT_Snap_Shot_TrapMaster.sky",
            segments = listOf("04_Trap_Team", "Water", "Snap_Shot")
        )

        assertEquals("Snap Shot", result.name)
        assertEquals("Trap Master", result.typeLabel)
        assertEquals("Water", result.element)
    }
}

// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.data

data class ParsedFigureMetadata(
    val name: String,
    val element: String,
    val generation: String,
    val kind: FigureKind,
    val typeLabel: String
)

object SkylanderPathParser {
    private val elements = listOf("Air", "Dark", "Earth", "Fire", "Life", "Light", "Magic", "Tech", "Undead", "Water", "Kaos")

    fun parse(fileName: String, segments: List<String>): ParsedFigureMetadata {
        val generation = when {
            segments.any { it.contains("01_Spyros_Adventure", true) } -> "Spyro's Adventure"
            segments.any { it.contains("02_Giants", true) } -> "Giants"
            segments.any { it.contains("03_Swap_Force", true) } -> "Swap Force"
            segments.any { it.contains("04_Trap_Team", true) } -> "Trap Team"
            segments.any { it.contains("05_SuperChargers", true) } -> "SuperChargers"
            segments.any { it.contains("06_Imaginators", true) } -> "Imaginators"
            else -> "Autre"
        }

        val kind = when {
            segments.any { it.equals("Vehicules", true) || it.equals("Vehicles", true) } -> FigureKind.VEHICLE
            segments.any { it.equals("Traps", true) } -> FigureKind.TRAP
            segments.any { it.equals("Creation_Crystals", true) } -> FigureKind.CREATION_CRYSTAL
            segments.any { it.equals("Trophies", true) } -> FigureKind.TROPHY
            segments.any { it.contains("Objets_Portail", true) } -> FigureKind.ITEM
            else -> FigureKind.CHARACTER
        }

        val element = segments.firstOrNull { segment -> elements.any { it.equals(segment, true) } }
            ?.let { found -> elements.first { it.equals(found, true) } }
            ?: "Other"

        val typeLabel = when (kind) {
            FigureKind.VEHICLE -> "Véhicule"
            FigureKind.TRAP -> "Trap"
            FigureKind.CREATION_CRYSTAL -> "Creation Crystal"
            FigureKind.TROPHY -> "Trophée"
            FigureKind.ITEM -> segments.lastOrNull {
                it.contains("Item", true) || it.contains("Pack", true) || it.contains("Sidekick", true)
            }?.replace('_', ' ') ?: "Objet du portail"
            FigureKind.CHARACTER -> when {
                segments.any { it.equals("Senseis", true) } -> "Sensei"
                segments.any { it.equals("Vilains", true) } -> "Vilain Sensei"
                fileName.contains("TrapMaster", true) -> "Trap Master"
                fileName.contains("SuperCharger", true) -> "SuperCharger"
                fileName.contains("_Giant", true) -> "Giant"
                fileName.contains("_SWAP", true) -> "SWAP"
                else -> "Skylander"
            }
            FigureKind.UNKNOWN -> "Inconnu"
        }

        val name = parseName(fileName, segments)
        return ParsedFigureMetadata(name, element, generation, kind, typeLabel)
    }

    private fun parseName(fileName: String, segments: List<String>): String {
        val parsed = fileName
            .removeSuffix(".sky")
            .replace(Regex("^(SSA|SG|SF|TT|SC|IMAG)_"), "")
            .replace(
                Regex(
                    "_(S[1-4]|Core|Giant|SWAP|TrapMaster|SuperCharger|MagicItem|Location|Sidekick|BattleItem|MASTER_BLANK|Vehicle_(Land|Sea|Sky))$"
                ),
                ""
            )
            .replace('_', ' ')
            .trim()

        if (parsed.isNotBlank()) return parsed
        return segments.lastOrNull()?.replace('_', ' ')?.trim().orEmpty().ifBlank { "Unknown" }
    }
}

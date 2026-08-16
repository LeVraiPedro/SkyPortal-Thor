package com.skyportalthor.app.data

enum class EmulationState { NONE, STARTING, RUNNING, PAUSED, STOPPING, UNKNOWN }

enum class SmartPortalReadiness {
    DOLPHIN_ABSENT,
    DOLPHIN_DETECTED,
    CONNECTING,
    NO_GAME,
    GAME_DETECTED,
    PORTAL_DISABLED,
    ENABLING_PORTAL,
    READY,
    ERROR
}

data class FigureKey(val id: Int, val variant: Int)

data class FigureMetadata(
    val key: FigureKey,
    val canonicalName: String,
    val generation: Int,
    val element: String,
    val kind: FigureKind,
    val typeLabel: String
)

data class CompatibilityResult(val compatible: Boolean, val reason: String? = null)

enum class SkylandersGame(
    val displayName: String,
    val generation: Int,
    val idPrefixes: Set<String>,
    val supportedKinds: Set<FigureKind>,
    val features: Set<String>
) {
    SPYROS_ADVENTURE(
        "Spyro’s Adventure", 1, setOf("SSP"),
        setOf(FigureKind.CHARACTER, FigureKind.ITEM),
        setOf("Adventure Packs", "Magic Items")
    ),
    GIANTS(
        "Giants", 2, setOf("SKY"),
        setOf(FigureKind.CHARACTER, FigureKind.ITEM),
        setOf("Giants", "Adventure Packs", "Magic Items")
    ),
    SWAP_FORCE(
        "Swap Force", 3, setOf("SVX"),
        setOf(FigureKind.CHARACTER, FigureKind.ITEM),
        setOf("SWAP Force", "Adventure Packs", "Magic Items")
    ),
    TRAP_TEAM(
        "Trap Team", 4, setOf("SK8"),
        setOf(FigureKind.CHARACTER, FigureKind.ITEM, FigureKind.TRAP),
        setOf("Trap Masters", "Traps", "Light", "Dark")
    ),
    SUPERCHARGERS(
        "SuperChargers", 5, setOf("SKN", "BS5"),
        setOf(FigureKind.CHARACTER, FigureKind.ITEM, FigureKind.TRAP, FigureKind.VEHICLE, FigureKind.TROPHY),
        setOf("Land", "Sea", "Sky", "Trophies")
    ),
    IMAGINATORS(
        "Imaginators", 6, setOf("BL6"),
        FigureKind.entries.toSet() - FigureKind.UNKNOWN,
        setOf("Senseis", "Creation Crystals")
    );

    companion object {
        fun detect(gameId: String?, title: String?): SkylandersGame? {
            val normalizedId = gameId.orEmpty().uppercase()
            entries.firstOrNull { game -> game.idPrefixes.any(normalizedId::startsWith) }?.let { return it }
            val normalizedTitle = title.orEmpty().lowercase()
            if ("skylander" !in normalizedTitle) return null
            return when {
                "spyro" in normalizedTitle -> SPYROS_ADVENTURE
                "giant" in normalizedTitle -> GIANTS
                "swap" in normalizedTitle -> SWAP_FORCE
                "trap" in normalizedTitle -> TRAP_TEAM
                "supercharger" in normalizedTitle -> SUPERCHARGERS
                "imaginator" in normalizedTitle -> IMAGINATORS
                else -> null
            }
        }
    }
}

object FigureCompatibilityEngine {
    fun check(figure: Skylander, game: SkylandersGame?): CompatibilityResult = check(
        figure.generationNumber, figure.kind, figure.typeLabel, figure.name, figure.generation, game
    )

    fun check(
        generationNumber: Int,
        kind: FigureKind,
        typeLabel: String,
        name: String,
        generationName: String,
        game: SkylandersGame?
    ): CompatibilityResult {
        if (game == null) return CompatibilityResult(true)
        if (kind == FigureKind.UNKNOWN) {
            return CompatibilityResult(false, "Cet objet n’est pas identifié de façon assez fiable pour vérifier sa compatibilité.")
        }
        if (generationNumber > game.generation) {
            return CompatibilityResult(
                false,
                "$name provient de $generationName et ne peut pas être utilisé dans ${game.displayName}."
            )
        }
        if (kind !in game.supportedKinds) {
            return CompatibilityResult(
                false,
                "$typeLabel n’est pas pris en charge par ${game.displayName}."
            )
        }
        return CompatibilityResult(true)
    }
}

object DolphinFigureCatalog {
    private val games = arrayOf("Spyro's Adventure", "Giants", "Swap Force", "Trap Team", "SuperChargers")
    private val elements = arrayOf("Other", "Magic", "Fire", "Air", "Life", "Undead", "Earth", "Water", "Tech", "Dark", "Light", "Other")

    fun decode(id: Int, variant: Int, name: String, game: Int, element: Int, type: Int): FigureMetadata {
        val kind = when (type) {
            1, 2, 3, 4, 5 -> FigureKind.CHARACTER
            6 -> FigureKind.ITEM
            7 -> FigureKind.TROPHY
            8 -> FigureKind.VEHICLE
            9 -> FigureKind.TRAP
            else -> FigureKind.UNKNOWN
        }
        val typeLabel = when (type) {
            1 -> "Skylander"
            2 -> "Giant"
            3 -> "SWAP Force"
            4 -> "Trap Master"
            5 -> "Mini"
            6 -> "Objet du portail"
            7 -> "Trophée"
            8 -> "Véhicule"
            9 -> "Trap"
            else -> "Inconnu"
        }
        return FigureMetadata(
            FigureKey(id, variant), name,
            generation = game + 1,
            element = elements.getOrElse(element) { "Other" },
            kind = kind,
            typeLabel = typeLabel
        )
    }

    fun generationName(number: Int): String = games.getOrElse(number - 1) {
        if (number == 6) "Imaginators" else "Autre"
    }
}

package com.skyportalthor.app.data

enum class EmulationState { NONE, STARTING, RUNNING, PAUSED, STOPPING, UNKNOWN }

enum class DolphinServiceState { INITIALIZING, READY, UNKNOWN }

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

data class NativeIdentityResult(
    val recognized: Boolean,
    val diagnosticCode: String? = null,
    val reason: String? = null,
    val metadata: FigureMetadata? = null
)

object NativeIdentityPolicy {
    fun check(
        figureId: Int,
        variantId: Int,
        catalog: Map<FigureKey, FigureMetadata>,
        required: Boolean
    ): NativeIdentityResult {
        if (!required) return NativeIdentityResult(recognized = true)
        val metadata = catalog[FigureKey(figureId, variantId)]
        if (metadata != null) return NativeIdentityResult(recognized = true, metadata = metadata)
        val idKnown = catalog.keys.any { it.id == figureId }
        return NativeIdentityResult(
            recognized = false,
            diagnosticCode = if (idKnown) "UNKNOWN_FIGURE_VARIANT" else "UNKNOWN_FIGURE_ID",
            reason = if (idKnown) {
                "La variante $variantId de ce Skylander n’est pas reconnue par Dolphin."
            } else {
                "L’identifiant $figureId de ce fichier .sky n’est pas reconnu par Dolphin."
            }
        )
    }
}

enum class GameFeature {
    ADVENTURE_PACKS,
    MAGIC_ITEMS,
    GIANTS,
    SWAP_FORCE,
    TRAP_MASTERS,
    TRAPS,
    LIGHT_DARK,
    VEHICLES,
    TROPHIES,
    SENSEIS,
    CREATION_CRYSTALS
}

private val CORE_ELEMENTS = setOf("Magic", "Fire", "Air", "Life", "Undead", "Earth", "Water", "Tech")

enum class SkylandersGame(
    val displayName: String,
    val generation: Int,
    val gameIds: Set<String>,
    val supportedKinds: Set<FigureKind>,
    val features: Set<GameFeature>,
    val availableElements: Set<String>
) {
    SPYROS_ADVENTURE(
        "Spyro’s Adventure", 1,
        setOf("SSPE52", "SSPJGD", "SSPP52", "SSPX52", "SSPY52"),
        setOf(FigureKind.CHARACTER, FigureKind.ITEM),
        setOf(GameFeature.ADVENTURE_PACKS, GameFeature.MAGIC_ITEMS),
        CORE_ELEMENTS
    ),
    GIANTS(
        "Giants", 2,
        setOf("SKYE52", "SKYP52", "SKYX52", "SKYY52", "SKYZ52"),
        setOf(FigureKind.CHARACTER, FigureKind.ITEM),
        setOf(GameFeature.GIANTS, GameFeature.ADVENTURE_PACKS, GameFeature.MAGIC_ITEMS),
        CORE_ELEMENTS
    ),
    SWAP_FORCE(
        "Swap Force", 3,
        setOf("SVXE52", "SVXF52", "SVXI52", "SVXP52", "SVXX52", "SVXY52"),
        setOf(FigureKind.CHARACTER, FigureKind.ITEM),
        setOf(GameFeature.SWAP_FORCE, GameFeature.ADVENTURE_PACKS, GameFeature.MAGIC_ITEMS),
        CORE_ELEMENTS
    ),
    TRAP_TEAM(
        "Trap Team", 4,
        setOf("SK8D52", "SK8E52", "SK8I52", "SK8P52", "SK8V52", "SK8X52"),
        setOf(FigureKind.CHARACTER, FigureKind.ITEM, FigureKind.TRAP),
        setOf(GameFeature.TRAP_MASTERS, GameFeature.TRAPS, GameFeature.LIGHT_DARK),
        CORE_ELEMENTS + setOf("Light", "Dark")
    ),
    SUPERCHARGERS(
        "SuperChargers", 5,
        setOf("SKNE52", "SKNP52", "BS5E52", "BS5P52"),
        setOf(FigureKind.CHARACTER, FigureKind.ITEM, FigureKind.TRAP, FigureKind.VEHICLE, FigureKind.TROPHY),
        setOf(GameFeature.VEHICLES, GameFeature.TROPHIES, GameFeature.TRAPS, GameFeature.LIGHT_DARK),
        CORE_ELEMENTS + setOf("Light", "Dark")
    ),
    IMAGINATORS(
        "Imaginators", 6,
        setOf("BL6E52", "BL6P52"),
        FigureKind.entries.toSet() - FigureKind.UNKNOWN,
        setOf(GameFeature.SENSEIS, GameFeature.CREATION_CRYSTALS),
        CORE_ELEMENTS + setOf("Light", "Dark", "Kaos")
    );

    companion object {
        fun detect(gameId: String?, title: String?): SkylandersGame? {
            val normalizedId = gameId.orEmpty().uppercase()
            entries.firstOrNull { normalizedId in it.gameIds }?.let { return it }
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
    fun check(
        figure: Skylander,
        game: SkylandersGame?,
        requireNativeIdentity: Boolean = false
    ): CompatibilityResult {
        if (figure.isMasterTemplate) {
            return CompatibilityResult(
                false,
                "Ce fichier MASTER est un modèle vierge. Crée d’abord une copie de travail dans Dolphin."
            )
        }
        if (figure.dumpStatus !in setOf(SkyDumpStatus.UNKNOWN, SkyDumpStatus.VALID)) {
            return CompatibilityResult(
                false,
                figure.dumpProblem ?: "Ce fichier .sky est invalide ou inaccessible."
            )
        }
        if (requireNativeIdentity && !figure.identifiedByDolphin) {
            val identity = listOfNotNull(figure.figureId, figure.variantId).joinToString("/")
            return CompatibilityResult(
                false,
                if (identity.isBlank()) {
                    "L’identité de ce fichier .sky ne peut pas être lue de façon fiable."
                } else {
                    "L’identifiant $identity de ce fichier .sky n’est pas reconnu par cette version de Dolphin."
                }
            )
        }
        return check(
            figure.generationNumber,
            figure.kind,
            figure.typeLabel,
            figure.name,
            figure.generation,
            game
        )
    }

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
            val subject = if (kind == FigureKind.CHARACTER) "Ce personnage" else "Ce contenu"
            return CompatibilityResult(
                false,
                "$subject provient d’une génération plus récente ($generationName) et ne peut pas être utilisé dans ${game.displayName}."
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
            5 -> if (name.contains("Sidekick", ignoreCase = true)) "Sidekick" else "Mini"
            6 -> if (id in LOCATION_IDS) "Adventure / Location" else "Magic Item"
            7 -> "Trophée"
            8 -> when (id) {
                in SKY_VEHICLE_IDS -> "Véhicule Sky"
                in SEA_VEHICLE_IDS -> "Véhicule Sea"
                else -> "Véhicule Land"
            }
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

    private val LOCATION_IDS = (300..308).toSet() + setOf(3300, 3301)
    private val SKY_VEHICLE_IDS = setOf(3220, 3232, 3233, 3236, 3241)
    private val SEA_VEHICLE_IDS = setOf(3222, 3231, 3237, 3238, 3239)
}

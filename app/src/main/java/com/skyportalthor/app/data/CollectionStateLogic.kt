package com.skyportalthor.app.data

internal object CollectionStateLogic {
    fun toggleFavorite(current: Set<String>, uri: String): Set<String> =
        current.toMutableSet().apply { if (!add(uri)) remove(uri) }.toSet()

    fun recordRecent(current: List<String>, uri: String, limit: Int = 12): List<String> =
        (listOf(uri) + current.filterNot { it == uri }).take(limit)

    fun missingQuickTeamMember(team: QuickTeam, availableUris: Set<String>): String? = when {
        team.playerOneUri !in availableUris -> "Joueur 1"
        team.playerTwoUri != null && team.playerTwoUri !in availableUris -> "Joueur 2"
        else -> null
    }
}

internal object FigureFilterPolicy {
    fun visible(
        kind: FigureKind,
        charactersCategory: Boolean,
        smartFilterEnabled: Boolean,
        compatible: Boolean
    ): Boolean {
        val categoryMatches = if (charactersCategory) {
            kind == FigureKind.CHARACTER
        } else {
            kind != FigureKind.CHARACTER
        }
        return categoryMatches && (!smartFilterEnabled || compatible)
    }
}

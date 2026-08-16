// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.storage

import com.skyportalthor.app.data.QuickTeam
import org.json.JSONArray
import org.json.JSONObject

data class CollectionMigrationData(
    val rootUri: String?,
    val dolphinPackage: String?,
    val playerTwoEnabled: Boolean,
    val favoriteUris: Set<String>,
    val recentUris: List<String>,
    val quickTeams: List<QuickTeam>
)

sealed interface CollectionMigrationResult {
    data object NotFound : CollectionMigrationResult
    data object Imported : CollectionMigrationResult
    data class Failed(val reason: String) : CollectionMigrationResult
}

object CollectionMigrationCodec {
    const val FILE_NAME = "collection-v1.json"
    const val MAX_FILE_BYTES = 256 * 1024L
    private const val SCHEMA_VERSION = 1
    private const val MAX_URI_COUNT = 512
    private const val MAX_URI_LENGTH = 8_192
    private const val MAX_TEAMS = 10
    private val allowedDolphinPackages = setOf(
        "org.dolphinemu.dolphinemu",
        "org.dolphinemu.dolphinemu.debug"
    )

    fun decode(raw: String): Result<CollectionMigrationData> = runCatching {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) {
            "fichier de migration trop volumineux"
        }
        val json = JSONObject(raw)
        require(json.optInt("schemaVersion", -1) == SCHEMA_VERSION) {
            "version de migration non prise en charge"
        }
        val rootUri = json.optionalString("rootUri")?.validatedUri()
        val dolphinPackage = json.optionalString("dolphinPackage")?.also {
            require(it in allowedDolphinPackages) { "cible Dolphin non reconnue" }
        }
        val favorites = json.stringList("favoriteUris").toSet()
        val recents = json.stringList("recentUris")
        val quickTeams = json.optJSONArray("quickTeams")?.let(::readTeams).orEmpty()
        CollectionMigrationData(
            rootUri = rootUri,
            dolphinPackage = dolphinPackage,
            playerTwoEnabled = json.optBoolean("playerTwoEnabled", false),
            favoriteUris = favorites,
            recentUris = recents,
            quickTeams = quickTeams
        )
    }

    fun teamsToJson(teams: List<QuickTeam>): String {
        val array = JSONArray()
        teams.take(MAX_TEAMS).forEach { team ->
            array.put(
                JSONObject()
                    .put("id", team.id)
                    .put("name", team.name)
                    .put("playerOneUri", team.playerOneUri)
                    .put("playerTwoUri", team.playerTwoUri ?: "")
            )
        }
        return array.toString()
    }

    private fun JSONObject.optionalString(key: String): String? =
        optString(key).trim().takeIf { it.isNotEmpty() }

    private fun JSONObject.stringList(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        require(array.length() <= MAX_URI_COUNT) { "trop d'éléments dans $key" }
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index).trim()
                require(value.isNotEmpty()) { "entrée vide dans $key" }
                add(value.validatedUri())
            }
        }.distinct()
    }

    private fun readTeams(array: JSONArray): List<QuickTeam> {
        require(array.length() <= MAX_TEAMS) { "trop d'équipes rapides" }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: error("équipe rapide invalide")
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                val playerOne = item.optString("playerOneUri").trim().validatedUri()
                val playerTwo = item.optString("playerTwoUri").trim()
                    .takeIf { it.isNotEmpty() }
                    ?.validatedUri()
                require(id.isNotEmpty() && id.length <= 128) { "identifiant d'équipe invalide" }
                require(name.isNotEmpty() && name.length <= 80) { "nom d'équipe invalide" }
                add(QuickTeam(id, name, playerOne, playerTwo))
            }
        }
    }

    private fun String.validatedUri(): String {
        require(length <= MAX_URI_LENGTH && startsWith("content://")) { "URI de collection invalide" }
        return this
    }
}

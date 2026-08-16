package com.skyportalthor.app.storage

import android.content.Context
import android.net.Uri
import com.skyportalthor.app.data.QuickTeam
import com.skyportalthor.app.data.CollectionStateLogic
import org.json.JSONArray
import org.json.JSONObject

class CollectionPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("skyportal_collection", Context.MODE_PRIVATE)

    fun getRootUri(): Uri? = prefs.getString(KEY_ROOT_URI, null)?.let(Uri::parse)

    fun setRootUri(uri: Uri) {
        prefs.edit().putString(KEY_ROOT_URI, uri.toString()).apply()
    }

    fun getDolphinPackage(): String? = prefs.getString(KEY_DOLPHIN_PACKAGE, null)

    fun setDolphinPackage(packageName: String) {
        prefs.edit().putString(KEY_DOLPHIN_PACKAGE, packageName).apply()
    }

    fun isPlayerTwoEnabled(): Boolean = prefs.getBoolean(KEY_PLAYER_TWO_ENABLED, false)

    fun setPlayerTwoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PLAYER_TWO_ENABLED, enabled).apply()
    }

    fun getFavoriteUris(): Set<String> =
        prefs.getStringSet(KEY_FAVORITE_URIS, emptySet())?.toSet().orEmpty()

    fun toggleFavorite(uri: String): Set<String> {
        val updated = CollectionStateLogic.toggleFavorite(getFavoriteUris(), uri)
        prefs.edit().putStringSet(KEY_FAVORITE_URIS, updated).apply()
        return updated
    }

    fun getRecentUris(): List<String> = readStringArray(KEY_RECENT_URIS)

    fun recordRecent(uri: String): List<String> {
        val updated = CollectionStateLogic.recordRecent(getRecentUris(), uri, MAX_RECENTS)
        writeStringArray(KEY_RECENT_URIS, updated)
        return updated
    }

    fun getQuickTeams(): List<QuickTeam> = runCatching {
        val array = JSONArray(prefs.getString(KEY_QUICK_TEAMS, "[]") ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val name = item.optString("name")
                val playerOneUri = item.optString("playerOneUri")
                if (id.isBlank() || name.isBlank() || playerOneUri.isBlank()) continue
                add(
                    QuickTeam(
                        id = id,
                        name = name,
                        playerOneUri = playerOneUri,
                        playerTwoUri = item.optString("playerTwoUri").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun saveQuickTeam(team: QuickTeam): List<QuickTeam> {
        val updated = (listOf(team) + getQuickTeams().filterNot { it.id == team.id }).take(MAX_TEAMS)
        val array = JSONArray()
        updated.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("playerOneUri", item.playerOneUri)
                    .put("playerTwoUri", item.playerTwoUri ?: "")
            )
        }
        prefs.edit().putString(KEY_QUICK_TEAMS, array.toString()).apply()
        return updated
    }

    fun deleteQuickTeam(id: String): List<QuickTeam> {
        val updated = getQuickTeams().filterNot { it.id == id }
        val array = JSONArray()
        updated.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("playerOneUri", item.playerOneUri)
                    .put("playerTwoUri", item.playerTwoUri ?: "")
            )
        }
        prefs.edit().putString(KEY_QUICK_TEAMS, array.toString()).apply()
        return updated
    }

    private fun readStringArray(key: String): List<String> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]") ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }.getOrDefault(emptyList())

    private fun writeStringArray(key: String, values: List<String>) {
        val array = JSONArray()
        values.forEach(array::put)
        prefs.edit().putString(key, array.toString()).apply()
    }

    companion object {
        private const val KEY_ROOT_URI = "root_uri"
        private const val KEY_DOLPHIN_PACKAGE = "dolphin_package"
        private const val KEY_PLAYER_TWO_ENABLED = "player_two_enabled"
        private const val KEY_FAVORITE_URIS = "favorite_uris"
        private const val KEY_RECENT_URIS = "recent_uris"
        private const val KEY_QUICK_TEAMS = "quick_teams"
        private const val MAX_RECENTS = 12
        private const val MAX_TEAMS = 10
    }
}

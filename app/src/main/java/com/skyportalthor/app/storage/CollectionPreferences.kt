package com.skyportalthor.app.storage

import android.content.Context
import android.net.Uri

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

    companion object {
        private const val KEY_ROOT_URI = "root_uri"
        private const val KEY_DOLPHIN_PACKAGE = "dolphin_package"
    }
}

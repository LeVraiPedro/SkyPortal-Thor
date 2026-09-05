// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.storage

import android.content.Context
import com.skyportalthor.app.portal.led.bifrost.LightingSettings

internal class LightingPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("skyportal_lighting", Context.MODE_PRIVATE)
    fun read() = LightingSettings(prefs.getBoolean("enabled", false), prefs.getInt("brightness", 35).coerceIn(0, 100))
    fun write(settings: LightingSettings) {
        prefs.edit().putBoolean("enabled", settings.enabled).putInt("brightness", settings.brightnessPercent).apply()
    }
}

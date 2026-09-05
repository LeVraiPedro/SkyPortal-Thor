// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led.bifrost

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import com.skyportalthor.app.dolphin.DolphinPortalBridge
import com.skyportalthor.app.storage.LightingPreferences
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive

/** Owned by a visible Activity; no foreground service, wake lock, profile or hardware writes. */
internal class BifrostLightingController(context: Context, private val portal: DolphinPortalBridge) {
    private val preferences = LightingPreferences(context)
    private val transport = AndroidBifrostTransport(context)
    private val session = BifrostSession(transport)
    private val power = context.applicationContext.getSystemService(PowerManager::class.java)
    private val mutableSettings = MutableStateFlow(preferences.read())
    val settings = mutableSettings.asStateFlow()
    val status = session.status
    fun availability() = transport.availability()

    fun updateSettings(settings: LightingSettings) {
        preferences.write(settings)
        mutableSettings.value = settings
    }

    suspend fun runWhileVisible() {
        try {
            while (currentCoroutineContext().isActive) {
                val state = portal.state.value
                val confirmedAt = portal.lastLedConfirmedAtMs
                val now = SystemClock.elapsedRealtime()
                val frame = if (power?.isInteractive == true) {
                    BifrostFramePolicy.frame(state, settings.value, confirmedAt, now)
                } else null
                session.tick(frame, now)
                delay(100L)
            }
        } finally {
            // A bounded CLEAR, even when repeatOnLifecycle cancels onStop or the Activity is destroyed.
            session.release()
        }
    }
}

package com.skyportalthor.app.display

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.view.Display

class DisplayRouter(private val context: Context) {
    private val displayManager = context.getSystemService(DisplayManager::class.java)

    fun allDisplays(): List<Display> = displayManager.displays.toList()

    fun supportsSecondaryActivities(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS)

    /**
     * The Thor exposes its upper panel as Android's default display and its lower panel as a
     * presentation display. Never derive the destination from the display that opened the
     * launcher: doing so swaps the panels when the icon is tapped on the lower screen.
     */
    fun lowerDisplay(): Display? = allDisplays()
        .filter { it.displayId != Display.DEFAULT_DISPLAY && it.state != Display.STATE_OFF }
        .sortedWith(
            compareByDescending<Display> { it.flags and Display.FLAG_PRESENTATION != 0 }
                .thenBy { it.mode.physicalWidth * it.mode.physicalHeight }
                .thenBy { it.displayId }
        )
        .firstOrNull()

    fun launchOnDisplay(activity: Activity, intent: Intent, display: Display): Boolean = runCatching {
        val options = ActivityOptions.makeBasic().apply { launchDisplayId = display.displayId }
        activity.startActivity(intent, options.toBundle())
        true
    }.getOrDefault(false)
}

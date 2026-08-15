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

    fun secondaryDisplay(currentDisplayId: Int): Display? = allDisplays()
        .filter { it.displayId != currentDisplayId && it.state != Display.STATE_OFF }
        .sortedWith(compareByDescending<Display> { it.mode.physicalHeight }.thenBy { it.displayId })
        .firstOrNull()

    fun launchOnDisplay(activity: Activity, intent: Intent, display: Display) {
        val options = ActivityOptions.makeBasic().apply { launchDisplayId = display.displayId }
        activity.startActivity(intent, options.toBundle())
    }
}

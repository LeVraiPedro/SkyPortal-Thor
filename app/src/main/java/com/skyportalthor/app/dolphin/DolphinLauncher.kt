// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.dolphin

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.view.Display

object DolphinLauncher {
    fun launchOnPrimaryDisplay(activity: Activity, preferredPackage: String? = null): Boolean {
        val pm = activity.packageManager
        val orderedPackages = buildList {
            if (preferredPackage in DolphinTargets.packages) add(preferredPackage!!)
            addAll(DolphinTargets.packages.filterNot { it == preferredPackage })
        }
        val launchIntent = orderedPackages.asSequence()
            .mapNotNull { packageName -> pm.getLaunchIntentForPackage(packageName) }
            .firstOrNull() ?: return false

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptions.makeBasic().apply { launchDisplayId = Display.DEFAULT_DISPLAY }
        activity.startActivity(launchIntent, options.toBundle())
        return true
    }

    fun installedPackage(pm: PackageManager): String? = DolphinTargets.packages.firstOrNull { pkg ->
        runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
    }
}

// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.dolphin

import android.content.ComponentName

object DolphinTargets {
    const val DEBUG_PACKAGE = "org.dolphinemu.dolphinemu.debug"
    const val RELEASE_PACKAGE = "org.dolphinemu.dolphinemu"
    private const val SERVICE_CLASS = "org.dolphinemu.dolphinemu.skyportal.SkyPortalService"

    val packages = listOf(DEBUG_PACKAGE, RELEASE_PACKAGE)
    val components = packages.map { ComponentName(it, SERVICE_CLASS) }

    fun label(packageName: String?): String = when (packageName) {
        DEBUG_PACKAGE -> "Dolphin Debug"
        RELEASE_PACKAGE -> "Dolphin Release"
        null -> "Dolphin"
        else -> packageName.substringAfterLast('.')
    }
}

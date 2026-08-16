// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme

internal object PortalPalette {
    val Background = Color(0xFF08111F)
    val Panel = Color(0xFF141F33)
    val PanelRaised = Color(0xFF1C2B44)
    val Accent = Color(0xFF79B4FF)
    val Muted = Color(0xFFA0B0CC)
    val Success = Color(0xFF61D095)
    val Warning = Color(0xFFFFC857)
    val Error = Color(0xFFFF7A7A)

    fun element(name: String): Color = when (name.lowercase()) {
        "air" -> Color(0xFFD6F4FF)
        "dark" -> Color(0xFF8E7CC3)
        "earth" -> Color(0xFFC98C55)
        "fire" -> Color(0xFFFF6B35)
        "life" -> Color(0xFF73D35B)
        "light" -> Color(0xFFFFE785)
        "magic" -> Color(0xFFA879FF)
        "tech" -> Color(0xFFFFC928)
        "undead" -> Color(0xFFA7D8C9)
        "water" -> Color(0xFF4DA3FF)
        "kaos" -> Color(0xFFE95BCE)
        else -> Muted
    }
}

internal val PortalColorScheme = darkColorScheme(
    primary = PortalPalette.Accent,
    onPrimary = Color(0xFF061425),
    secondary = PortalPalette.Success,
    onSecondary = Color(0xFF062116),
    background = PortalPalette.Background,
    onBackground = Color.White,
    surface = PortalPalette.Panel,
    onSurface = Color.White,
    surfaceVariant = PortalPalette.PanelRaised,
    onSurfaceVariant = PortalPalette.Muted,
    error = PortalPalette.Error,
    outline = PortalPalette.Muted
)

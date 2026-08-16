// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.data

data class QuickTeam(
    val id: String,
    val name: String,
    val playerOneUri: String,
    val playerTwoUri: String? = null
)

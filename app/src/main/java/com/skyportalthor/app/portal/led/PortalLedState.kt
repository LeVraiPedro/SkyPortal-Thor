// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led

data class PortalRgb(
    val red: Int,
    val green: Int,
    val blue: Int
) {
    init {
        require(red in CHANNEL_RANGE) { "red must be between 0 and 255" }
        require(green in CHANNEL_RANGE) { "green must be between 0 and 255" }
        require(blue in CHANNEL_RANGE) { "blue must be between 0 and 255" }
    }

    fun toArgb(alpha: Int = 255): Int {
        require(alpha in CHANNEL_RANGE) { "alpha must be between 0 and 255" }
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    fun toHex(): String = "#%02X%02X%02X".format(red, green, blue)

    companion object {
        val Black = PortalRgb(0, 0, 0)
        private val CHANNEL_RANGE = 0..255
    }
}

data class PortalLedState(
    val schemaVersion: Int,
    val active: Boolean,
    val sequence: Long,
    val left: PortalRgb,
    val right: PortalRgb,
    val trap: PortalRgb? = null
) {
    init {
        require(schemaVersion >= 1) { "schemaVersion must be positive" }
        require(sequence >= 0L) { "sequence must not be negative" }
    }

    fun hasSameVisibleOutputAs(other: PortalLedState): Boolean =
        active == other.active &&
            left == other.left &&
            right == other.right &&
            trap == other.trap

    companion object {
        const val SCHEMA_VERSION_1 = 1

        fun off(sequence: Long = 0L): PortalLedState = PortalLedState(
            schemaVersion = SCHEMA_VERSION_1,
            active = false,
            sequence = sequence,
            left = PortalRgb.Black,
            right = PortalRgb.Black,
            trap = null
        )
    }
}

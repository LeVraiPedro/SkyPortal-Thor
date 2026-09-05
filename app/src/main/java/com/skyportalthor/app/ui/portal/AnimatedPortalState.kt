// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui.portal

import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.data.GameFeature
import com.skyportalthor.app.data.SmartPortalReadiness
import com.skyportalthor.app.portal.PortalState
import com.skyportalthor.app.portal.led.PortalLedState
import com.skyportalthor.app.portal.led.PortalRgb

internal enum class PortalVisualMode {
    DISCONNECTED,
    CONNECTING,
    DISABLED,
    INITIALIZING,
    CONFLICT,
    ERROR,
    LEGACY,
    READY_IDLE,
    READY_ACTIVE
}

internal enum class PortalVisualTone {
    NEUTRAL,
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

internal data class AnimatedPortalState(
    val mode: PortalVisualMode,
    val title: String,
    val detail: String,
    val active: Boolean,
    val left: PortalRgb,
    val right: PortalRgb,
    val trap: PortalRgb?,
    val sequence: Long?,
    val tone: PortalVisualTone,
    val pulse: Boolean,
    val warning: String? = null
) {
    val accessibilityDescription: String
        get() = buildString {
            append(title)
            append(". ")
            append(detail)
            append(". Couleur gauche ")
            append(left.toHex())
            append(", couleur droite ")
            append(right.toHex())
            trap?.let {
                append(", couleur Trap ")
                append(it.toHex())
            }
            warning?.let {
                append(". Avertissement : ")
                append(it)
            }
        }
}

internal object AnimatedPortalStateMapper {
    fun from(state: PortalState): AnimatedPortalState {
        val warning = state.portalLedError ?: state.portalLedWarnings.firstOrNull()

        if (!state.connected) {
            return fallback(
                mode = PortalVisualMode.DISCONNECTED,
                title = "Dolphin déconnecté",
                detail = "Le portail reprendra vie après la reconnexion.",
                left = DISCONNECTED_LEFT,
                right = DISCONNECTED_RIGHT,
                tone = PortalVisualTone.NEUTRAL,
                warning = warning
            )
        }

        if (state.readiness == SmartPortalReadiness.PORTAL_CONFLICT) {
            return fallback(
                mode = PortalVisualMode.CONFLICT,
                title = "Conflit de portail",
                detail = "Désactive la base concurrente puis redémarre l’émulation.",
                left = CONFLICT_LEFT,
                right = CONFLICT_RIGHT,
                tone = PortalVisualTone.ERROR,
                active = true,
                pulse = true,
                warning = warning
            )
        }

        if (state.readiness == SmartPortalReadiness.PORTAL_DISABLED || state.portalEnabled == false) {
            return fallback(
                mode = PortalVisualMode.DISABLED,
                title = "Portal of Power désactivé",
                detail = "Active le portail dans SkyPortal ou dans Dolphin.",
                left = DISABLED,
                right = DISABLED,
                tone = PortalVisualTone.WARNING,
                warning = warning
            )
        }

        if (
            state.serviceState == DolphinServiceState.INITIALIZING ||
            state.readiness in setOf(
                SmartPortalReadiness.CONNECTING,
                SmartPortalReadiness.ENABLING_PORTAL,
                SmartPortalReadiness.PORTAL_INITIALIZING
            )
        ) {
            return fallback(
                mode = PortalVisualMode.INITIALIZING,
                title = "Initialisation du portail",
                detail = "Dolphin prépare le périphérique et le canal lumineux.",
                left = INITIALIZING_LEFT,
                right = INITIALIZING_RIGHT,
                tone = PortalVisualTone.INFO,
                active = true,
                pulse = true,
                warning = warning
            )
        }

        if (
            state.readiness in setOf(
                SmartPortalReadiness.PORTAL_RESTART_REQUIRED,
                SmartPortalReadiness.ERROR
            )
        ) {
            return fallback(
                mode = PortalVisualMode.ERROR,
                title = "Portail indisponible",
                detail = "Un redémarrage complet de l’émulation peut être nécessaire.",
                left = ERROR_LEFT,
                right = ERROR_RIGHT,
                tone = PortalVisualTone.ERROR,
                active = true,
                pulse = true,
                warning = warning
            )
        }

        val apiVersion = state.apiVersion ?: 0
        if (apiVersion < 4) {
            return fallback(
                mode = PortalVisualMode.LEGACY,
                title = if (state.readiness == SmartPortalReadiness.READY) {
                    "Portail prêt"
                } else {
                    "Portail détecté"
                },
                detail = "Dolphin API $apiVersion • couleurs du jeu indisponibles",
                left = LEGACY_LEFT,
                right = LEGACY_RIGHT,
                tone = if (state.readiness == SmartPortalReadiness.READY) {
                    PortalVisualTone.SUCCESS
                } else {
                    PortalVisualTone.INFO
                },
                active = state.readiness == SmartPortalReadiness.READY,
                pulse = state.readiness == SmartPortalReadiness.READY,
                warning = warning
            )
        }

        val led = state.portalLedState
        if (led == null) {
            return fallback(
                mode = PortalVisualMode.CONNECTING,
                title = "En attente des lumières",
                detail = "Dolphin API 4 est connecté, le premier état LED n’est pas encore arrivé.",
                left = WAITING_LEFT,
                right = WAITING_RIGHT,
                tone = if (warning == null) PortalVisualTone.INFO else PortalVisualTone.WARNING,
                active = true,
                pulse = true,
                warning = warning
            )
        }

        return fromLedState(
            led,
            warning,
            showTrapZone = state.skylandersGame?.features?.contains(GameFeature.TRAPS) == true
        )
    }

    private fun fromLedState(
        led: PortalLedState,
        warning: String?,
        showTrapZone: Boolean
    ): AnimatedPortalState {
        val trap = led.trap.takeIf { showTrapZone }
        if (!led.active) {
            return AnimatedPortalState(
                mode = PortalVisualMode.READY_IDLE,
                title = "Éclairage du portail en veille",
                detail = "Dolphin API 4 • séquence ${led.sequence}",
                active = false,
                left = led.left,
                right = led.right,
                trap = trap,
                sequence = led.sequence,
                tone = if (warning == null) PortalVisualTone.NEUTRAL else PortalVisualTone.WARNING,
                pulse = false,
                warning = warning
            )
        }

        return AnimatedPortalState(
            mode = PortalVisualMode.READY_ACTIVE,
            title = "Portal of Power actif",
            detail = "Dolphin API 4 • séquence ${led.sequence}",
            active = true,
            left = led.left,
            right = led.right,
            trap = trap,
            sequence = led.sequence,
            tone = if (warning == null) PortalVisualTone.SUCCESS else PortalVisualTone.WARNING,
            pulse = true,
            warning = warning
        )
    }

    private fun fallback(
        mode: PortalVisualMode,
        title: String,
        detail: String,
        left: PortalRgb,
        right: PortalRgb,
        tone: PortalVisualTone,
        active: Boolean = false,
        pulse: Boolean = false,
        warning: String? = null
    ): AnimatedPortalState = AnimatedPortalState(
        mode = mode,
        title = title,
        detail = detail,
        active = active,
        left = left,
        right = right,
        trap = null,
        sequence = null,
        tone = tone,
        pulse = pulse,
        warning = warning
    )

    private val DISCONNECTED_LEFT = PortalRgb(19, 34, 53)
    private val DISCONNECTED_RIGHT = PortalRgb(26, 48, 73)
    private val DISABLED = PortalRgb(23, 29, 39)
    private val INITIALIZING_LEFT = PortalRgb(59, 139, 255)
    private val INITIALIZING_RIGHT = PortalRgb(116, 204, 255)
    private val WAITING_LEFT = PortalRgb(68, 119, 210)
    private val WAITING_RIGHT = PortalRgb(100, 170, 235)
    private val LEGACY_LEFT = PortalRgb(70, 128, 214)
    private val LEGACY_RIGHT = PortalRgb(121, 180, 255)
    private val CONFLICT_LEFT = PortalRgb(255, 74, 73)
    private val CONFLICT_RIGHT = PortalRgb(255, 161, 52)
    private val ERROR_LEFT = PortalRgb(203, 54, 78)
    private val ERROR_RIGHT = PortalRgb(255, 105, 72)
}

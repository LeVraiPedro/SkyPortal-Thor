// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui

import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.data.SmartPortalReadiness
import com.skyportalthor.app.portal.PortalState

internal enum class HomeRecovery(val label: String) {
    NONE(""), RECONNECT("Reconnecter"), DOLPHIN("Ouvrir Dolphin"), ACTIVATE("Activer"), HELP("Résoudre")
}

internal data class HomeStatus(val label: String, val recovery: HomeRecovery = HomeRecovery.NONE,
    val isReady: Boolean = false, val isError: Boolean = false, val description: String? = null)

internal object PortalHomePolicy {
    fun selectedPlayer(requested: Int, twoPlayers: Boolean): Int = if (twoPlayers && requested == 1) 1 else 0

    fun status(state: PortalState): HomeStatus = when {
        !state.connected -> HomeStatus("Déconnecté", HomeRecovery.RECONNECT,
            description = "Dolphin n'est pas connecté à ton portail.")
        state.readiness == SmartPortalReadiness.PORTAL_CONFLICT -> HomeStatus("Conflit", HomeRecovery.HELP, isError = true)
        state.readiness in setOf(SmartPortalReadiness.ERROR, SmartPortalReadiness.PORTAL_RESTART_REQUIRED) ->
            HomeStatus("À vérifier", HomeRecovery.HELP, isError = true)
        state.serviceState == DolphinServiceState.INITIALIZING || state.readiness in setOf(
            SmartPortalReadiness.CONNECTING, SmartPortalReadiness.ENABLING_PORTAL, SmartPortalReadiness.PORTAL_INITIALIZING
        ) -> HomeStatus("Connexion…")
        state.readiness == SmartPortalReadiness.PORTAL_DISABLED -> HomeStatus("Désactivé",
            if (state.canSetPortalEnabled) HomeRecovery.ACTIVATE else HomeRecovery.DOLPHIN)
        (state.apiVersion ?: 1) < 3 -> HomeStatus("Non vérifié", HomeRecovery.HELP)
        state.readiness == SmartPortalReadiness.READY -> HomeStatus("Prêt à jouer", isReady = true)
        state.readiness == SmartPortalReadiness.NO_GAME -> HomeStatus("En attente", HomeRecovery.DOLPHIN,
            description = "Lance ton jeu sur l'écran supérieur.")
        else -> HomeStatus("À vérifier", HomeRecovery.HELP)
    }
}

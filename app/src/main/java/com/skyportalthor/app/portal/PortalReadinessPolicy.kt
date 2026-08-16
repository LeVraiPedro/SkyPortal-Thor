package com.skyportalthor.app.portal

import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.data.SkylandersGame
import com.skyportalthor.app.data.SmartPortalReadiness

internal data class PortalReadinessDecision(
    val readiness: SmartPortalReadiness,
    val restartRequired: Boolean = false
)

internal data class PortalLoadBlock(
    val message: String,
    val diagnosticCode: String,
    val recoveryHint: String
)

/**
 * Separates a configured emulated portal from a portal the running game has really used.
 * `portalActivated` is deliberately not an input: Dolphin historically initialised that flag to
 * true, so it is not proof that IOS/USB exposed the device to the game.
 */
internal object PortalReadinessPolicy {
    fun canAutoActivate(
        apiVersion: Int?,
        readiness: SmartPortalReadiness,
        canSetPortalEnabled: Boolean,
        portalUsbStatusValid: Boolean,
        conflictingUsbDevices: List<String>
    ): Boolean = (apiVersion ?: 0) >= VERIFIED_USB_STATUS_API &&
        readiness == SmartPortalReadiness.PORTAL_DISABLED &&
        canSetPortalEnabled && portalUsbStatusValid && conflictingUsbDevices.isEmpty()

    fun evaluate(
        apiVersion: Int,
        serviceState: DolphinServiceState,
        emulationState: EmulationState,
        game: SkylandersGame?,
        portalEnabled: Boolean?,
        portalUsbPresent: Boolean?,
        portalUsbAttached: Boolean?,
        portalUsbHandshakeSeen: Boolean?,
        conflictingUsbDevices: List<String>,
        portalUsbStatusValid: Boolean
    ): PortalReadinessDecision {
        if (serviceState == DolphinServiceState.INITIALIZING) {
            return PortalReadinessDecision(SmartPortalReadiness.CONNECTING)
        }
        if (serviceState != DolphinServiceState.READY) {
            return PortalReadinessDecision(SmartPortalReadiness.PORTAL_UNVERIFIED)
        }
        if (emulationState == EmulationState.NONE || emulationState == EmulationState.STOPPING) {
            return PortalReadinessDecision(SmartPortalReadiness.NO_GAME)
        }
        if (emulationState == EmulationState.UNKNOWN) {
            return PortalReadinessDecision(SmartPortalReadiness.PORTAL_UNVERIFIED)
        }
        if (emulationState == EmulationState.STARTING || game == null) {
            return PortalReadinessDecision(SmartPortalReadiness.GAME_DETECTED)
        }
        if (emulationState != EmulationState.RUNNING && emulationState != EmulationState.PAUSED) {
            return PortalReadinessDecision(SmartPortalReadiness.PORTAL_UNVERIFIED)
        }
        if (apiVersion < VERIFIED_USB_STATUS_API) {
            return PortalReadinessDecision(SmartPortalReadiness.PORTAL_UNVERIFIED)
        }
        if (conflictingUsbDevices.isNotEmpty()) {
            return PortalReadinessDecision(
                SmartPortalReadiness.PORTAL_CONFLICT,
                restartRequired = emulationState == EmulationState.RUNNING ||
                emulationState == EmulationState.PAUSED
            )
        }
        if (portalEnabled == false) {
            return PortalReadinessDecision(SmartPortalReadiness.PORTAL_DISABLED)
        }
        if (portalEnabled != true) {
            return PortalReadinessDecision(SmartPortalReadiness.PORTAL_UNVERIFIED)
        }

        val usbEvidenceAvailable = portalUsbStatusValid && portalUsbPresent != null &&
            portalUsbAttached != null && portalUsbHandshakeSeen != null
        if (!usbEvidenceAvailable) {
            // An older API 3 build is still Binder-compatible, but cannot prove guest USB state.
            return PortalReadinessDecision(SmartPortalReadiness.PORTAL_UNVERIFIED)
        }
        if (portalUsbPresent == true && portalUsbAttached == true && portalUsbHandshakeSeen == true) {
            return PortalReadinessDecision(SmartPortalReadiness.READY)
        }
        if (portalUsbHandshakeSeen == true && (portalUsbPresent != true || portalUsbAttached != true)) {
            return PortalReadinessDecision(SmartPortalReadiness.PORTAL_UNVERIFIED)
        }
        if (portalUsbAttached == true && portalUsbPresent != true) {
            return PortalReadinessDecision(SmartPortalReadiness.PORTAL_UNVERIFIED)
        }
        if (portalUsbAttached == true) {
            return PortalReadinessDecision(SmartPortalReadiness.PORTAL_INITIALIZING)
        }

        val running = emulationState == EmulationState.RUNNING ||
            emulationState == EmulationState.PAUSED
        return if (running) {
            PortalReadinessDecision(
                SmartPortalReadiness.PORTAL_RESTART_REQUIRED,
                restartRequired = true
            )
        } else {
            PortalReadinessDecision(SmartPortalReadiness.PORTAL_INITIALIZING)
        }
    }

    fun conflictLabel(code: String): String = when (code.trim().uppercase()) {
        "DISNEY_INFINITY_BASE", "DISNEY_INFINITY", "INFINITY_BASE" -> "base Disney Infinity"
        "SKYLANDERS_PORTAL" -> "autre portail Skylanders"
        else -> code.trim().takeIf(String::isNotBlank)
            ?.replace('_', ' ')
            ?.lowercase()
            ?.let { "périphérique USB $it" }
            ?: "périphérique USB émulé"
    }

    fun conflictSummary(devices: List<String>): String = devices
        .map(::conflictLabel)
        .distinct()
        .joinToString()

    /** API 1/2 retain their legacy load path because they cannot expose USB evidence. */
    fun loadBlock(
        apiVersion: Int,
        gameDetected: Boolean,
        readiness: SmartPortalReadiness,
        conflictingUsbDevices: List<String>
    ): PortalLoadBlock? {
        if (apiVersion < VERIFIED_USB_STATUS_API || !gameDetected || readiness == SmartPortalReadiness.READY) {
            return null
        }
        return when (readiness) {
            SmartPortalReadiness.PORTAL_CONFLICT -> {
                val conflicts = conflictSummary(conflictingUsbDevices).ifBlank { "une autre base USB émulée" }
                PortalLoadBlock(
                    message = "Le Portal of Power est bloqué par $conflicts",
                    diagnosticCode = "PORTAL_USB_CONFLICT",
                    recoveryHint = "Désactive la base concurrente dans Dolphin, arrête complètement l’émulation puis relance le jeu."
                )
            }
            SmartPortalReadiness.PORTAL_UNVERIFIED -> PortalLoadBlock(
                message = "Dolphin ne permet pas de vérifier que le jeu détecte réellement le portail",
                diagnosticCode = "PORTAL_USB_UNVERIFIED",
                recoveryHint = "Installe la paire SkyPortal/Dolphin mise à jour avant de charger une figurine."
            )
            SmartPortalReadiness.PORTAL_RESTART_REQUIRED -> PortalLoadBlock(
                message = "Le jeu n’a pas détecté le Portal of Power",
                diagnosticCode = "PORTAL_USB_RESTART_REQUIRED",
                recoveryHint = "Arrête complètement l’émulation, vérifie que seul le portail Skylanders est activé, puis relance le jeu."
            )
            SmartPortalReadiness.PORTAL_INITIALIZING,
            SmartPortalReadiness.GAME_DETECTED,
            SmartPortalReadiness.CONNECTING,
            SmartPortalReadiness.ENABLING_PORTAL -> PortalLoadBlock(
                message = "Le Portal of Power n’est pas encore prêt dans le jeu",
                diagnosticCode = "PORTAL_USB_NOT_READY",
                recoveryHint = "Attends que le jeu ait interrogé le portail puis réessaie."
            )
            SmartPortalReadiness.PORTAL_DISABLED -> PortalLoadBlock(
                message = "Le Portal of Power est désactivé dans Dolphin",
                diagnosticCode = "PORTAL_DISABLED",
                recoveryHint = "Active le portail, puis redémarre l’émulation si le jeu est déjà lancé."
            )
            SmartPortalReadiness.ERROR,
            SmartPortalReadiness.DOLPHIN_ABSENT,
            SmartPortalReadiness.DOLPHIN_DETECTED,
            SmartPortalReadiness.NO_GAME -> PortalLoadBlock(
                message = "L’état USB du Portal of Power ne permet pas le chargement",
                diagnosticCode = "PORTAL_USB_NOT_READY",
                recoveryHint = "Vérifie Dolphin et le portail avant de réessayer."
            )
            SmartPortalReadiness.READY -> null
        }
    }

    private const val VERIFIED_USB_STATUS_API = 3
}

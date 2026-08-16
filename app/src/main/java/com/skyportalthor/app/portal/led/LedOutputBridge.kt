// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led

enum class LedEffect {
    STATIC,
    BREATH,
    PULSE,
    FADE_TRANSITION
}

data class LedOutputFrame(
    val left: PortalRgb,
    val right: PortalRgb,
    val intensity: Int = 255,
    val effect: LedEffect = LedEffect.STATIC
) {
    init {
        require(intensity in 0..255) { "intensity must be between 0 and 255" }
    }

    companion object {
        fun fromPortalState(
            state: PortalLedState,
            intensity: Int = 255,
            effect: LedEffect = LedEffect.STATIC
        ): LedOutputFrame? = if (state.active) {
            LedOutputFrame(
                left = state.left,
                right = state.right,
                intensity = intensity,
                effect = effect
            )
        } else {
            null
        }
    }
}

enum class LedOutputAvailability {
    AVAILABLE,
    NOT_INSTALLED,
    SERVICE_STOPPED,
    CONTROL_DISABLED,
    UNSUPPORTED
}

sealed interface LedOutputResult {
    object Accepted : LedOutputResult

    data class Unavailable(
        val availability: LedOutputAvailability,
        val message: String
    ) : LedOutputResult

    data class Rejected(
        val message: String,
        val code: Int? = null
    ) : LedOutputResult
}

interface LedOutputBridge {
    fun availability(): LedOutputAvailability
    fun display(frame: LedOutputFrame): LedOutputResult
    fun clear(): LedOutputResult
}

class NoOpLedOutputBridge(
    private val reason: String = "Aucun contrôleur LED n’est configuré."
) : LedOutputBridge {
    override fun availability(): LedOutputAvailability = LedOutputAvailability.UNSUPPORTED

    override fun display(frame: LedOutputFrame): LedOutputResult = unavailable()

    override fun clear(): LedOutputResult = unavailable()

    private fun unavailable(): LedOutputResult.Unavailable = LedOutputResult.Unavailable(
        availability = LedOutputAvailability.UNSUPPORTED,
        message = reason
    )
}

// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led.bifrost

import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.data.SmartPortalReadiness
import com.skyportalthor.app.portal.PortalState
import com.skyportalthor.app.portal.led.LedOutputFrame

internal data class LightingSettings(val enabled: Boolean = false, val brightnessPercent: Int = 35) {
    init { require(brightnessPercent in 0..100) }
}

internal object BifrostFramePolicy {
    fun frame(state: PortalState, settings: LightingSettings, lastConfirmedAtMs: Long?, nowMs: Long): LedOutputFrame? {
        if (!settings.enabled || !state.connected || (state.apiVersion ?: 0) < 4 ||
            state.readiness != SmartPortalReadiness.READY || state.emulationState != EmulationState.RUNNING ||
            state.portalLedError != null || state.portalLedWarnings.isNotEmpty() || lastConfirmedAtMs == null ||
            nowMs < lastConfirmedAtMs || nowMs - lastConfirmedAtMs > 1_500L
        ) return null
        return state.portalLedState?.let {
            LedOutputFrame.fromPortalState(it, intensity = settings.brightnessPercent * 255 / 100)
        }
    }
}

// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.dolphin

import com.skyportalthor.app.portal.led.PortalLedParseResult
import com.skyportalthor.app.portal.led.PortalLedState
import com.skyportalthor.app.portal.led.PortalLedStateParser
import com.skyportalthor.app.portal.led.PortalLedStateReducer
import com.skyportalthor.app.portal.led.PortalLedUpdateDisposition

internal data class DolphinLedStateResolution(
    val state: PortalLedState?,
    val warnings: List<String> = emptyList(),
    val error: String? = null
)

/**
 * Converts the optional API 4 LED transaction into the companion's monotonic LED state.
 *
 * LED transport is deliberately non-blocking for the existing portal features: a malformed or
 * unavailable LED payload preserves the last confirmed LED state and reports a diagnostic, while
 * API 1–3 clear the capability without being treated as an error.
 */
internal object DolphinLedStateResolver {
    fun unavailable(): DolphinLedStateResolution = DolphinLedStateResolution(state = null)

    fun resolve(
        apiVersion: Int,
        current: PortalLedState?,
        payloadJson: String? = null,
        transportFailure: String? = null
    ): DolphinLedStateResolution {
        if (apiVersion < MIN_LED_API_VERSION) return DolphinLedStateResolution(state = null)

        if (transportFailure != null) {
            return DolphinLedStateResolution(
                state = current,
                error = "État lumineux Dolphin indisponible : $transportFailure"
            )
        }

        if (payloadJson == null) {
            return DolphinLedStateResolution(
                state = current,
                error = "Dolphin API 4 n’a renvoyé aucun état lumineux."
            )
        }

        return when (val parsed = PortalLedStateParser.parse(payloadJson)) {
            is PortalLedParseResult.Failure -> DolphinLedStateResolution(
                state = current,
                error = parsed.message
            )

            is PortalLedParseResult.Success -> {
                val reduction = PortalLedStateReducer.reduce(current, parsed.state)
                when (reduction.disposition) {
                    PortalLedUpdateDisposition.INITIAL,
                    PortalLedUpdateDisposition.ADVANCED,
                    PortalLedUpdateDisposition.DUPLICATE -> DolphinLedStateResolution(
                        state = reduction.state,
                        warnings = parsed.warnings
                    )

                    PortalLedUpdateDisposition.STALE -> DolphinLedStateResolution(
                        state = reduction.state,
                        warnings = parsed.warnings + "État lumineux Dolphin obsolète ignoré."
                    )

                    PortalLedUpdateDisposition.CONFLICT -> DolphinLedStateResolution(
                        state = reduction.state,
                        warnings = parsed.warnings,
                        error = "Conflit de séquence dans l’état lumineux Dolphin."
                    )
                }
            }
        }
    }

    private const val MIN_LED_API_VERSION = 4
}

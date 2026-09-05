// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui.portal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyportalthor.app.portal.led.bifrost.BifrostAvailability
import com.skyportalthor.app.portal.led.bifrost.BifrostSessionStatus
import com.skyportalthor.app.portal.led.bifrost.LightingSettings
import kotlin.math.roundToInt

@Composable
internal fun LightingSettingsDialog(
    settings: LightingSettings,
    status: BifrostSessionStatus,
    availability: BifrostAvailability,
    onChange: (LightingSettings) -> Unit,
    onOpenBifrost: () -> Unit,
    onDismiss: () -> Unit
) {
    var brightness by remember(settings.brightnessPercent) { mutableFloatStateOf(settings.brightnessPercent.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Éclairage des joysticks") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(when (availability) {
                    BifrostAvailability.AVAILABLE -> "Bifrost 1.3.1 détecté • API externe 1"
                    BifrostAvailability.NOT_INSTALLED -> "Bifrost absent : le portail reste utilisable sans LED externes."
                    BifrostAvailability.UNSUPPORTED_VERSION -> "Version Bifrost non validée. Cette intégration nécessite la version 1.3.1."
                    BifrostAvailability.UNAVAILABLE -> "Bifrost indisponible sur cet appareil."
                })
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Synchroniser le Portal of Power", Modifier.weight(1f))
                    Switch(
                        checked = settings.enabled,
                        enabled = availability == BifrostAvailability.AVAILABLE || settings.enabled,
                        onCheckedChange = { onChange(settings.copy(enabled = it)) }
                    )
                }
                Text("Luminosité : ${brightness.roundToInt()} %")
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    onValueChangeFinished = { onChange(settings.copy(brightnessPercent = brightness.roundToInt())) },
                    valueRange = 0f..100f
                )
                Text(if (settings.enabled) status.message else "Synchronisation désactivée dans SkyPortal.", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Démarre Bifrost et active « Allow third-party LED control » dans ses réglages. Les couleurs gauche/droite suivent Dolphin API 4 à 2 Hz maximum, seulement si le portail est prêt et SkyPortal visible.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Bifrost ne confirme ni son service ni les LED physiques. À la sortie, SkyPortal lui demande de restituer l’éclairage précédent ; cela nécessite que Bifrost reste actif.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenBifrost, enabled = availability == BifrostAvailability.AVAILABLE) {
                Text("Bifrost en haut")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

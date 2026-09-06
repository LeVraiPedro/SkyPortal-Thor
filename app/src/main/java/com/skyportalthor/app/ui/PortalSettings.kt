// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skyportalthor.app.portal.PortalSlotState

@Composable
internal fun PortalSettingsDialog(
    statusLine: String,
    rootSelected: Boolean,
    scanning: Boolean,
    playerTwoEnabled: Boolean,
    onDismiss: () -> Unit,
    onLighting: () -> Unit,
    onPlayerMode: () -> Unit,
    onDolphin: () -> Unit,
    onTarget: () -> Unit,
    onDiagnostics: () -> Unit,
    onPickRoot: () -> Unit,
    onRescan: () -> Unit,
    onClear: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    UtilitySheet("Réglages", onDismiss) {
        Text(statusLine.removePrefix("● "), color = PortalPalette.Muted, style = MaterialTheme.typography.bodySmall)
        SettingsSection("À ta façon")
        SettingsAction("Éclairage des joysticks", "Synchronisation du portail et luminosité", onClick = onLighting)
        SettingsAction("Joueurs", if (playerTwoEnabled) "Deux joueurs" else "Mode solo", onClick = onPlayerMode)
        SettingsSection("Collection")
        SettingsAction("Dossier des figurines", if (rootSelected) "Changer le dossier sélectionné" else "Sélectionner un dossier .sky",
            enabled = !scanning, onClick = onPickRoot)
        SettingsAction("Actualiser la collection", if (scanning) "Actualisation en cours…" else "Retrouver les fichiers ajoutés ou déplacés",
            enabled = rootSelected && !scanning, onClick = onRescan)
        SettingsSection("Dolphin & assistance")
        SettingsAction("Ouvrir Dolphin", "Sur l'écran supérieur", onClick = onDolphin)
        SettingsAction("Version de Dolphin", "Choisir l'application à connecter", onClick = onTarget)
        SettingsAction("Diagnostic", "Connexion, portail, lumières et slots natifs", onClick = onDiagnostics)
        SettingsAction("Retirer tout du portail", "Les fichiers de la collection sont conservés", onClick = { confirmClear = true })
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Retirer toutes les figurines ?") },
            text = { Text("Tous les joueurs et objets seront retirés du portail. Aucun fichier .sky ne sera supprimé.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClear(); onDismiss() }) { Text("Tout retirer") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Annuler") } }
        )
    }
}

@Composable
internal fun ExtraSlotsDialog(slots: List<PortalSlotState>, onDismiss: () -> Unit, onTap: (PortalSlotState) -> Unit) {
    UtilitySheet("Objets & emplacements", onDismiss) {
        Text("Choisis un emplacement libre pour ajouter un objet ou une figurine compatible.",
            color = PortalPalette.Muted, style = MaterialTheme.typography.bodySmall)
        slots.forEach { slot ->
            SettingsAction(
                "Emplacement ${slot.logicalSlot + 1}",
                slot.figure?.name ?: slot.label ?: "Ajouter une figurine ou un objet",
                onClick = { onTap(slot) }
            )
        }
    }
}

@Composable
private fun UtilitySheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        MaterialTheme(colorScheme = PortalColorScheme) {
            Surface(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp), color = PortalPalette.Background,
                shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Fermer") }
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(title, Modifier.padding(top = 14.dp, bottom = 4.dp), color = PortalPalette.Accent,
        style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun SettingsAction(title: String, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, color = PortalPalette.Panel, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(subtitle, color = PortalPalette.Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = PortalPalette.Muted, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

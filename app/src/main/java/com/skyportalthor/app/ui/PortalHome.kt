// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyportalthor.app.portal.PortalSlotState
import com.skyportalthor.app.portal.PortalState
import com.skyportalthor.app.ui.portal.AnimatedPortalArtwork

/** Presentation only: every action still uses PortalScreen's guarded callbacks. */
@Composable
internal fun PortalHome(
    state: PortalState,
    playerTwoEnabled: Boolean,
    figureCount: Int,
    hasFolder: Boolean,
    scanning: Boolean,
    busy: Boolean,
    statusLine: String,
    onChoose: (Int) -> Unit,
    onActions: (PortalSlotState) -> Unit,
    onSettings: () -> Unit,
    onPlayerMode: () -> Unit,
    onTeams: () -> Unit,
    onExtras: () -> Unit,
    onRecovery: (HomeRecovery) -> Unit,
    notice: @Composable () -> Unit
) {
    var requestedPlayer by rememberSaveable { mutableIntStateOf(0) }
    val selectedPlayer = PortalHomePolicy.selectedPlayer(requestedPlayer, playerTwoEnabled)
    val slot = state.slots.firstOrNull { it.logicalSlot == selectedPlayer } ?: PortalSlotState(selectedPlayer)
    val occupied = slot.actualPortalSlot >= 0 || slot.figure != null || !slot.label.isNullOrBlank()
    val accent = slot.figure?.let { PortalPalette.element(it.element) } ?: PortalPalette.Accent
    val status = PortalHomePolicy.status(state)

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Canvas(Modifier.size(28.dp)) {
                drawCircle(PortalPalette.Accent, style = Stroke(2.dp.toPx()))
                drawCircle(PortalPalette.Accent.copy(alpha = 0.4f), radius = size.width * 0.28f, style = Stroke(1.dp.toPx()))
                drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(size.width * 0.83f, size.height * 0.2f))
            }
            Column(Modifier.weight(1f)) {
                Text("SkyPortal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    state.skylandersGame?.displayName ?: state.gameTitle?.takeIf { it.isNotBlank() } ?: "Ton portail, à portée de main",
                    color = PortalPalette.Muted, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Surface(color = status.color().copy(alpha = 0.10f), shape = CircleShape) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).background(status.color(), CircleShape))
                    Text(status.label, color = status.color(), style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            TextButton(onClick = onSettings, modifier = Modifier.size(48.dp)
                .semantics { contentDescription = "Réglages de SkyPortal" }, contentPadding = PaddingValues(0.dp)) {
                Canvas(Modifier.size(24.dp)) {
                    val ink = PortalPalette.Muted
                    repeat(8) { index ->
                        val angle = Math.PI * index / 4.0
                        val dx = kotlin.math.cos(angle).toFloat()
                        val dy = kotlin.math.sin(angle).toFloat()
                        drawLine(ink, center + Offset(dx, dy) * 7.dp.toPx(),
                            center + Offset(dx, dy) * 10.dp.toPx(), 2.dp.toPx(), StrokeCap.Round)
                    }
                    drawCircle(ink, 6.5.dp.toPx(), style = Stroke(2.dp.toPx()))
                    drawCircle(ink, 2.dp.toPx())
                }
            }
        }

        notice()

        if (status.recovery != HomeRecovery.NONE) {
            Surface(color = status.color().copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(status.description ?: statusLine.removePrefix("● "), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = PortalPalette.Muted)
                    TextButton(onClick = { onRecovery(status.recovery) }) { Text(status.recovery.label) }
                }
            }
        }

        Surface(color = PortalPalette.Panel, shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.07f), Color.Transparent)))
                .padding(horizontal = 18.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (playerTwoEnabled) {
                        listOf(0, 1).forEach { player ->
                            TextButton(onClick = { requestedPlayer = player }, enabled = !busy,
                                modifier = Modifier.semantics { selected = selectedPlayer == player },
                                colors = ButtonDefaults.textButtonColors(contentColor = if (selectedPlayer == player) accent else PortalPalette.Muted)) {
                                Text(if (selectedPlayer == player) "● Joueur ${player + 1}" else "Joueur ${player + 1}")
                            }
                        }
                    } else {
                        Text("TON PORTAIL", style = MaterialTheme.typography.labelSmall, color = PortalPalette.Muted,
                            modifier = Modifier.weight(1f))
                    }
                    if (playerTwoEnabled) Spacer(Modifier.weight(1f))
                    TextButton(onClick = onPlayerMode, enabled = !busy) {
                        Text(if (playerTwoEnabled) "2 joueurs" else "Solo", color = PortalPalette.Muted,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Canvas has its own measured area. Text/actions never overlap its animated bounds.
                AnimatedPortalArtwork(state, Modifier.fillMaxWidth().height(105.dp)
                    .clickable(enabled = !busy, role = Role.Button, onClickLabel = "Choisir pour Joueur ${selectedPlayer + 1}") {
                        onChoose(selectedPlayer)
                    })

                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(targetState = slot.figure?.name ?: slot.label ?: "Choisis ton Skylander",
                        modifier = Modifier.weight(1f), label = "active-figure-name") { name ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (busy) "Placement en cours…" else slot.figure?.element ?: if (occupied) "Sur le portail" else "L'aventure commence ici",
                                color = accent, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(onClick = { onChoose(selectedPlayer) }, enabled = !busy, shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(if (occupied) "Changer" else "Choisir")
                    }
                    if (occupied) {
                        TextButton(onClick = { onActions(slot) }, enabled = !busy, modifier = Modifier.size(48.dp)
                            .semantics { contentDescription = "Actions pour ${slot.figure?.name ?: slot.label ?: "Joueur ${selectedPlayer + 1}"} : retirer, sauvegarder, informations" },
                            contentPadding = PaddingValues(0.dp)) { Text("•••") }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeDestination("Collection", when {
                scanning -> "Actualisation…"
                !hasFolder -> "Ajouter tes fichiers"
                else -> "$figureCount figurines"
            }, "◈", Modifier.weight(1f), !busy) { onChoose(selectedPlayer) }
            HomeDestination("Équipes", "Tes compositions", "◇", Modifier.weight(1f), !busy, onTeams)
        }
        val extraCount = state.slots.drop(2).count { it.actualPortalSlot >= 0 || it.figure != null || !it.label.isNullOrBlank() }
        TextButton(onClick = onExtras, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (extraCount > 0) "Objets & emplacements · $extraCount occupé(s)" else "＋  Objets & emplacements",
                color = PortalPalette.Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HomeDestination(title: String, subtitle: String, symbol: String, modifier: Modifier,
    enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = modifier, color = PortalPalette.Panel,
        shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(symbol, color = PortalPalette.Accent, style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = PortalPalette.Muted, style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("›", color = PortalPalette.Muted)
        }
    }
}

private fun HomeStatus.color(): Color = when {
    isReady -> PortalPalette.Success
    isError -> PortalPalette.Error
    else -> PortalPalette.Muted
}

// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skyportalthor.app.data.FigureKind
import com.skyportalthor.app.data.QuickTeam
import com.skyportalthor.app.data.Skylander
import com.skyportalthor.app.data.SmartPortalReadiness
import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.diagnostics.DiagnosticItem
import com.skyportalthor.app.diagnostics.DiagnosticLevel
import com.skyportalthor.app.dolphin.DolphinTargets
import com.skyportalthor.app.portal.PortalResult
import com.skyportalthor.app.portal.PortalReadinessPolicy
import com.skyportalthor.app.portal.PortalSlotState
import com.skyportalthor.app.portal.PortalState
import com.skyportalthor.app.ui.portal.AnimatedPortalPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PortalScreen(
    portalState: PortalState,
    figures: List<Skylander>,
    playerTwoEnabled: Boolean,
    favoriteUris: Set<String>,
    recentUris: List<String>,
    quickTeams: List<QuickTeam>,
    rootUri: Uri?,
    scanning: Boolean,
    uiMessage: UiNotice?,
    onDismissMessage: () -> Unit,
    onPickRoot: () -> Unit,
    onRescan: () -> Unit,
    onReconnect: () -> Unit,
    onSelectDolphinPackage: (String) -> Unit,
    onLaunchDolphin: () -> Unit,
    onSetPortalEnabled: (Boolean) -> Unit,
    onLoad: suspend (Int, Skylander) -> PortalResult,
    onToggleFavorite: (Skylander) -> Unit,
    onSaveCurrentTeam: (String) -> PortalResult,
    onDeleteTeam: (String) -> Unit,
    onLoadTeam: suspend (QuickTeam) -> PortalResult,
    onRunDiagnostics: () -> List<DiagnosticItem>,
    onPlayerTwoEnabledChange: suspend (Boolean) -> PortalResult,
    onBackup: suspend (Int, Skylander) -> PortalResult,
    onRemove: suspend (Int) -> PortalResult,
    onClear: () -> Unit
) {
    var pickerSlot by remember { mutableStateOf<Int?>(null) }
    var actionSlot by remember { mutableStateOf<PortalSlotState?>(null) }
    var infoSlot by remember { mutableStateOf<PortalSlotState?>(null) }
    var loadState by remember { mutableStateOf<LoadUiState>(LoadUiState.Idle) }
    var localNotice by remember { mutableStateOf<UiNotice?>(null) }
    var showDolphinTargets by remember { mutableStateOf(false) }
    var showPlayerMode by remember { mutableStateOf(false) }
    var showQuickTeams by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }

    LaunchedEffect(loadState) {
        val success = loadState as? LoadUiState.Success ?: return@LaunchedEffect
        val notice = UiNotice(
            success.result.message ?: "${success.figure.name} est sur le portail",
            NoticeKind.SUCCESS
        )
        localNotice = notice
        delay(2_800L)
        if (loadState == success) loadState = LoadUiState.Idle
        if (localNotice == notice) localNotice = null
    }

    MaterialTheme(colorScheme = PortalColorScheme) {
        Surface(color = PortalPalette.Background, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Header(
                    portalState = portalState,
                    playerTwoEnabled = playerTwoEnabled,
                    onReconnect = onReconnect,
                    onChooseTarget = { showDolphinTargets = true },
                    onChoosePlayerMode = { showPlayerMode = true },
                    onLaunchDolphin = onLaunchDolphin,
                    onSetPortalEnabled = onSetPortalEnabled
                )

                when {
                    localNotice != null -> MessageBar(localNotice!!) { localNotice = null }
                    uiMessage != null -> MessageBar(uiMessage, onDismissMessage)
                }

                PrimarySlots(
                    slots = portalState.slots,
                    playerTwoEnabled = playerTwoEnabled,
                    loadState = loadState,
                    onTap = { slot ->
                        if (slot.isOccupied()) actionSlot = slot else pickerSlot = slot.logicalSlot
                    }
                )

                AnimatedPortalPanel(
                    portalState = portalState,
                    playerTwoEnabled = playerTwoEnabled,
                    teamCount = quickTeams.size,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    onTeams = { showQuickTeams = true },
                    onDiagnostics = { showDiagnostics = true }
                )

                val compactFeedback = !portalState.connected || localNotice != null || uiMessage != null
                if (!compactFeedback) {
                    ExtraSlots(
                        slots = portalState.slots.drop(2),
                        loadState = loadState,
                        onTap = { slot ->
                            if (slot.isOccupied()) actionSlot = slot else pickerSlot = slot.logicalSlot
                        }
                    )
                }

                StorageBar(
                    rootUri = rootUri,
                    scanning = scanning,
                    totalCount = figures.size,
                    playableCount = figures.count { it.kind == FigureKind.CHARACTER },
                    onPickRoot = onPickRoot,
                    onRescan = onRescan,
                    onClear = onClear
                )

            }
        }
    }

    pickerSlot?.let { logicalSlot ->
        SkylanderPickerDialog(
            logicalSlot = logicalSlot,
            figures = figures,
            occupiedUris = portalState.slots.mapNotNull { it.sourceUri }.toSet(),
            favoriteUris = favoriteUris,
            recentUris = recentUris,
            portalConnected = portalState.connected,
            portalMessage = portalState.message,
            detectedGame = portalState.skylandersGame,
            requireNativeIdentity = (portalState.apiVersion ?: 1) >= 3 && portalState.figureCatalog.isNotEmpty(),
            loadState = loadState,
            onLoadStateChange = { loadState = it },
            onDismiss = { pickerSlot = null },
            onPickRoot = onPickRoot,
            onReconnect = onReconnect,
            onToggleFavorite = onToggleFavorite,
            onLoad = onLoad
        )
    }

    actionSlot?.let { slotSnapshot ->
        OccupiedSlotDialog(
            slot = slotSnapshot,
            onDismiss = { actionSlot = null },
            onChange = {
                actionSlot = null
                pickerSlot = slotSnapshot.logicalSlot
            },
            onInfo = {
                actionSlot = null
                infoSlot = slotSnapshot
            },
            onRemove = onRemove,
            onBackup = onBackup,
            onNotice = { localNotice = it },
            onClearLoadState = { loadState = LoadUiState.Idle }
        )
    }

    infoSlot?.let { slotSnapshot ->
        SlotInfoDialog(slot = slotSnapshot, onDismiss = { infoSlot = null })
    }

    if (showDolphinTargets) {
        DolphinTargetDialog(
            packages = portalState.availablePackages,
            selectedPackage = portalState.connectedPackage,
            onSelect = { packageName ->
                showDolphinTargets = false
                onSelectDolphinPackage(packageName)
            },
            onDismiss = { showDolphinTargets = false }
        )
    }

    if (showPlayerMode) {
        PlayerModeDialog(
            playerTwoEnabled = playerTwoEnabled,
            playerTwoOccupied = portalState.slots.getOrNull(1)?.isOccupied() == true,
            onChange = onPlayerTwoEnabledChange,
            onNotice = { localNotice = it },
            onDismiss = { showPlayerMode = false }
        )
    }

    if (showQuickTeams) {
        QuickTeamsDialog(
            teams = quickTeams,
            figures = figures,
            portalState = portalState,
            playerTwoEnabled = playerTwoEnabled,
            onSaveCurrentTeam = onSaveCurrentTeam,
            onDeleteTeam = onDeleteTeam,
            onLoadTeam = onLoadTeam,
            onNotice = { localNotice = it },
            onDismiss = { showQuickTeams = false }
        )
    }

    if (showDiagnostics) {
        DiagnosticsDialog(
            onRunDiagnostics = onRunDiagnostics,
            onReconnect = onReconnect,
            onRescan = onRescan,
            onLaunchDolphin = onLaunchDolphin,
            onDismiss = { showDiagnostics = false }
        )
    }
}

@Composable
private fun Header(
    portalState: PortalState,
    playerTwoEnabled: Boolean,
    onReconnect: () -> Unit,
    onChooseTarget: () -> Unit,
    onChoosePlayerMode: () -> Unit,
    onLaunchDolphin: () -> Unit,
    onSetPortalEnabled: (Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = PortalPalette.Panel), shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SKYPORTAL THOR",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    smartStatusLine(portalState),
                    color = if (portalState.readiness == SmartPortalReadiness.READY) PortalPalette.Success else PortalPalette.Warning,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onChoosePlayerMode,
                    modifier = Modifier.semantics {
                        contentDescription = if (playerTwoEnabled) {
                            "Mode deux joueurs, toucher pour modifier"
                        } else {
                            "Mode un joueur, toucher pour modifier"
                        }
                    }
                ) { Text(if (playerTwoEnabled) "2J" else "1J") }
                if (portalState.availablePackages.size > 1) {
                    OutlinedButton(onClick = onChooseTarget) { Text("Cible") }
                }
                if (!portalState.connected) {
                    OutlinedButton(onClick = onReconnect) { Text("Reconnecter") }
                }
                if (portalState.readiness == SmartPortalReadiness.PORTAL_DISABLED && portalState.canSetPortalEnabled) {
                    Button(onClick = { onSetPortalEnabled(true) }) { Text("Activer le portail") }
                }
                Button(onClick = onLaunchDolphin) { Text("Dolphin en haut") }
            }
        }
    }
}

private fun smartStatusLine(state: PortalState): String {
    if (!state.connected) return "● ${state.message}"
    if (state.serviceState == DolphinServiceState.INITIALIZING) {
        return "● Connecté | Initialisation Dolphin…"
    }
    val game = state.skylandersGame?.displayName ?: state.gameTitle?.takeIf { it.isNotBlank() } ?: "Aucun jeu"
    val portal = if ((state.apiVersion ?: 1) < 3) {
        "Portail non vérifié (API ${state.apiVersion ?: "?"})"
    } else when (state.readiness) {
        SmartPortalReadiness.ENABLING_PORTAL -> "Activation…"
        SmartPortalReadiness.PORTAL_DISABLED -> "Portail désactivé"
        SmartPortalReadiness.PORTAL_INITIALIZING -> "Portail détecté, initialisation…"
        SmartPortalReadiness.PORTAL_UNVERIFIED -> if (state.portalUsbStatusValid) {
            "État Dolphin non vérifié"
        } else {
            "Portail non vérifié — mise à jour requise"
        }
        SmartPortalReadiness.PORTAL_RESTART_REQUIRED -> "Portail absent — redémarrage requis"
        SmartPortalReadiness.PORTAL_CONFLICT -> {
            val conflict = PortalReadinessPolicy.conflictSummary(state.conflictingUsbDevices)
            if (conflict.isBlank()) "Conflit USB — redémarrage requis"
            else "Conflit : $conflict — redémarrage requis"
        }
        SmartPortalReadiness.READY -> "Portail prêt"
        else -> when {
            state.portalEnabled == true -> "Portail configuré, détection non vérifiée"
            state.portalEnabled == false -> "Portail désactivé"
            else -> "Portail inconnu"
        }
    }
    return "● Connecté | $game | $portal"
}

@Composable
private fun PrimarySlots(
    slots: List<PortalSlotState>,
    playerTwoEnabled: Boolean,
    loadState: LoadUiState,
    onTap: (PortalSlotState) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PortalSlotCard(
            title = "JOUEUR 1",
            slot = slots.getOrElse(0) { PortalSlotState(0) },
            loadState = loadState,
            modifier = Modifier.weight(1f),
            onTap = onTap
        )
        if (playerTwoEnabled) {
            PortalSlotCard(
                title = "JOUEUR 2",
                slot = slots.getOrElse(1) { PortalSlotState(1) },
                loadState = loadState,
                modifier = Modifier.weight(1f),
                onTap = onTap
            )
        }
    }
}

@Composable
private fun QuickActionsPanel(
    playerTwoEnabled: Boolean,
    teamCount: Int,
    modifier: Modifier = Modifier,
    onTeams: () -> Unit,
    onDiagnostics: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PortalPalette.Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (playerTwoEnabled) "Touchez Joueur 1 ou Joueur 2" else "Touchez Joueur 1",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Favoris et récents sont disponibles dans la collection.",
                    color = PortalPalette.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onTeams) {
                    Text(if (teamCount > 0) "Équipes ($teamCount)" else "Équipes")
                }
                OutlinedButton(onClick = onDiagnostics) { Text("Diagnostic") }
            }
        }
    }
}

@Composable
private fun QuickTeamsDialog(
    teams: List<QuickTeam>,
    figures: List<Skylander>,
    portalState: PortalState,
    playerTwoEnabled: Boolean,
    onSaveCurrentTeam: (String) -> PortalResult,
    onDeleteTeam: (String) -> Unit,
    onLoadTeam: suspend (QuickTeam) -> PortalResult,
    onNotice: (UiNotice) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val figuresByUri = remember(figures) { figures.associateBy { it.documentUri.toString() } }
    val playerOne = portalState.slots.getOrNull(0)?.figure
    val playerTwo = portalState.slots.getOrNull(1)?.figure.takeIf { playerTwoEnabled }
    val suggestedName = listOfNotNull(playerOne?.name, playerTwo?.name).joinToString(" + ")
    var saveExpanded by remember { mutableStateOf(false) }
    var teamName by remember(suggestedName) { mutableStateOf(suggestedName) }
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<PortalResult.Error?>(null) }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(color = PortalPalette.Panel, shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Équipes rapides",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Enregistre la configuration actuelle puis recharge-la en une seule action.",
                    color = PortalPalette.Muted
                )

                if (saveExpanded) {
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !busy,
                        label = { Text("Nom de l'équipe") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !busy && teamName.isNotBlank() && playerOne != null,
                            onClick = {
                                when (val result = onSaveCurrentTeam(teamName.trim())) {
                                    is PortalResult.Success -> {
                                        saveExpanded = false
                                        onNotice(UiNotice(result.message ?: "Équipe enregistrée", NoticeKind.SUCCESS))
                                    }
                                    is PortalResult.Error -> actionError = result
                                }
                            }
                        ) { Text("Enregistrer") }
                        TextButton(onClick = { saveExpanded = false }, enabled = !busy) { Text("Annuler") }
                    }
                } else {
                    Button(
                        onClick = {
                            teamName = suggestedName
                            saveExpanded = true
                            actionError = null
                        },
                        enabled = !busy && playerOne != null
                    ) { Text("Enregistrer l'équipe actuelle") }
                    if (playerOne == null) {
                        Text(
                            "Charge d'abord un personnage en Joueur 1 pour créer une équipe.",
                            color = PortalPalette.Muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (teams.isEmpty()) {
                    Text("Aucune équipe enregistrée.", color = PortalPalette.Muted)
                } else {
                    teams.forEach { team ->
                        val first = figuresByUri[team.playerOneUri]
                        val second = team.playerTwoUri?.let(figuresByUri::get)
                        val missing = first == null || (team.playerTwoUri != null && second == null)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PortalPalette.PanelRaised),
                            border = BorderStroke(1.dp, if (missing) PortalPalette.Warning else PortalPalette.Accent)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(team.name, color = Color.White, fontWeight = FontWeight.Black)
                                Text(
                                    listOfNotNull(
                                        first?.name ?: "Joueur 1 introuvable",
                                        team.playerTwoUri?.let { second?.name ?: "Joueur 2 introuvable" }
                                    ).joinToString(" • "),
                                    color = if (missing) PortalPalette.Warning else PortalPalette.Muted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        enabled = !busy && !missing,
                                        onClick = {
                                            if (busy) return@Button
                                            busy = true
                                            actionError = null
                                            scope.launch {
                                                when (val result = onLoadTeam(team)) {
                                                    is PortalResult.Success -> {
                                                        onNotice(
                                                            UiNotice(
                                                                result.message ?: "Équipe ${team.name} chargée",
                                                                NoticeKind.SUCCESS
                                                            )
                                                        )
                                                        onDismiss()
                                                    }
                                                    is PortalResult.Error -> actionError = result
                                                }
                                                busy = false
                                            }
                                        }
                                    ) { Text("Charger") }
                                    TextButton(
                                        onClick = { onDeleteTeam(team.id) },
                                        enabled = !busy
                                    ) { Text("Supprimer") }
                                }
                            }
                        }
                    }
                }

                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Chargement de l'équipe…", color = PortalPalette.Muted)
                    }
                }
                actionError?.let { error ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3C222D))) {
                        Column(modifier = Modifier.padding(9.dp)) {
                            Text(error.message, color = PortalPalette.Error, fontWeight = FontWeight.Bold)
                            error.recoveryHint?.let {
                                Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Code : ${error.diagnosticCode}",
                                color = PortalPalette.Muted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, enabled = !busy) { Text("Fermer") }
            }
        }
    }
}

@Composable
private fun DiagnosticsDialog(
    onRunDiagnostics: () -> List<DiagnosticItem>,
    onReconnect: () -> Unit,
    onRescan: () -> Unit,
    onLaunchDolphin: () -> Unit,
    onDismiss: () -> Unit
) {
    var items by remember { mutableStateOf(onRunDiagnostics()) }
    val errors = items.count { it.level == DiagnosticLevel.ERROR }
    val warnings = items.count { it.level == DiagnosticLevel.WARNING }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(10.dp),
            color = PortalPalette.Background,
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Assistant de diagnostic",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            when {
                                errors > 0 -> "$errors erreur(s) • $warnings avertissement(s)"
                                warnings > 0 -> "Prêt avec $warnings avertissement(s)"
                                else -> "Tous les contrôles automatiques sont réussis"
                            },
                            color = if (errors > 0) PortalPalette.Error else if (warnings > 0) PortalPalette.Warning else PortalPalette.Success
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Fermer") }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items.forEach { item -> DiagnosticItemCard(item) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { items = onRunDiagnostics() }) { Text("Actualiser") }
                    OutlinedButton(onClick = onReconnect) { Text("Reconnecter") }
                    OutlinedButton(onClick = onRescan) { Text("Scanner") }
                    OutlinedButton(onClick = onLaunchDolphin) { Text("Dolphin") }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItemCard(item: DiagnosticItem) {
    val color = when (item.level) {
        DiagnosticLevel.SUCCESS -> PortalPalette.Success
        DiagnosticLevel.WARNING -> PortalPalette.Warning
        DiagnosticLevel.ERROR -> PortalPalette.Error
        DiagnosticLevel.INFO -> PortalPalette.Accent
    }
    val symbol = when (item.level) {
        DiagnosticLevel.SUCCESS -> "✓"
        DiagnosticLevel.WARNING -> "!"
        DiagnosticLevel.ERROR -> "×"
        DiagnosticLevel.INFO -> "i"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f)),
        border = BorderStroke(1.dp, color)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(symbol, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(item.detail, color = PortalPalette.Muted, style = MaterialTheme.typography.bodySmall)
                item.recovery?.let {
                    Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PlayerModeDialog(
    playerTwoEnabled: Boolean,
    playerTwoOccupied: Boolean,
    onChange: suspend (Boolean) -> PortalResult,
    onNotice: (UiNotice) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<PortalResult.Error?>(null) }

    fun requestChange(enabled: Boolean) {
        if (busy || enabled == playerTwoEnabled) return
        busy = true
        actionError = null
        scope.launch {
            when (val result = onChange(enabled)) {
                is PortalResult.Success -> {
                    onNotice(
                        UiNotice(
                            result.message ?: if (enabled) "Joueur 2 activé" else "Mode solo activé",
                            NoticeKind.SUCCESS
                        )
                    )
                    onDismiss()
                }
                is PortalResult.Error -> actionError = result
            }
            busy = false
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(color = PortalPalette.Panel, shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Nombre de joueurs",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Le mode solo est recommandé sur une console portable.",
                    color = PortalPalette.Muted
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(enabled = !busy) { requestChange(!playerTwoEnabled) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Activer le joueur 2", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            if (playerTwoEnabled) "Deux cartes de joueur affichées" else "Seul Joueur 1 est affiché",
                            color = PortalPalette.Muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = playerTwoEnabled,
                        onCheckedChange = { requestChange(it) },
                        enabled = !busy
                    )
                }
                if (playerTwoEnabled && playerTwoOccupied) {
                    Text(
                        "Désactiver Joueur 2 retirera d'abord son personnage du portail afin de sauvegarder sa progression.",
                        color = PortalPalette.Warning,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Mise à jour du portail…", color = PortalPalette.Muted)
                    }
                }
                actionError?.let { error ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3C222D))) {
                        Column(modifier = Modifier.padding(9.dp)) {
                            Text(error.message, color = PortalPalette.Error, fontWeight = FontWeight.Bold)
                            error.recoveryHint?.let {
                                Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Code : ${error.diagnosticCode}",
                                color = PortalPalette.Muted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, enabled = !busy) { Text("Fermer") }
            }
        }
    }
}

@Composable
private fun PortalSlotCard(
    title: String,
    slot: PortalSlotState,
    loadState: LoadUiState,
    modifier: Modifier,
    onTap: (PortalSlotState) -> Unit
) {
    val status = slotVisualStatus(slot, loadState)
    val borderColor = when (status) {
        SlotVisualStatus.LOADING -> PortalPalette.Warning
        SlotVisualStatus.SUCCESS -> PortalPalette.Success
        SlotVisualStatus.ERROR -> PortalPalette.Error
        SlotVisualStatus.OCCUPIED -> slot.figure?.let { PortalPalette.element(it.element) } ?: PortalPalette.Accent
        SlotVisualStatus.EMPTY -> PortalPalette.Muted.copy(alpha = 0.5f)
    }
    Card(
        modifier = modifier
            .heightIn(min = 126.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$title, ${slot.figure?.name ?: slot.label ?: "vide"}"
                stateDescription = status.accessibilityLabel
            }
            .clickable(role = Role.Button) { onTap(slot) },
        colors = CardDefaults.cardColors(containerColor = PortalPalette.PanelRaised),
        border = BorderStroke(if (status in setOf(SlotVisualStatus.LOADING, SlotVisualStatus.SUCCESS, SlotVisualStatus.ERROR)) 3.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = PortalPalette.Accent, fontWeight = FontWeight.Black)
                SlotStatusLabel(status)
            }
            if (slot.isOccupied()) {
                Text(
                    slot.figure?.name ?: slot.label.orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    slot.figure?.let { "${it.element} • ${it.generation}" }
                        ?: "Slot Dolphin #${slot.actualPortalSlot}",
                    color = PortalPalette.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Toucher : Changer · Retirer · Backup · Infos",
                    color = PortalPalette.Accent,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(Modifier.height(4.dp))
                Text("SLOT VIDE", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text("Touchez pour choisir un Skylander", color = PortalPalette.Success, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SlotStatusLabel(status: SlotVisualStatus) {
    val color = when (status) {
        SlotVisualStatus.LOADING -> PortalPalette.Warning
        SlotVisualStatus.SUCCESS -> PortalPalette.Success
        SlotVisualStatus.ERROR -> PortalPalette.Error
        SlotVisualStatus.OCCUPIED -> PortalPalette.Success
        SlotVisualStatus.EMPTY -> PortalPalette.Muted
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (status == SlotVisualStatus.LOADING) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = color, strokeWidth = 2.dp)
            Spacer(Modifier.width(5.dp))
        }
        Text(status.visibleLabel, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExtraSlots(
    slots: List<PortalSlotState>,
    loadState: LoadUiState,
    onTap: (PortalSlotState) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(slots, key = { it.logicalSlot }) { slot ->
            val status = slotVisualStatus(slot, loadState)
            Card(
                modifier = Modifier
                    .width(132.dp)
                    .height(58.dp)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = "Slot ${slot.logicalSlot + 1}, ${slot.figure?.name ?: slot.label ?: "vide"}"
                    }
                    .clickable(role = Role.Button) { onTap(slot) },
                colors = CardDefaults.cardColors(containerColor = PortalPalette.Panel),
                border = BorderStroke(1.dp, if (status == SlotVisualStatus.ERROR) PortalPalette.Error else PortalPalette.PanelRaised),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(9.dp)) {
                    Text("SLOT ${slot.logicalSlot + 1}", color = PortalPalette.Accent, fontWeight = FontWeight.Bold)
                    Text(
                        slot.figure?.name ?: slot.label ?: "Toucher pour choisir",
                        color = if (slot.isOccupied()) Color.White else PortalPalette.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageBar(
    rootUri: Uri?,
    scanning: Boolean,
    totalCount: Int,
    playableCount: Int,
    onPickRoot: () -> Unit,
    onRescan: () -> Unit,
    onClear: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = PortalPalette.Panel), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Collection locale", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        scanning -> "Analyse du dossier…"
                        rootUri == null -> "Aucun dossier Skylanders sélectionné"
                        else -> "$playableCount jouable(s) • $totalCount fichier(s) .sky"
                    },
                    color = PortalPalette.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedButton(onClick = onRescan, enabled = rootUri != null && !scanning) { Text("Scanner") }
                OutlinedButton(onClick = onPickRoot, enabled = !scanning) { Text("Dossier") }
                OutlinedButton(onClick = onClear) { Text("Vider") }
            }
        }
    }
}

@Composable
private fun MessageBar(notice: UiNotice, onDismiss: () -> Unit) {
    val color = when (notice.kind) {
        NoticeKind.SUCCESS -> PortalPalette.Success
        NoticeKind.ERROR -> PortalPalette.Error
        NoticeKind.INFO -> PortalPalette.Accent
    }
    Card(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.17f)),
        border = BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(notice.text, color = Color.White, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onDismiss) { Text("OK", color = color) }
        }
    }
}

@Composable
private fun OccupiedSlotDialog(
    slot: PortalSlotState,
    onDismiss: () -> Unit,
    onChange: () -> Unit,
    onInfo: () -> Unit,
    onRemove: suspend (Int) -> PortalResult,
    onBackup: suspend (Int, Skylander) -> PortalResult,
    onNotice: (UiNotice) -> Unit,
    onClearLoadState: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember(slot.logicalSlot) { mutableStateOf(false) }
    var confirmBackup by remember(slot.logicalSlot) { mutableStateOf(false) }
    var actionError by remember(slot.logicalSlot) { mutableStateOf<PortalResult.Error?>(null) }
    val figure = slot.figure

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(color = PortalPalette.Panel, shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    slot.figure?.name ?: slot.label ?: "Slot ${slot.logicalSlot + 1}",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(slotLabel(slot.logicalSlot), color = PortalPalette.Accent)

                if (confirmBackup && figure != null) {
                    Text(
                        "Pour créer une copie cohérente, ${figure.name} sera d'abord retiré du portail.",
                        color = Color.White
                    )
                    Text("Le personnage ne sera pas rechargé automatiquement.", color = PortalPalette.Warning)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !busy,
                            onClick = {
                                if (busy) return@Button
                                busy = true
                                scope.launch {
                                    actionError = null
                                    when (val result = onBackup(slot.logicalSlot, figure)) {
                                        is PortalResult.Success -> {
                                            onClearLoadState()
                                            onNotice(UiNotice(result.message ?: "Backup créé", NoticeKind.SUCCESS))
                                            onDismiss()
                                        }
                                        is PortalResult.Error -> actionError = result
                                    }
                                    busy = false
                                }
                            }
                        ) { Text("Retirer et sauvegarder") }
                        TextButton(onClick = { confirmBackup = false }, enabled = !busy) { Text("Annuler") }
                    }
                } else {
                    ActionButton("Changer", "Ouvrir la collection pour ce slot", !busy, onChange)
                    ActionButton(
                        "Retirer",
                        "Enlever le personnage du portail Dolphin",
                        !busy
                    ) {
                        if (busy) return@ActionButton
                        busy = true
                        scope.launch {
                            actionError = null
                            when (val result = onRemove(slot.logicalSlot)) {
                                is PortalResult.Success -> {
                                    onClearLoadState()
                                    onNotice(UiNotice(result.message ?: "Personnage retiré", NoticeKind.SUCCESS))
                                    onDismiss()
                                }
                                is PortalResult.Error -> actionError = result
                            }
                            busy = false
                        }
                    }
                    ActionButton(
                        "Backup",
                        if (figure != null) "Retirer puis sauvegarder le fichier .sky" else "Fichier source inconnu",
                        !busy && figure != null
                    ) { confirmBackup = true }
                    ActionButton("Informations", "Jeu, élément, fichier et slot Dolphin", !busy, onInfo)
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("Fermer") }
                }

                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Opération en cours…", color = PortalPalette.Muted)
                    }
                }
                actionError?.let { error ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3C222D))) {
                        Column(modifier = Modifier.padding(9.dp)) {
                            Text(error.message, color = PortalPalette.Error, fontWeight = FontWeight.Bold)
                            error.recoveryHint?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall) }
                            Text("Code : ${error.diagnosticCode}", color = PortalPalette.Muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) PortalPalette.PanelRaised else PortalPalette.PanelRaised.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, color = if (enabled) Color.White else PortalPalette.Muted, fontWeight = FontWeight.Bold)
            Text(subtitle, color = PortalPalette.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SlotInfoDialog(slot: PortalSlotState, onDismiss: () -> Unit) {
    val figure = slot.figure
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = PortalPalette.Panel, shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text("Informations", color = PortalPalette.Accent, fontWeight = FontWeight.Black)
                Text(
                    figure?.name ?: slot.label ?: "Slot ${slot.logicalSlot + 1}",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                InfoLine("Emplacement", slotLabel(slot.logicalSlot))
                InfoLine("Slot Dolphin", slot.actualPortalSlot.toString())
                if (figure != null) {
                    InfoLine("Élément", figure.element)
                    InfoLine("Jeu", figure.generation)
                    InfoLine("Type", figure.typeLabel)
                    InfoLine("Fichier", figure.fileName)
                    SelectionContainer {
                        InfoLine("Chemin", figure.relativePath)
                    }
                } else {
                    Text("Chargé par Dolphin, mais le fichier source n'est pas connu de cette session.", color = PortalPalette.Warning)
                }
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Fermer") }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label, color = PortalPalette.Muted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DolphinTargetDialog(
    packages: List<String>,
    selectedPackage: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = PortalPalette.Panel, shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Choisir le Dolphin actif", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    "Le personnage doit être envoyé au même Dolphin que le jeu affiché en haut.",
                    color = PortalPalette.Muted
                )
                packages.forEach { packageName ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onSelect(packageName) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (packageName == selectedPackage) Color(0xFF244E3E) else PortalPalette.PanelRaised
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(DolphinTargets.label(packageName), color = Color.White, fontWeight = FontWeight.Bold)
                            Text(packageName, color = PortalPalette.Muted, style = MaterialTheme.typography.bodySmall)
                            if (packageName == selectedPackage) Text("● Connecté", color = PortalPalette.Success)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Annuler") }
            }
        }
    }
}

private fun PortalSlotState.isOccupied(): Boolean = figure != null || !label.isNullOrBlank() || actualPortalSlot >= 0

private fun slotVisualStatus(slot: PortalSlotState, state: LoadUiState): SlotVisualStatus {
    val stateSlot = when (state) {
        LoadUiState.Idle -> null
        is LoadUiState.Loading -> state.logicalSlot
        is LoadUiState.Success -> state.logicalSlot
        is LoadUiState.Error -> state.logicalSlot
    }
    if (stateSlot == slot.logicalSlot) {
        return when (state) {
            is LoadUiState.Loading -> SlotVisualStatus.LOADING
            is LoadUiState.Success -> SlotVisualStatus.SUCCESS
            is LoadUiState.Error -> SlotVisualStatus.ERROR
            LoadUiState.Idle -> if (slot.isOccupied()) SlotVisualStatus.OCCUPIED else SlotVisualStatus.EMPTY
        }
    }
    return if (slot.isOccupied()) SlotVisualStatus.OCCUPIED else SlotVisualStatus.EMPTY
}

private enum class SlotVisualStatus(val visibleLabel: String, val accessibilityLabel: String) {
    EMPTY("VIDE", "Slot vide"),
    OCCUPIED("● ACTIF", "Sur le portail"),
    LOADING("PLACEMENT…", "Chargement en cours"),
    SUCCESS("✓ CHARGÉ", "Chargement réussi"),
    ERROR("⚠ ÉCHEC", "Dernier chargement échoué")
}

private fun slotLabel(logicalSlot: Int): String = when (logicalSlot) {
    0 -> "Joueur 1"
    1 -> "Joueur 2"
    else -> "Slot ${logicalSlot + 1}"
}

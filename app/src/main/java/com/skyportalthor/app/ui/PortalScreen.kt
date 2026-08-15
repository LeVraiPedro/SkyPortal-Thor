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
import androidx.compose.material3.Surface
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
import com.skyportalthor.app.data.FigureKind
import com.skyportalthor.app.data.Skylander
import com.skyportalthor.app.dolphin.DolphinTargets
import com.skyportalthor.app.portal.PortalResult
import com.skyportalthor.app.portal.PortalSlotState
import com.skyportalthor.app.portal.PortalState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PortalScreen(
    portalState: PortalState,
    figures: List<Skylander>,
    rootUri: Uri?,
    scanning: Boolean,
    uiMessage: UiNotice?,
    onDismissMessage: () -> Unit,
    onPickRoot: () -> Unit,
    onRescan: () -> Unit,
    onReconnect: () -> Unit,
    onSelectDolphinPackage: (String) -> Unit,
    onLaunchDolphin: () -> Unit,
    onLoad: suspend (Int, Skylander) -> PortalResult,
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
                    onReconnect = onReconnect,
                    onChooseTarget = { showDolphinTargets = true },
                    onLaunchDolphin = onLaunchDolphin
                )

                when {
                    localNotice != null -> MessageBar(localNotice!!) { localNotice = null }
                    uiMessage != null -> MessageBar(uiMessage, onDismissMessage)
                }

                PrimarySlots(
                    slots = portalState.slots,
                    loadState = loadState,
                    onTap = { slot ->
                        if (slot.isOccupied()) actionSlot = slot else pickerSlot = slot.logicalSlot
                    }
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

                if (!compactFeedback) {
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.cardColors(containerColor = PortalPalette.Panel),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Touchez Joueur 1 ou Joueur 2",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    "La collection s'ouvrira directement. La recherche reste disponible dans la sélection.",
                                    color = PortalPalette.Muted
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pickerSlot?.let { logicalSlot ->
        SkylanderPickerDialog(
            logicalSlot = logicalSlot,
            figures = figures,
            occupiedUris = portalState.slots.mapNotNull { it.sourceUri }.toSet(),
            portalConnected = portalState.connected,
            portalMessage = portalState.message,
            loadState = loadState,
            onLoadStateChange = { loadState = it },
            onDismiss = { pickerSlot = null },
            onPickRoot = onPickRoot,
            onReconnect = onReconnect,
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
}

@Composable
private fun Header(
    portalState: PortalState,
    onReconnect: () -> Unit,
    onChooseTarget: () -> Unit,
    onLaunchDolphin: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = PortalPalette.Panel), shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (portalState.connected) "SKYPORTAL THOR V3" else "SKYPORTAL V3",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (portalState.connected) "● ${portalState.message}" else "● ${portalState.message}",
                    color = if (portalState.connected) PortalPalette.Success else PortalPalette.Warning,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (portalState.availablePackages.size > 1) {
                    OutlinedButton(onClick = onChooseTarget) { Text("Cible") }
                }
                if (!portalState.connected) {
                    OutlinedButton(onClick = onReconnect) { Text("Reconnecter") }
                }
                Button(onClick = onLaunchDolphin) { Text("Dolphin en haut") }
            }
        }
    }
}

@Composable
private fun PrimarySlots(
    slots: List<PortalSlotState>,
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
        PortalSlotCard(
            title = "JOUEUR 2",
            slot = slots.getOrElse(1) { PortalSlotState(1) },
            loadState = loadState,
            modifier = Modifier.weight(1f),
            onTap = onTap
        )
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

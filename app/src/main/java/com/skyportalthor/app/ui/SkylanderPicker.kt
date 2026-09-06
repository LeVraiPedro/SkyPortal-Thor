// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.skyportalthor.app.data.FigureCompatibilityEngine
import com.skyportalthor.app.data.FigureFilterPolicy
import com.skyportalthor.app.data.FigureKind
import com.skyportalthor.app.data.Skylander
import com.skyportalthor.app.data.SkylandersGame
import com.skyportalthor.app.portal.PortalResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun SkylanderPickerDialog(
    logicalSlot: Int,
    figures: List<Skylander>,
    occupiedUris: Set<String>,
    favoriteUris: Set<String>,
    recentUris: List<String>,
    portalConnected: Boolean,
    portalMessage: String,
    detectedGame: SkylandersGame?,
    requireNativeIdentity: Boolean,
    loadState: LoadUiState,
    onLoadStateChange: (LoadUiState) -> Unit,
    onDismiss: () -> Unit,
    onPickRoot: () -> Unit,
    onReconnect: () -> Unit,
    onToggleFavorite: (Skylander) -> Unit,
    onLoad: suspend (Int, Skylander) -> PortalResult
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var query by remember(logicalSlot) { mutableStateOf("") }
    var generation by remember(logicalSlot) { mutableStateOf("Tous") }
    var element by remember(logicalSlot) { mutableStateOf("Tous") }
    var typeFilter by remember(logicalSlot) { mutableStateOf("Tous") }
    var collectionView by remember(logicalSlot) { mutableStateOf(CollectionView.ALL) }
    var searchExpanded by remember(logicalSlot) { mutableStateOf(false) }
    var detailsExpanded by remember(logicalSlot) { mutableStateOf(false) }
    var filtersExpanded by remember(logicalSlot) { mutableStateOf(false) }
    var launchInFlight by remember(logicalSlot) { mutableStateOf(false) }
    var category by remember(logicalSlot) { mutableStateOf(FigureCategory.CHARACTERS) }
    var smartFilterEnabled by remember(logicalSlot, detectedGame) { mutableStateOf(detectedGame != null) }

    val available = remember(figures, category, detectedGame, smartFilterEnabled, requireNativeIdentity) {
        figures.filter { figure ->
            FigureFilterPolicy.visible(
                kind = figure.kind,
                charactersCategory = category == FigureCategory.CHARACTERS,
                smartFilterEnabled = smartFilterEnabled,
                compatible = FigureCompatibilityEngine.check(
                    figure,
                    detectedGame,
                    requireNativeIdentity
                ).compatible
            )
        }
    }
    val generations = remember(available) {
        listOf("Tous") + available.map { it.generation }.distinct().sortedBy(::generationOrder)
    }
    val elements = remember(available) {
        val preferred = listOf("Magic", "Water", "Tech", "Fire", "Earth", "Life", "Undead", "Air", "Light", "Dark", "Kaos")
        listOf("Tous") + available.map { it.element }.distinct().sortedBy {
            preferred.indexOf(it).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
        }
    }
    val types = remember(available) {
        listOf("Tous") + available.map { it.typeLabel }.distinct().sorted()
    }
    val activeType = typeFilter.takeIf { it in types } ?: "Tous"
    val filtered = remember(
        available,
        query,
        generation,
        element,
        activeType,
        searchExpanded,
        collectionView,
        favoriteUris,
        recentUris
    ) {
        val recentOrder = recentUris.withIndex().associate { it.value to it.index }
        val visible = if (searchExpanded) {
            available
        } else {
            when (collectionView) {
                CollectionView.ALL -> available
                CollectionView.FAVORITES -> available.filter { it.documentUri.toString() in favoriteUris }
                CollectionView.RECENTS -> available.filter { it.documentUri.toString() in recentOrder }
            }
        }
        visible.filter { figure ->
            if (searchExpanded) {
                query.isBlank() || figure.name.contains(query, true) || figure.fileName.contains(query, true)
            } else {
                (generation == "Tous" || figure.generation == generation) &&
                    (element == "Tous" || figure.element == element) &&
                    (activeType == "Tous" || figure.typeLabel == activeType)
            }
        }.let { result ->
            if (!searchExpanded && collectionView == CollectionView.RECENTS) {
                result.sortedBy { recentOrder[it.documentUri.toString()] ?: Int.MAX_VALUE }
            } else {
                result
            }
        }
    }
    val currentState = loadState.takeIf { stateSlot(it) == logicalSlot } ?: LoadUiState.Idle
    val busy = launchInFlight || currentState is LoadUiState.Loading || currentState is LoadUiState.Success

    fun requestLoad(figure: Skylander) {
        if (
            launchInFlight || currentState is LoadUiState.Loading ||
            currentState is LoadUiState.Success || figure.documentUri.toString() in occupiedUris
        ) return
        launchInFlight = true
        focusManager.clearFocus(force = true)
        scope.launch {
            detailsExpanded = false
            onLoadStateChange(LoadUiState.Loading(logicalSlot, figure))
            val result = runCatching { onLoad(logicalSlot, figure) }.getOrElse { error ->
                PortalResult.Error(
                    message = "Le chargement a été interrompu",
                    diagnosticCode = "UI_LOAD_EXCEPTION",
                    technicalDetails = "${error.javaClass.simpleName}: ${error.message ?: "sans détail"}",
                    recoveryHint = "Réessaie après avoir reconnecté Dolphin."
                )
            }
            when (result) {
                is PortalResult.Success -> {
                    onLoadStateChange(LoadUiState.Success(logicalSlot, figure, result))
                    delay(SUCCESS_DISPLAY_MS)
                    launchInFlight = false
                    onDismiss()
                }
                is PortalResult.Error -> {
                    onLoadStateChange(LoadUiState.Error(logicalSlot, figure, result))
                    launchInFlight = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(10.dp),
            color = PortalPalette.Background,
            shape = RoundedCornerShape(28.dp)
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(18.dp)) {
                val filtersMaxHeight = (maxHeight * 0.3f).coerceAtMost(112.dp)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Collection",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = PortalPalette.Accent.copy(alpha = 0.12f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        slotLabel(logicalSlot),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        color = PortalPalette.Accent,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Text(
                                "${filtered.size} au choix · toucher pour placer",
                                color = PortalPalette.Muted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (currentState !is LoadUiState.Error) {
                            TextButton(
                                onClick = {
                                    if (searchExpanded) focusManager.clearFocus(force = true)
                                    searchExpanded = !searchExpanded
                                },
                                enabled = !busy
                            ) {
                                Text(if (searchExpanded) "Annuler" else "Rechercher")
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        TextButton(
                            onClick = onDismiss,
                            enabled = !busy,
                            modifier = Modifier.size(48.dp).semantics { contentDescription = "Fermer la collection" },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("×", style = MaterialTheme.typography.headlineSmall)
                        }
                    }

                    if (currentState !is LoadUiState.Error) {
                        if (!searchExpanded) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FigureCategory.entries.forEach { entry ->
                                    PickerTab(
                                        label = entry.label,
                                        selected = category == entry,
                                        enabled = !busy,
                                        onClick = {
                                            category = entry
                                            typeFilter = "Tous"
                                        }
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(
                                    onClick = { filtersExpanded = !filtersExpanded },
                                    enabled = !busy
                                ) {
                                    Text(if (filtersExpanded) "Réduire" else "Filtres")
                                }
                            }
                            if (!filtersExpanded) {
                                PickerCollectionViews(collectionView, !busy) { collectionView = it }
                            }
                            if (detectedGame != null) {
                                Text(
                                    if (smartFilterEnabled) {
                                        "Pour ${detectedGame.displayName}"
                                    } else {
                                        "Toute la collection · compatibilité vérifiée au chargement"
                                    },
                                    color = PortalPalette.Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (filtersExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = filtersMaxHeight)
                                        .background(PortalPalette.Panel, RoundedCornerShape(20.dp))
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    PickerCollectionViews(collectionView, !busy) { collectionView = it }
                                    if (detectedGame != null) {
                                        FilterChip(
                                            selected = smartFilterEnabled,
                                            onClick = { smartFilterEnabled = !smartFilterEnabled },
                                            enabled = !busy,
                                            label = {
                                                Text(if (smartFilterEnabled) "Compatibles avec le jeu" else "Toute la collection")
                                            }
                                        )
                                    }
                                    PickerFilterRow("Élément", elements, element, !busy) { element = it }
                                    if (types.size > 2) {
                                        PickerFilterRow("Type", types, activeType, !busy) { typeFilter = it }
                                    }
                                    PickerFilterRow("Jeu", generations, generation, !busy) { generation = it }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                label = { Text("Trouver un personnage ou un objet") },
                                trailingIcon = {
                                    if (query.isNotBlank()) {
                                        TextButton(onClick = { query = "" }) { Text("Effacer") }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { focusManager.clearFocus(force = true) }
                                )
                            )
                        }
                    }

                    when (currentState) {
                        is LoadUiState.Loading -> LoadingPanel(currentState.figure)
                        is LoadUiState.Success -> SuccessPanel(currentState.figure)
                        is LoadUiState.Error -> ErrorPanel(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            state = currentState,
                            detailsExpanded = detailsExpanded,
                            onToggleDetails = { detailsExpanded = !detailsExpanded },
                            onRetry = { requestLoad(currentState.figure) },
                            onReconnect = onReconnect,
                            onChooseAnother = {
                                detailsExpanded = false
                                onLoadStateChange(LoadUiState.Idle)
                            },
                            portalConnected = portalConnected,
                            portalMessage = portalMessage
                        )
                        LoadUiState.Idle -> Unit
                    }

                    if (currentState is LoadUiState.Error) {
                        Unit
                    } else if (figures.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Aucun Skylander jouable détecté", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Sélectionne le dossier qui contient tes fichiers .sky.", color = PortalPalette.Muted)
                                Button(onClick = onPickRoot, modifier = Modifier.padding(top = 12.dp)) {
                                    Text("Choisir le dossier")
                                }
                            }
                        }
                    } else if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    when (collectionView) {
                                        CollectionView.FAVORITES -> "Aucun favori avec ces filtres"
                                        CollectionView.RECENTS -> "Aucun personnage récent avec ces filtres"
                                        CollectionView.ALL -> "Aucun résultat avec ces filtres"
                                    },
                                    color = Color.White
                                )
                                TextButton(onClick = {
                                    query = ""
                                    generation = "Tous"
                                    element = "Tous"
                                    typeFilter = "Tous"
                                    collectionView = CollectionView.ALL
                                }) { Text("Réinitialiser les filtres") }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(144.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.documentUri.toString() }) { figure ->
                                val occupied = figure.documentUri.toString() in occupiedUris
                                val loading = currentState is LoadUiState.Loading && currentState.figure.documentUri == figure.documentUri
                                FigureGridCard(
                                    figure = figure,
                                    occupied = occupied,
                                    favorite = figure.documentUri.toString() in favoriteUris,
                                    showGeneration = generations.size > 2,
                                    loading = loading,
                                    enabled = !busy,
                                    onToggleFavorite = { onToggleFavorite(figure) },
                                    onClick = { requestLoad(figure) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerCollectionViews(selected: CollectionView, enabled: Boolean, onSelect: (CollectionView) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CollectionView.entries, key = { it.name }) { entry ->
            PickerTab(
                label = entry.label,
                selected = selected == entry,
                enabled = enabled,
                subtle = true,
                onClick = { onSelect(entry) }
            )
        }
    }
}

@Composable
private fun PickerTab(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    subtle: Boolean = false,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = {
            Text(
                label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        shape = CircleShape,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = PortalPalette.Muted,
            selectedContainerColor = if (subtle) PortalPalette.PanelRaised else PortalPalette.Accent,
            selectedLabelColor = if (subtle) Color.White else PortalPalette.Background
        )
    )
}

@Composable
private fun PickerFilterRow(
    label: String,
    values: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = PortalPalette.Muted,
            modifier = Modifier.width(76.dp),
            maxLines = 1
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(values, key = { it }) { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    enabled = enabled,
                    label = { Text(value) },
                    shape = CircleShape,
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PortalPalette.PanelRaised
                    )
                )
            }
        }
    }
}

@Composable
private fun FigureGridCard(
    figure: Skylander,
    occupied: Boolean,
    favorite: Boolean,
    showGeneration: Boolean,
    loading: Boolean,
    enabled: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val canClick = enabled && !occupied
    val elementColor = PortalPalette.element(figure.element)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 164.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${figure.name}, ${figure.element}, ${figure.generation}"
                stateDescription = when {
                    loading -> "Chargement en cours"
                    occupied -> "Déjà sur le portail"
                    else -> "Disponible"
                }
            }
            .clickable(enabled = canClick, role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = PortalPalette.Panel.copy(alpha = if (occupied) 0.6f else 1f)
        ),
        border = if (loading) BorderStroke(2.dp, PortalPalette.Warning) else null,
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(94.dp),
            contentAlignment = Alignment.Center
        ) {
            FigureMonogram(figure, elementColor, occupied)
            TextButton(
                onClick = onToggleFavorite,
                enabled = enabled,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.align(Alignment.TopEnd).size(48.dp).semantics {
                    contentDescription = if (favorite) "Retirer ${figure.name} des favoris"
                    else "Ajouter ${figure.name} aux favoris"
                }
            ) {
                Text(
                    if (favorite) "★" else "☆",
                    color = if (favorite) PortalPalette.Warning else PortalPalette.Muted,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(22.dp),
                    color = PortalPalette.Warning,
                    strokeWidth = 2.dp
                )
            }
        }
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                figure.name,
                color = if (occupied) PortalPalette.Muted else Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(
                    if (figure.kind == FigureKind.CHARACTER) figure.element else figure.typeLabel,
                    figure.generation.takeIf { showGeneration }
                ).joinToString(" · "),
                color = elementColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (occupied) {
                Text("Sur le portail", color = PortalPalette.Warning, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun FigureMonogram(figure: Skylander, color: Color, occupied: Boolean) {
    val initials = remember(figure.name) {
        figure.name.split(' ', '-').filter { it.isNotBlank() }.take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    }
    Box(
        modifier = Modifier.size(84.dp).background(
            Brush.radialGradient(listOf(color.copy(alpha = if (occupied) 0.07f else 0.16f), Color.Transparent)),
            CircleShape
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(64.dp)) {
            val radius = size.minDimension * 0.4f
            drawCircle(color.copy(alpha = 0.2f), radius, style = Stroke(width = 1.dp.toPx()))
            drawArc(
                color = color.copy(alpha = 0.45f),
                startAngle = 205f,
                sweepAngle = 70f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(color.copy(alpha = 0.7f), 2.dp.toPx(), Offset(size.width * 0.86f, size.height * 0.78f))
        }
        Text(
            initials,
            color = color.copy(alpha = if (occupied) 0.45f else 0.9f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
private fun LoadingPanel(figure: Skylander) {
    FeedbackPanel(PortalPalette.Warning) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PortalPalette.Warning, strokeWidth = 3.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Placement de ${figure.name}…", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Un instant…", color = PortalPalette.Muted)
        }
    }
}

@Composable
private fun SuccessPanel(figure: Skylander) {
    FeedbackPanel(PortalPalette.Success) {
        Text("✓", color = PortalPalette.Success, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(10.dp))
        Column {
            Text("${figure.name} est sur le portail", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Placement confirmé", color = PortalPalette.Muted)
        }
    }
}

@Composable
private fun ErrorPanel(
    modifier: Modifier = Modifier,
    state: LoadUiState.Error,
    detailsExpanded: Boolean,
    onToggleDetails: () -> Unit,
    onRetry: () -> Unit,
    onReconnect: () -> Unit,
    onChooseAnother: () -> Unit,
    portalConnected: Boolean,
    portalMessage: String
) {
    Card(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3C222D)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Impossible de placer ${state.figure.name}", color = Color.White, fontWeight = FontWeight.Black)
            Text(state.result.message, color = PortalPalette.Error)
            state.result.recoveryHint?.let { Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall) }
            Text(
                "Dolphin : $portalMessage",
                color = if (portalConnected) PortalPalette.Success else PortalPalette.Warning,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onRetry) { Text("Réessayer") }
                if (
                    !portalConnected &&
                    (state.result.diagnosticCode.contains("DOLPHIN") || state.result.diagnosticCode.contains("BINDER"))
                ) {
                    OutlinedButton(
                        onClick = onReconnect,
                        enabled = !portalMessage.startsWith("Connexion à")
                    ) { Text(if (portalConnected) "Reconnecté" else "Reconnecter") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onChooseAnother) { Text("Choisir un autre") }
                TextButton(onClick = onToggleDetails) {
                    Text(if (detailsExpanded) "Masquer détails" else "Voir détails")
                }
            }
            if (detailsExpanded) {
                Text(
                    "Code : ${state.result.diagnosticCode}\n${state.result.technicalDetails.orEmpty()}",
                    color = PortalPalette.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun FeedbackPanel(color: Color, content: @Composable RowScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

private fun stateSlot(state: LoadUiState): Int? = when (state) {
    LoadUiState.Idle -> null
    is LoadUiState.Loading -> state.logicalSlot
    is LoadUiState.Success -> state.logicalSlot
    is LoadUiState.Error -> state.logicalSlot
}

private fun slotLabel(logicalSlot: Int): String = when (logicalSlot) {
    0 -> "Joueur 1"
    1 -> "Joueur 2"
    else -> "Slot ${logicalSlot + 1}"
}

private fun generationOrder(value: String): Int = when (value) {
    "Spyro's Adventure" -> 1
    "Giants" -> 2
    "Swap Force" -> 3
    "Trap Team" -> 4
    "SuperChargers" -> 5
    "Imaginators" -> 6
    "Tous" -> 0
    else -> 99
}

private const val SUCCESS_DISPLAY_MS = 550L

private enum class CollectionView(val label: String) {
    ALL("Tous"),
    FAVORITES("★ Favoris"),
    RECENTS("Récents")
}

private enum class FigureCategory(val label: String) {
    CHARACTERS("Personnages"),
    OBJECTS("Objets")
}

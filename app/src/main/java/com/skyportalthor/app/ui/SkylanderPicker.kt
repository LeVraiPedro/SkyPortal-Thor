package com.skyportalthor.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
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
import com.skyportalthor.app.data.FigureKind
import com.skyportalthor.app.data.FigureCompatibilityEngine
import com.skyportalthor.app.data.FigureFilterPolicy
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
                            "Choisir pour ${slotLabel(logicalSlot)}",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "${filtered.size} résultat(s) • toucher pour placer immédiatement",
                            color = PortalPalette.Muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (currentState !is LoadUiState.Error) {
                        OutlinedButton(
                            onClick = {
                                if (searchExpanded) focusManager.clearFocus(force = true)
                                searchExpanded = !searchExpanded
                            },
                            enabled = !busy
                        ) {
                            Text(if (searchExpanded) "Masquer recherche" else "Rechercher")
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("Fermer") }
                }

                if (currentState !is LoadUiState.Error) {
                    if (!searchExpanded) {
                        PickerFilterRow(
                            "Contenu",
                            FigureCategory.entries.map { it.label },
                            category.label,
                            !busy
                        ) { label ->
                            category = FigureCategory.entries.first { it.label == label }
                            typeFilter = "Tous"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (detectedGame != null) {
                                FilterChip(
                                    selected = smartFilterEnabled,
                                    onClick = { smartFilterEnabled = !smartFilterEnabled },
                                    enabled = !busy,
                                    label = {
                                        Text(if (smartFilterEnabled) "Compatible ${detectedGame.displayName}" else "Toute la collection")
                                    }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = { filtersExpanded = !filtersExpanded },
                                enabled = !busy
                            ) {
                                Text(if (filtersExpanded) "Masquer filtres" else "Filtres")
                            }
                        }
                        if (filtersExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 164.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PickerFilterRow(
                                    "Vue",
                                    CollectionView.entries.map { it.label },
                                    collectionView.label,
                                    !busy
                                ) { label -> collectionView = CollectionView.entries.first { it.label == label } }
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
                            label = { Text("Nom ou fichier .sky") },
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
                                collectionView = CollectionView.ALL
                            }) { Text("Réinitialiser les filtres") }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.documentUri.toString() }) { figure ->
                            val occupied = figure.documentUri.toString() in occupiedUris
                            val loading = currentState is LoadUiState.Loading && currentState.figure.documentUri == figure.documentUri
                            FigureGridCard(
                                figure = figure,
                                occupied = occupied,
                                favorite = figure.documentUri.toString() in favoriteUris,
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
            "$label :",
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
                    label = { Text(value) }
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
            .heightIn(min = 112.dp)
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
            containerColor = if (occupied) PortalPalette.Panel.copy(alpha = 0.55f) else PortalPalette.PanelRaised
        ),
        border = BorderStroke(if (loading) 3.dp else 1.dp, if (loading) PortalPalette.Warning else elementColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(elementColor.copy(alpha = 0.18f)).padding(start = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(figure.element, color = elementColor, fontWeight = FontWeight.Black)
            TextButton(
                onClick = onToggleFavorite,
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = if (favorite) {
                        "Retirer ${figure.name} des favoris"
                    } else {
                        "Ajouter ${figure.name} aux favoris"
                    }
                }
            ) {
                Text(if (favorite) "★" else "☆", color = if (favorite) PortalPalette.Warning else PortalPalette.Muted)
            }
        }
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        figure.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        figure.generation,
                        color = PortalPalette.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = PortalPalette.Warning,
                        strokeWidth = 3.dp
                    )
                }
            }
            Text(
                if (occupied) "Déjà sur le portail" else "Toucher pour charger",
                color = if (occupied) PortalPalette.Warning else PortalPalette.Success,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LoadingPanel(figure: Skylander) {
    FeedbackPanel(PortalPalette.Warning) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PortalPalette.Warning, strokeWidth = 3.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Placement de ${figure.name}…", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Vérification du fichier puis envoi à Dolphin", color = PortalPalette.Muted)
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
            Text("La sélection va se fermer automatiquement.", color = PortalPalette.Muted)
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
        border = BorderStroke(1.dp, PortalPalette.Error)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
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
        border = BorderStroke(1.dp, color)
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

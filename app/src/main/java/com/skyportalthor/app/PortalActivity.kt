package com.skyportalthor.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.skyportalthor.app.data.Skylander
import com.skyportalthor.app.data.DolphinFigureCatalog
import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.data.FigureCompatibilityEngine
import com.skyportalthor.app.data.FigureKey
import com.skyportalthor.app.data.SmartPortalReadiness
import com.skyportalthor.app.data.QuickTeam
import com.skyportalthor.app.data.CollectionStateLogic
import com.skyportalthor.app.diagnostics.DiagnosticAssistant
import com.skyportalthor.app.dolphin.DolphinLauncher
import com.skyportalthor.app.dolphin.DolphinPortalBridge
import com.skyportalthor.app.portal.PortalResult
import com.skyportalthor.app.portal.PortalProtocol
import com.skyportalthor.app.storage.BackupRepository
import com.skyportalthor.app.storage.CollectionPreferences
import com.skyportalthor.app.storage.SkylanderCollectionRepository
import com.skyportalthor.app.ui.PortalScreen
import com.skyportalthor.app.ui.PortalColorScheme
import com.skyportalthor.app.ui.NoticeKind
import com.skyportalthor.app.ui.UiNotice
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PortalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val scope = rememberCoroutineScope()
            val prefs = remember { CollectionPreferences(applicationContext) }
            val repository = remember { SkylanderCollectionRepository(applicationContext) }
            val backups = remember { BackupRepository(applicationContext) }
            val diagnosticAssistant = remember { DiagnosticAssistant(applicationContext) }
            val bridge = remember { DolphinPortalBridge(applicationContext) }
            val portalState by bridge.state.collectAsState()

            var rootUri by remember { mutableStateOf(prefs.getRootUri()) }
            var figures by remember { mutableStateOf<List<Skylander>>(emptyList()) }
            var scanning by remember { mutableStateOf(false) }
            var scanGeneration by remember { mutableIntStateOf(0) }
            var uiMessage by remember { mutableStateOf<UiNotice?>(null) }
            var playerTwoEnabled by remember { mutableStateOf(prefs.isPlayerTwoEnabled()) }
            var favoriteUris by remember { mutableStateOf(prefs.getFavoriteUris()) }
            var recentUris by remember { mutableStateOf(prefs.getRecentUris()) }
            var quickTeams by remember { mutableStateOf(prefs.getQuickTeams()) }
            var autoActivationAttemptedFor by remember { mutableStateOf<String?>(null) }
            val reconciledPortalState = remember(portalState, figures) {
                val figuresByUri = figures.associateBy { it.documentUri.toString() }
                portalState.copy(
                    slots = portalState.slots.map { slot ->
                        slot.copy(figure = slot.sourceUri?.let(figuresByUri::get))
                    }
                )
            }

            suspend fun rescan(uri: Uri?) {
                val generation = ++scanGeneration
                if (uri == null) {
                    if (generation == scanGeneration) figures = emptyList()
                    return
                }
                scanning = true
                try {
                    val result = runCatching { repository.scan(uri) }
                    if (generation == scanGeneration) {
                        figures = result.onFailure {
                            uiMessage = UiNotice("Lecture du dossier impossible : ${it.message}", NoticeKind.ERROR)
                        }.getOrDefault(emptyList()).enrich(portalState.figureCatalog)
                    }
                } finally {
                    if (generation == scanGeneration) scanning = false
                }
            }

            val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
                        .onSuccess {
                            prefs.setRootUri(uri)
                            rootUri = uri
                            uiMessage = null
                            scope.launch { rescan(uri) }
                        }
                        .onFailure {
                            uiMessage = UiNotice(
                                "Android n'a pas accordé l'accès durable au dossier : ${it.message}",
                                NoticeKind.ERROR
                            )
                        }
                }
            }

            LaunchedEffect(Unit) {
                bridge.connect(prefs.getDolphinPackage())
                rescan(rootUri)
            }

            LaunchedEffect(bridge) {
                while (isActive) {
                    delay(2_000L)
                    if (bridge.state.value.connected) {
                        bridge.refresh()
                    } else {
                        bridge.connect(prefs.getDolphinPackage())
                    }
                }
            }

            LaunchedEffect(portalState.figureCatalog) {
                if (portalState.figureCatalog.isNotEmpty()) {
                    figures = figures.enrich(portalState.figureCatalog)
                }
            }

            LaunchedEffect(portalState.readiness, portalState.gameId) {
                val key = portalState.gameId ?: portalState.gameTitle
                if (
                    portalState.readiness == SmartPortalReadiness.PORTAL_DISABLED &&
                    portalState.canSetPortalEnabled && key != null && autoActivationAttemptedFor != key
                ) {
                    autoActivationAttemptedFor = key
                    when (val result = bridge.setPortalEnabled(true)) {
                        is PortalResult.Error -> uiMessage = UiNotice(result.message, NoticeKind.ERROR)
                        is PortalResult.Success -> uiMessage = UiNotice(result.message ?: "Portail activé", NoticeKind.SUCCESS)
                    }
                } else if (
                    !portalState.connected ||
                    portalState.emulationState == EmulationState.NONE ||
                    portalState.emulationState == EmulationState.STOPPING
                ) {
                    autoActivationAttemptedFor = null
                }
            }

            LaunchedEffect(
                portalState.gameId,
                portalState.emulationState,
                portalState.nativeSlots,
                portalState.figureCatalog
            ) {
                val game = portalState.skylandersGame
                if (
                    game != null &&
                    (portalState.apiVersion ?: 1) >= 3 &&
                    portalState.figureCatalog.isNotEmpty() &&
                    portalState.emulationState in setOf(EmulationState.RUNNING, EmulationState.PAUSED)
                ) {
                    for (slot in portalState.slots) {
                        if (!PortalProtocol.isValidActualSlot(slot.actualPortalSlot)) continue
                        val native = portalState.nativeSlots.firstOrNull {
                            it.slot == slot.actualPortalSlot && it.occupied
                        } ?: continue
                        val metadata = native.figureId?.let { id ->
                            native.variantId?.let { variant -> portalState.figureCatalog[FigureKey(id, variant)] }
                        }
                        val compatibility = metadata?.let {
                            FigureCompatibilityEngine.check(
                                generationNumber = it.generation,
                                kind = it.kind,
                                typeLabel = it.typeLabel,
                                name = it.canonicalName,
                                generationName = DolphinFigureCatalog.generationName(it.generation),
                                game = game
                            )
                        }
                        if (metadata == null || compatibility?.compatible == false) {
                            when (val result = bridge.remove(slot.logicalSlot)) {
                                is PortalResult.Success -> uiMessage = UiNotice(
                                    "${metadata?.canonicalName ?: "Contenu inconnu"} retiré : incompatible avec ${game.displayName}",
                                    NoticeKind.ERROR
                                )
                                is PortalResult.Error -> uiMessage = UiNotice(
                                    "Impossible de retirer un ancien slot incompatible : ${result.message}",
                                    NoticeKind.ERROR
                                )
                            }
                        }
                    }
                }
            }

            DisposableEffect(bridge) {
                onDispose { bridge.close() }
            }

            suspend fun setPlayerTwoMode(enabled: Boolean): PortalResult {
                if (enabled == playerTwoEnabled) return PortalResult.Success()
                val playerTwoSlot = bridge.state.value.slots.getOrNull(1)
                if (
                    !enabled &&
                    playerTwoSlot != null &&
                    PortalProtocol.isValidActualSlot(playerTwoSlot.actualPortalSlot)
                ) {
                    when (val removeResult = bridge.remove(1)) {
                        is PortalResult.Error -> return removeResult
                        is PortalResult.Success -> Unit
                    }
                }
                prefs.setPlayerTwoEnabled(enabled)
                playerTwoEnabled = enabled
                return PortalResult.Success(
                    message = if (enabled) "Joueur 2 activé" else "Mode solo activé"
                )
            }

            MaterialTheme(colorScheme = PortalColorScheme) {
                PortalScreen(
                    portalState = reconciledPortalState,
                    figures = figures,
                    playerTwoEnabled = playerTwoEnabled,
                    favoriteUris = favoriteUris,
                    recentUris = recentUris,
                    quickTeams = quickTeams,
                    rootUri = rootUri,
                    scanning = scanning,
                    uiMessage = uiMessage,
                    onDismissMessage = { uiMessage = null },
                    onPickRoot = { picker.launch(rootUri) },
                    onRescan = { scope.launch { rescan(rootUri) } },
                    onReconnect = {
                        scope.launch {
                            val connected = bridge.connect(prefs.getDolphinPackage())
                            if (!connected) uiMessage = UiNotice(bridge.state.value.message, NoticeKind.ERROR)
                        }
                    },
                    onSelectDolphinPackage = { packageName ->
                        scope.launch {
                            val connected = bridge.connect(packageName)
                            uiMessage = if (connected) {
                                prefs.setDolphinPackage(packageName)
                                UiNotice("Connexion basculée vers ${bridge.state.value.message}", NoticeKind.SUCCESS)
                            } else {
                                UiNotice(bridge.state.value.message, NoticeKind.ERROR)
                            }
                        }
                    },
                    onLaunchDolphin = {
                        val target = portalState.connectedPackage ?: prefs.getDolphinPackage()
                        if (!DolphinLauncher.launchOnPrimaryDisplay(this@PortalActivity, target)) {
                            uiMessage = UiNotice("Dolphin n'est pas installé sur la Thor", NoticeKind.ERROR)
                        }
                    },
                    onSetPortalEnabled = { enabled ->
                        scope.launch {
                            when (val result = bridge.setPortalEnabled(enabled)) {
                                is PortalResult.Success -> uiMessage = UiNotice(result.message ?: "État du portail modifié", NoticeKind.SUCCESS)
                                is PortalResult.Error -> uiMessage = UiNotice(result.message, NoticeKind.ERROR)
                            }
                        }
                    },
                    onLoad = { logicalSlot, figure ->
                        bridge.load(logicalSlot, figure).also { result ->
                            if (result is PortalResult.Success) {
                                recentUris = prefs.recordRecent(figure.documentUri.toString())
                            }
                        }
                    },
                    onToggleFavorite = { figure ->
                        favoriteUris = prefs.toggleFavorite(figure.documentUri.toString())
                    },
                    onSaveCurrentTeam = saveTeam@{ name ->
                        val first = reconciledPortalState.slots.getOrNull(0)?.figure
                            ?: return@saveTeam PortalResult.Error(
                                message = "Joueur 1 est vide",
                                diagnosticCode = "TEAM_PLAYER_ONE_EMPTY",
                                recoveryHint = "Charge un personnage en Joueur 1 puis réessaie."
                            )
                        val second = reconciledPortalState.slots.getOrNull(1)?.figure.takeIf { playerTwoEnabled }
                        val team = QuickTeam(
                            id = System.currentTimeMillis().toString(),
                            name = name,
                            playerOneUri = first.documentUri.toString(),
                            playerTwoUri = second?.documentUri?.toString()
                        )
                        quickTeams = prefs.saveQuickTeam(team)
                        PortalResult.Success(message = "Équipe $name enregistrée")
                    },
                    onDeleteTeam = { id -> quickTeams = prefs.deleteQuickTeam(id) },
                    onLoadTeam = loadTeam@{ team ->
                        val byUri = figures.associateBy { it.documentUri.toString() }
                        val missingMember = CollectionStateLogic.missingQuickTeamMember(team, byUri.keys)
                        if (missingMember != null) {
                            return@loadTeam PortalResult.Error(
                                message = "Le fichier $missingMember de cette équipe est introuvable",
                                diagnosticCode = if (missingMember == "Joueur 1") {
                                    "TEAM_PLAYER_ONE_MISSING"
                                } else {
                                    "TEAM_PLAYER_TWO_MISSING"
                                },
                                recoveryHint = "Rescanne le dossier ou recrée l'équipe."
                            )
                        }
                        val first = byUri[team.playerOneUri]
                            ?: return@loadTeam PortalResult.Error("Le fichier Joueur 1 est introuvable", "TEAM_PLAYER_ONE_MISSING")
                        val second = team.playerTwoUri?.let(byUri::get)
                        when (val modeResult = setPlayerTwoMode(second != null)) {
                            is PortalResult.Error -> return@loadTeam modeResult
                            is PortalResult.Success -> Unit
                        }
                        when (val firstResult = bridge.load(0, first)) {
                            is PortalResult.Error -> return@loadTeam firstResult
                            is PortalResult.Success -> recentUris = prefs.recordRecent(first.documentUri.toString())
                        }
                        if (second != null) {
                            when (val secondResult = bridge.load(1, second)) {
                                is PortalResult.Error -> return@loadTeam secondResult
                                is PortalResult.Success -> recentUris = prefs.recordRecent(second.documentUri.toString())
                            }
                        }
                        PortalResult.Success(message = "Équipe ${team.name} chargée")
                    },
                    onRunDiagnostics = {
                        diagnosticAssistant.run(
                            rootUri = rootUri,
                            figures = figures,
                            portalState = portalState,
                            preferredDolphinPackage = prefs.getDolphinPackage()
                        )
                    },
                    onPlayerTwoEnabledChange = { enabled -> setPlayerTwoMode(enabled) },
                    onBackup = backup@{ logicalSlot, figure ->
                        val root = rootUri
                        if (root == null) {
                            return@backup PortalResult.Error(
                                message = "Sélectionne d'abord le dossier Skylanders",
                                diagnosticCode = "BACKUP_NO_ROOT"
                            )
                        }
                        bridge.backupAfterRemoving(logicalSlot, figure) {
                            backups.backup(root, figure).fold(
                                onSuccess = { path -> PortalResult.Success(message = "Backup créé : $path") },
                                onFailure = { error ->
                                    PortalResult.Error(
                                        message = "${figure.name} a été retiré, mais le backup a échoué",
                                        diagnosticCode = "BACKUP_WRITE_FAILED",
                                        technicalDetails = error.javaClass.simpleName,
                                        recoveryHint = "Vérifie que le dossier autorise l'écriture, puis recharge le personnage."
                                    )
                                }
                            )
                        }
                    },
                    onRemove = { logicalSlot -> bridge.remove(logicalSlot) },
                    onClear = {
                        scope.launch {
                            when (val result = bridge.clear()) {
                                is PortalResult.Success -> uiMessage = UiNotice(result.message ?: "Portail vidé", NoticeKind.SUCCESS)
                                is PortalResult.Error -> uiMessage = UiNotice(
                                    "${result.message} (${result.diagnosticCode})",
                                    NoticeKind.ERROR
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun List<Skylander>.enrich(catalog: Map<FigureKey, com.skyportalthor.app.data.FigureMetadata>): List<Skylander> =
    map { figure ->
        val id = figure.figureId
        val variant = figure.variantId
        val metadata = if (id != null && variant != null) catalog[FigureKey(id, variant)] else null
        if (metadata == null) figure else figure.copy(
            name = metadata.canonicalName,
            element = metadata.element,
            generation = DolphinFigureCatalog.generationName(metadata.generation),
            kind = metadata.kind,
            typeLabel = metadata.typeLabel,
            generationNumber = metadata.generation,
            identifiedByDolphin = true
        )
    }

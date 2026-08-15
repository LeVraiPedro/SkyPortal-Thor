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
import com.skyportalthor.app.data.QuickTeam
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
                        }.getOrDefault(emptyList())
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
                    if (bridge.state.value.connected) bridge.refresh()
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
                        prefs.setDolphinPackage(packageName)
                        scope.launch {
                            val connected = bridge.connect(packageName)
                            uiMessage = if (connected) {
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
                        val first = byUri[team.playerOneUri]
                            ?: return@loadTeam PortalResult.Error(
                                message = "Le fichier Joueur 1 de cette équipe est introuvable",
                                diagnosticCode = "TEAM_PLAYER_ONE_MISSING",
                                recoveryHint = "Rescanne le dossier ou recrée l'équipe."
                            )
                        val second = team.playerTwoUri?.let(byUri::get)
                        if (team.playerTwoUri != null && second == null) {
                            return@loadTeam PortalResult.Error(
                                message = "Le fichier Joueur 2 de cette équipe est introuvable",
                                diagnosticCode = "TEAM_PLAYER_TWO_MISSING",
                                recoveryHint = "Rescanne le dossier ou recrée l'équipe."
                            )
                        }
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
                        val mounted = bridge.state.value.slots.getOrNull(logicalSlot)
                        if (
                            mounted?.sourceUri != figure.documentUri.toString() ||
                            !PortalProtocol.isValidActualSlot(mounted.actualPortalSlot)
                        ) {
                            return@backup PortalResult.Error(
                                message = "Le slot a changé avant le backup",
                                diagnosticCode = "BACKUP_STALE_SLOT",
                                recoveryHint = "Ferme ce panneau et réessaie."
                            )
                        }
                        when (val removeResult = bridge.remove(logicalSlot)) {
                            is PortalResult.Error -> return@backup removeResult
                            is PortalResult.Success -> Unit
                        }
                        backups.backup(root, figure).fold(
                            onSuccess = { path -> PortalResult.Success(message = "Backup créé : $path") },
                            onFailure = { error ->
                                PortalResult.Error(
                                    message = "${figure.name} a été retiré, mais le backup a échoué",
                                    diagnosticCode = "BACKUP_WRITE_FAILED",
                                    technicalDetails = "${error.javaClass.simpleName}: ${error.message ?: "sans détail"}",
                                    recoveryHint = "Vérifie que le dossier autorise l'écriture, puis recharge le personnage."
                                )
                            }
                        )
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

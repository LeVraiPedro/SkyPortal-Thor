package com.skyportalthor.app.dolphin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.DeadObjectException
import android.os.IBinder
import android.util.Log
import com.skyportalthor.app.data.Skylander
import com.skyportalthor.app.data.DolphinFigureCatalog
import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.data.FigureCompatibilityEngine
import com.skyportalthor.app.data.FigureKey
import com.skyportalthor.app.data.NativeIdentityPolicy
import com.skyportalthor.app.data.SkyDumpMetadataParser
import com.skyportalthor.app.data.SkyDumpMetadataResult
import com.skyportalthor.app.data.SkyDumpStatus
import com.skyportalthor.app.data.SkylandersGame
import com.skyportalthor.app.data.SmartPortalReadiness
import com.skyportalthor.app.portal.PortalBridge
import com.skyportalthor.app.portal.PortalResult
import com.skyportalthor.app.portal.PortalProtocol
import com.skyportalthor.app.portal.PortalMountPolicy
import com.skyportalthor.app.portal.PortalSlotState
import com.skyportalthor.app.portal.PortalState
import com.skyportalthor.app.portal.PortalStateReducer
import com.skyportalthor.app.portal.NativePortalSlotState
import com.skyportalthor.ipc.ISkylanderPortalService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

class DolphinPortalBridge(private val context: Context) : PortalBridge {
    private val _state = MutableStateFlow(PortalState())
    override val state: StateFlow<PortalState> = _state.asStateFlow()

    private val connectMutex = Mutex()
    private val operationMutex = Mutex()
    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serviceLock = Any()
    @Volatile private var service: ISkylanderPortalService? = null
    @Volatile private var activeComponent: ComponentName? = null
    private var serviceGeneration = 0L
    private var deathLink: DeathLink? = null
    private var bound = false
    private var connection: ServiceConnection? = null
    private val grantLock = Any()
    private val grantedUris = mutableMapOf<GrantSlotKey, String>()

    override suspend fun connect(preferredPackage: String?): Boolean = withContext(Dispatchers.Main.immediate) {
        connectMutex.withLock {
            operationMutex.withLock connectionOperation@{
                val available = DolphinTargets.components.filter(::isServiceAvailable)
                _state.update { it.copy(availablePackages = available.map(ComponentName::getPackageName)) }

                val target = preferredPackage
                    ?.let { preferred -> available.firstOrNull { it.packageName == preferred } }
                    ?: activeComponent?.takeIf { it in available }
                    ?: available.firstOrNull()

                if (target == null) {
                    disconnectCurrentBinding()
                    _state.value = disconnectedState(
                        message = "Dolphin SkyPortal Edition introuvable",
                        readiness = SmartPortalReadiness.DOLPHIN_ABSENT,
                        connectedPackage = null
                    )
                    return@connectionOperation false
                }

                val existing = currentServiceToken()
                val alreadyAlive = activeComponent == target && existing != null &&
                    binderCall(HANDSHAKE_TIMEOUT_MS) { existing.service.ping() }
                        .getOrDefault(false) && isCurrentService(existing)
                if (alreadyAlive) {
                    refreshLocked()
                    return@connectionOperation true
                }

                if (activeComponent != null && activeComponent != target && existing != null) {
                    val cleared = binderCall(OPERATION_TIMEOUT_MS) {
                        existing.service.clear()
                        true
                    }
                    if (cleared.isFailure && cleared.exceptionOrNull() !is DeadObjectException) {
                        val error = cleared.exceptionOrNull()!!
                        logFailure("target-switch-clear", error)
                        _state.update {
                            it.copy(
                                message = "Bascule refusée : l’ancien portail n’a pas pu être vidé"
                            )
                        }
                        return@connectionOperation false
                    }
                    if (cleared.isSuccess) {
                        revokeAllGrants(existing.packageName)
                        updateStateIfCurrent(existing) {
                            it.copy(slots = emptyLogicalSlots(), message = "Ancien portail vidé avant la bascule")
                        }
                    }
                }

                disconnectCurrentBinding()
                activeComponent = target
                _state.update {
                    it.copy(
                        connected = false,
                        readiness = SmartPortalReadiness.CONNECTING,
                        connectedPackage = target.packageName,
                        message = "Connexion à ${DolphinTargets.label(target.packageName)}…"
                    )
                }

                val connected = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
                    suspendCancellableCoroutine { continuation ->
                        var bindRequested = false
                        val conn = object : ServiceConnection {
                            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                                if (connection !== this || activeComponent != target || binder == null) {
                                    if (continuation.isActive) continuation.resume(false)
                                    return
                                }
                                bound = true
                                val token = installService(binder, target) ?: run {
                                    if (continuation.isActive) continuation.resume(false)
                                    return
                                }
                                bridgeScope.launch {
                                    val handshake = binderCall(HANDSHAKE_TIMEOUT_MS) {
                                        token.service.ping() to token.service.apiVersion
                                    }
                                    val values = handshake.getOrNull()
                                    val ok = values?.first == true && isCurrentService(token)
                                    if (ok) {
                                        updateStateIfCurrent(token) {
                                            it.copy(
                                                connected = true,
                                                readiness = SmartPortalReadiness.DOLPHIN_DETECTED,
                                                connectedPackage = target.packageName,
                                                apiVersion = values.second,
                                                message = "${DolphinTargets.label(target.packageName)} connecté"
                                            )
                                        }
                                    } else {
                                        handshake.exceptionOrNull()?.let { logFailure("bind-handshake", it) }
                                        markDisconnectedIfCurrent(token, "Le service Dolphin ne répond pas")
                                    }
                                    if (continuation.isActive) continuation.resume(ok)
                                    if (ok) refresh()
                                }
                            }

                            override fun onServiceDisconnected(name: ComponentName?) {
                                if (connection !== this) return
                                markDisconnected("Connexion Dolphin perdue")
                                if (continuation.isActive) continuation.resume(false)
                            }

                            override fun onBindingDied(name: ComponentName?) {
                                if (connection !== this) return
                                markDisconnected("Le service Dolphin a été arrêté")
                                if (continuation.isActive) continuation.resume(false)
                            }

                            override fun onNullBinding(name: ComponentName?) {
                                if (connection !== this) return
                                markDisconnected("Dolphin a renvoyé un service vide")
                                if (continuation.isActive) continuation.resume(false)
                            }
                        }
                        connection = conn

                        val bindAttempt = runCatching {
                            context.bindService(Intent().setComponent(target), conn, Context.BIND_AUTO_CREATE)
                        }
                        bindRequested = bindAttempt.getOrDefault(false)
                        if (bindRequested) bound = true
                        if (!bindRequested) {
                            val failure = bindAttempt.exceptionOrNull()
                            failure?.let { logFailure("bind", it) }
                            _state.update {
                                it.copy(
                                    connected = false,
                                    message = if (failure is SecurityException) {
                                        "Connexion refusée : signatures APK différentes"
                                    } else {
                                        "Impossible de se connecter à ${DolphinTargets.label(target.packageName)}"
                                    }
                                )
                            }
                            if (continuation.isActive) continuation.resume(false)
                        }

                        continuation.invokeOnCancellation {
                            if (bindRequested) runCatching { context.unbindService(conn) }
                        }
                    }
                }

                if (connected == null) {
                    markDisconnected("Délai de connexion Dolphin dépassé")
                    disconnectCurrentBinding()
                    false
                } else {
                    if (connected) refreshLocked() else disconnectCurrentBinding()
                    connected
                }
            }
        }
    }

    override suspend fun refresh() {
        operationMutex.withLock { refreshLocked() }
    }

    private suspend fun refreshLocked(): Boolean = withContext(Dispatchers.IO) {
        val token = currentServiceToken() ?: return@withContext false
        val json = binderCall(STATUS_TIMEOUT_MS) { token.service.statusJson }.getOrElse { error ->
            logFailure("refresh", error)
            if (error is DeadObjectException) {
                markDisconnectedIfCurrent(token, "Connexion Dolphin perdue")
            } else {
                updateStateIfCurrent(token) { it.copy(message = "Dolphin connecté, statut illisible") }
            }
            return@withContext false
        }
        val snapshot = runCatching {
            DolphinStatusParser.parse(json, _state.value.apiVersion ?: 1)
        }.getOrElse { error ->
            logFailure("status-json", error)
            updateStateIfCurrent(token) { it.copy(message = "Dolphin connecté, réponse de statut invalide") }
            return@withContext false
        }
        if (!isCurrentService(token)) return@withContext false

        val previousCatalog = _state.value.figureCatalog
        val catalog = if (snapshot.apiVersion >= 3 && previousCatalog.isEmpty()) {
            loadCatalog(token)
        } else previousCatalog
        if (!isCurrentService(token)) return@withContext false

        val detectedGame = SkylandersGame.detect(snapshot.gameId, snapshot.gameTitle)
        var committedSlots: List<PortalSlotState> = emptyList()
        val committed = updateStateIfCurrent(token) { current ->
            val currentFigures = current.slots.associateBy(PortalSlotState::logicalSlot)
            val reported = snapshot.logicalSlots.associateBy(ReportedLogicalSlot::logicalSlot)
            val nativeBySlot = snapshot.nativeSlots.associateBy(NativePortalSlotState::slot)
            val newSlots = List(LOGICAL_SLOT_COUNT) { logical ->
                val old = currentFigures[logical]
                val remote = reported[logical]
                val actual = remote?.actualSlot ?: -1
                val native = nativeBySlot[actual]
                val nativeConfirmsSlot = snapshot.apiVersion < 3 ||
                    snapshot.nativeSlots.isEmpty() || native?.occupied == true
                if (!PortalProtocol.isValidActualSlot(actual) || !nativeConfirmsSlot) {
                    PortalSlotState(logicalSlot = logical)
                } else {
                    val sourceUri = if (remote?.uriWasReported == true) {
                        remote.sourceUri
                    } else {
                        old?.sourceUri?.takeIf { old.actualPortalSlot == actual }
                    }
                    val oldFigure = old?.figure?.takeIf {
                        sourceUri != null && it.documentUri.toString() == sourceUri
                    }
                    val nativeIdentityMatches = oldFigure == null || native == null ||
                        (oldFigure.figureId == native.figureId && oldFigure.variantId == native.variantId)
                    val trustedSourceUri = sourceUri.takeIf { nativeIdentityMatches }
                    val trustedFigure = oldFigure.takeIf { nativeIdentityMatches }
                    val nativeLabel = native?.figureId?.let { id ->
                        native.variantId?.let { variant -> catalog[FigureKey(id, variant)]?.canonicalName }
                    }
                    PortalSlotState(
                        logicalSlot = logical,
                        actualPortalSlot = actual,
                        figure = trustedFigure,
                        label = nativeLabel ?: remote?.label ?: trustedFigure?.name
                            ?: old?.label?.takeIf { old.actualPortalSlot == actual && nativeIdentityMatches },
                        sourceUri = trustedSourceUri
                    )
                }
            }
            committedSlots = newSlots
            current.copy(
                connected = true,
                connectedPackage = token.packageName,
                apiVersion = snapshot.apiVersion,
                message = when {
                    snapshot.serviceState == DolphinServiceState.INITIALIZING ->
                        "Dolphin termine son initialisation…"
                    snapshot.issues.isNotEmpty() ->
                        "${DolphinTargets.label(token.packageName)} connecté, diagnostic : ${snapshot.issues.first()}"
                    else -> "${DolphinTargets.label(token.packageName)} connecté"
                },
                slots = newSlots,
                serviceState = snapshot.serviceState,
                emulationState = snapshot.emulationState,
                gameId = snapshot.gameId,
                gameTitle = snapshot.gameTitle,
                skylandersGame = detectedGame,
                portalEnabled = snapshot.portalEnabled,
                portalActivated = snapshot.portalActivated,
                canSetPortalEnabled = snapshot.canSetPortalEnabled,
                nativeSlots = snapshot.nativeSlots,
                figureCatalog = catalog,
                readiness = readiness(
                    snapshot.serviceState,
                    snapshot.emulationState,
                    detectedGame,
                    snapshot.portalEnabled,
                    snapshot.portalActivated
                )
            )
        }
        if (!committed) return@withContext false

        committedSlots.forEach { slot ->
            val remote = snapshot.logicalSlots.firstOrNull { it.logicalSlot == slot.logicalSlot }
            val uri = slot.sourceUri
            if (slot.actualPortalSlot < 0) releaseSlotGrant(token.packageName, slot.logicalSlot)
            else if (uri != null) rememberSlotGrant(token.packageName, slot.logicalSlot, uri)
            else if (remote?.uriWasReported == true) releaseSlotGrant(token.packageName, slot.logicalSlot)
        }
        true
    }

    override suspend fun load(logicalSlot: Int, skylander: Skylander): PortalResult =
        withContext(Dispatchers.IO) {
            operationMutex.withLock {
                if (logicalSlot !in 0 until LOGICAL_SLOT_COUNT) {
                    return@withLock PortalResult.Error(
                        message = "Le slot demandé est invalide",
                        diagnosticCode = "INVALID_SLOT",
                        technicalDetails = "Slot logique reçu : $logicalSlot"
                    )
                }

                if (!refreshLocked()) {
                    return@withLock PortalResult.Error(
                        message = "Impossible d’actualiser l’état du portail avant le chargement",
                        diagnosticCode = "STATUS_REFRESH_REQUIRED",
                        recoveryHint = "Attends la reconnexion de Dolphin puis réessaie."
                    )
                }
                val token = currentServiceToken() ?: return@withLock notConnectedError()

                if (_state.value.skylandersGame != null && _state.value.portalEnabled == false) {
                    return@withLock PortalResult.Error(
                        message = "Le Portal of Power est désactivé dans Dolphin",
                        diagnosticCode = "PORTAL_DISABLED",
                        recoveryHint = if (_state.value.canSetPortalEnabled) {
                            "Utilise Activer le portail dans l’en-tête puis réessaie."
                        } else {
                            "Active Emulated USB Devices > Skylanders Portal dans Dolphin."
                        }
                    )
                }
                if (
                    (_state.value.apiVersion ?: 1) >= 3 &&
                    _state.value.skylandersGame != null &&
                    _state.value.portalEnabled == true &&
                    _state.value.portalActivated == false
                ) {
                    return@withLock PortalResult.Error(
                        message = "Le Portal of Power est encore en cours d’initialisation",
                        diagnosticCode = "PORTAL_NOT_ACTIVATED",
                        recoveryHint = "Attends quelques secondes que le jeu détecte le portail puis réessaie."
                    )
                }

                val preflight = when (val result = preflightSkylander(skylander)) {
                    is SkyPreflight.Invalid -> return@withLock result.error
                    is SkyPreflight.Valid -> result
                }
                val stateBeforeLoad = _state.value
                PortalMountPolicy.unidentifiedMountReason(
                    apiVersion = stateBeforeLoad.apiVersion ?: 1,
                    requestedLogicalSlot = logicalSlot,
                    logicalSlots = stateBeforeLoad.slots,
                    nativeSlots = stateBeforeLoad.nativeSlots
                )?.let { reason ->
                    return@withLock PortalResult.Error(
                        message = "Impossible de garantir que ce fichier n’est pas déjà monté",
                        diagnosticCode = "UNIDENTIFIED_MOUNT",
                        technicalDetails = reason,
                        recoveryHint = "Retire ou vide d’abord les slots non identifiés dans Dolphin, puis actualise SkyPortal."
                    )
                }
                val strictIdentity = (stateBeforeLoad.apiVersion ?: 1) >= 3 &&
                    stateBeforeLoad.figureCatalog.isNotEmpty()
                val key = FigureKey(preflight.figureId, preflight.variantId)
                val identity = NativeIdentityPolicy.check(
                    preflight.figureId,
                    preflight.variantId,
                    stateBeforeLoad.figureCatalog,
                    strictIdentity
                )
                val metadata = identity.metadata ?: stateBeforeLoad.figureCatalog[key]
                if (!identity.recognized) {
                    return@withLock PortalResult.Error(
                        message = identity.reason ?: "Cette identité n’est pas reconnue par Dolphin",
                        diagnosticCode = identity.diagnosticCode ?: "UNKNOWN_FIGURE_IDENTITY",
                        technicalDetails = "ID=${preflight.figureId}; variant=${preflight.variantId}; API=${stateBeforeLoad.apiVersion}",
                        recoveryHint = "Utilise une figurine créée ou reconnue par le gestionnaire Skylanders de cette version de Dolphin."
                    )
                }
                val verifiedFigure = if (metadata == null) {
                    skylander.copy(
                        figureId = preflight.figureId,
                        variantId = preflight.variantId,
                        dumpStatus = SkyDumpStatus.VALID,
                        dumpProblem = null
                    )
                } else {
                    skylander.copy(
                        name = metadata.canonicalName,
                        element = metadata.element,
                        generation = DolphinFigureCatalog.generationName(metadata.generation),
                        kind = metadata.kind,
                        typeLabel = metadata.typeLabel,
                        figureId = preflight.figureId,
                        variantId = preflight.variantId,
                        generationNumber = metadata.generation,
                        identifiedByDolphin = true,
                        dumpStatus = SkyDumpStatus.VALID,
                        dumpProblem = null
                    )
                }

                FigureCompatibilityEngine.check(verifiedFigure, stateBeforeLoad.skylandersGame).let { compatibility ->
                    if (!compatibility.compatible) {
                        return@withLock PortalResult.Error(
                            message = compatibility.reason ?: "Cette figurine est incompatible avec le jeu actif",
                            diagnosticCode = "FIGURE_INCOMPATIBLE",
                            technicalDetails = "Jeu=${stateBeforeLoad.skylandersGame?.displayName}; type=${verifiedFigure.typeLabel}; génération=${verifiedFigure.generation}",
                            recoveryHint = "Choisis une figurine compatible ou lance le jeu correspondant."
                        )
                    }
                }
                if (!isCurrentService(token)) return@withLock serviceReplacedError()
                val packageName = token.packageName
                val newUri = skylander.documentUri.toString()
                val duplicate = grantForUri(newUri)
                if (duplicate != null && (duplicate.packageName != packageName || duplicate.logicalSlot != logicalSlot)) {
                    return@withLock PortalResult.Error(
                        message = "Ce fichier est déjà placé sur le portail",
                        diagnosticCode = "DUPLICATE_SKY_FILE",
                        technicalDetails = "${skylander.fileName} est déjà associé à ${DolphinTargets.label(duplicate.packageName)}, slot ${duplicate.logicalSlot + 1}.",
                        recoveryHint = "Retire d'abord le personnage de son autre slot."
                    )
                }
                grantUriToDolphin(packageName, skylander)?.let { return@withLock it }
                if (!isCurrentService(token)) {
                    revokeTemporaryGrant(packageName, newUri)
                    return@withLock serviceReplacedError()
                }

                updateStateIfCurrent(token) { it.copy(message = "Placement de ${verifiedFigure.name}…") }
                val loadAttempt = binderCall(OPERATION_TIMEOUT_MS) {
                    token.service.load(logicalSlot, skylander.documentUri.toString(), verifiedFigure.name)
                }
                val actual = loadAttempt.getOrElse { error ->
                    revokeTemporaryGrant(packageName, newUri)
                    logFailure("load", error)
                    if (error is DeadObjectException) {
                        markDisconnectedIfCurrent(token, "Connexion Dolphin perdue")
                    }
                    return@withLock binderError(error)
                }

                if (!isCurrentService(token)) {
                    revokeTemporaryGrant(packageName, newUri)
                    return@withLock serviceReplacedError()
                }

                if (!PortalProtocol.isValidActualSlot(actual)) {
                    revokeTemporaryGrant(packageName, newUri)
                    val error = mapLoadError(actual, verifiedFigure)
                    updateStateIfCurrent(token) { it.copy(message = "Échec : ${error.message}") }
                    return@withLock error
                }

                rememberSlotGrant(packageName, logicalSlot, newUri)
                val staged = updateStateIfCurrent(token) { current ->
                    val slots = current.slots.toMutableList()
                    slots[logicalSlot] = PortalSlotState(
                        logicalSlot = logicalSlot,
                        actualPortalSlot = actual,
                        figure = verifiedFigure,
                        label = verifiedFigure.name,
                        sourceUri = newUri
                    )
                    current.copy(
                        connected = true,
                        slots = slots,
                        message = "Vérification de ${verifiedFigure.name} sur le portail…"
                    )
                }
                if (!staged) {
                    revokeTemporaryGrant(packageName, newUri)
                    return@withLock serviceReplacedError()
                }

                val refreshed = refreshLocked()
                val confirmedState = _state.value
                val confirmedSlot = confirmedState.slots.getOrNull(logicalSlot)
                val native = confirmedState.nativeSlots.firstOrNull { it.slot == actual }
                val confirmed = PortalProtocol.isConfirmedLoad(
                    apiVersion = confirmedState.apiVersion ?: 1,
                    refreshSucceeded = refreshed,
                    expectedActualSlot = actual,
                    logicalActualSlot = confirmedSlot?.actualPortalSlot,
                    nativeOccupied = native?.occupied,
                    expectedFigureId = preflight.figureId,
                    expectedVariantId = preflight.variantId,
                    nativeFigureId = native?.figureId,
                    nativeVariantId = native?.variantId
                )
                if (!confirmed) {
                    if (isCurrentService(token)) {
                        binderCall(OPERATION_TIMEOUT_MS) { token.service.remove(logicalSlot) }
                        refreshLocked()
                    }
                    releaseSlotGrant(packageName, logicalSlot)
                    return@withLock PortalResult.Error(
                        message = "Dolphin n’a pas confirmé le chargement sur le portail natif",
                        diagnosticCode = "LOAD_NOT_CONFIRMED",
                        technicalDetails = "Slot logique=${logicalSlot + 1}; slot natif=$actual",
                        recoveryHint = "Attends la reconnexion, vérifie l’état du portail puis réessaie."
                    )
                }
                updateStateIfCurrent(token) { it.copy(message = "${verifiedFigure.name} est sur le portail") }
                PortalResult.Success(actual, "${verifiedFigure.name} chargé avec succès")
            }
        }

    override suspend fun remove(logicalSlot: Int): PortalResult = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            removeLocked(logicalSlot)
        }
    }

    /**
     * Keeps remove + backup under the same operation lock. No load, target switch or reconnect can
     * remount the source through this bridge while the read-only copy is in progress.
     */
    suspend fun backupAfterRemoving(
        logicalSlot: Int,
        skylander: Skylander,
        createBackup: suspend () -> PortalResult
    ): PortalResult = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            if (!refreshLocked()) {
                return@withLock PortalResult.Error(
                    message = "Impossible de vérifier le portail avant le backup",
                    diagnosticCode = "BACKUP_STATUS_REFRESH_FAILED",
                    recoveryHint = "Attends la reconnexion de Dolphin puis réessaie."
                )
            }
            val stateBeforeBackup = _state.value
            if ((stateBeforeBackup.apiVersion ?: 1) < 3) {
                return@withLock PortalResult.Error(
                    message = "Le backup sécurisé nécessite l’API Dolphin 3",
                    diagnosticCode = "BACKUP_REQUIRES_API3",
                    recoveryHint = "Installe le patch Dolphin API 3 : les API 1/2 ne peuvent pas révéler les montages du Manager."
                )
            }
            PortalMountPolicy.unidentifiedMountReason(
                apiVersion = stateBeforeBackup.apiVersion ?: 1,
                requestedLogicalSlot = logicalSlot,
                logicalSlots = stateBeforeBackup.slots,
                nativeSlots = stateBeforeBackup.nativeSlots
            )?.let { reason ->
                return@withLock PortalResult.Error(
                    message = "Le backup est bloqué car un montage Dolphin n’est pas identifié",
                    diagnosticCode = "BACKUP_UNIDENTIFIED_MOUNT",
                    technicalDetails = reason,
                    recoveryHint = "Retire le contenu ouvert dans le Manager Dolphin, actualise SkyPortal puis réessaie."
                )
            }
            val mounted = stateBeforeBackup.slots.getOrNull(logicalSlot)
            if (
                mounted?.sourceUri != skylander.documentUri.toString() ||
                !PortalProtocol.isValidActualSlot(mounted.actualPortalSlot)
            ) {
                return@withLock PortalResult.Error(
                    message = "Le slot a changé avant le backup",
                    diagnosticCode = "BACKUP_STALE_SLOT",
                    recoveryHint = "Ferme ce panneau et réessaie."
                )
            }
            when (val removal = removeLocked(logicalSlot)) {
                is PortalResult.Error -> removal
                is PortalResult.Success -> createBackup()
            }
        }
    }

    private suspend fun removeLocked(logicalSlot: Int): PortalResult {
        if (logicalSlot !in 0 until LOGICAL_SLOT_COUNT) {
            return PortalResult.Error("Slot invalide", "INVALID_SLOT")
        }
        val token = currentServiceToken() ?: return notConnectedError()
        val attempt = binderCall(OPERATION_TIMEOUT_MS) { token.service.remove(logicalSlot) }
        val ok = attempt.getOrElse { error ->
            logFailure("remove", error)
            if (error is DeadObjectException) {
                markDisconnectedIfCurrent(token, "Connexion Dolphin perdue")
            }
            return binderError(error)
        }
        if (!isCurrentService(token)) return serviceReplacedError()
        if (!ok) {
            return PortalResult.Error(
                message = "Dolphin n'a pas pu retirer ce slot",
                diagnosticCode = "REMOVE_REJECTED",
                recoveryHint = "Vérifie que le jeu et le portail émulé sont toujours actifs."
            )
        }
        releaseSlotGrant(token.packageName, logicalSlot)
        updateStateIfCurrent(token) { current ->
            val slots = current.slots.toMutableList()
            slots[logicalSlot] = PortalSlotState(logicalSlot)
            current.copy(slots = slots, message = "Slot ${logicalSlot + 1} retiré")
        }
        return PortalResult.Success(message = "Personnage retiré du slot ${logicalSlot + 1}")
    }

    override suspend fun clear(): PortalResult = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            val token = currentServiceToken() ?: return@withLock notConnectedError()
            val attempt = binderCall(OPERATION_TIMEOUT_MS) { token.service.clear() }
            attempt.exceptionOrNull()?.let { error ->
                logFailure("clear", error)
                if (error is DeadObjectException) {
                    markDisconnectedIfCurrent(token, "Connexion Dolphin perdue")
                }
                return@withLock binderError(error)
            }
            if (!isCurrentService(token)) return@withLock serviceReplacedError()
            revokeAllGrants(token.packageName)
            updateStateIfCurrent(token) {
                it.copy(
                    slots = List(LOGICAL_SLOT_COUNT) { logical -> PortalSlotState(logical) },
                    message = "Portail vidé"
                )
            }
            PortalResult.Success(message = "Tous les slots ont été retirés")
        }
    }

    override suspend fun setPortalEnabled(enabled: Boolean): PortalResult = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            val token = currentServiceToken() ?: return@withLock notConnectedError()
            if ((_state.value.apiVersion ?: 1) < 3 || !_state.value.canSetPortalEnabled) {
                return@withLock PortalResult.Error(
                    "Cette version de Dolphin ne permet pas d’activer le portail depuis SkyPortal",
                    "PORTAL_TOGGLE_UNSUPPORTED",
                    recoveryHint = "Installe le patch Dolphin API 3 ou active le portail dans les réglages Dolphin."
                )
            }
            updateStateIfCurrent(token) { it.copy(readiness = SmartPortalReadiness.ENABLING_PORTAL, message = "Activation du portail…") }
            val code = binderCall(OPERATION_TIMEOUT_MS) { token.service.setPortalEnabled(enabled) }.getOrElse { error ->
                if (error is DeadObjectException) markDisconnectedIfCurrent(token, "Connexion Dolphin perdue")
                return@withLock binderError(error)
            }
            if (!isCurrentService(token)) return@withLock serviceReplacedError()
            if (code != 0) {
                updateStateIfCurrent(token) { it.copy(readiness = SmartPortalReadiness.ERROR, message = "Activation du portail refusée") }
                if (code == PortalProtocol.ERROR_DOLPHIN_NOT_READY) {
                    return@withLock PortalResult.Error(
                        "Dolphin termine son initialisation",
                        "PORTAL_TOGGLE_DOLPHIN_NOT_READY",
                        recoveryHint = "Patiente quelques secondes : SkyPortal va se reconnecter automatiquement."
                    )
                }
                return@withLock PortalResult.Error("Dolphin n’a pas pu modifier l’état du portail", "PORTAL_TOGGLE_$code")
            }
            refreshLocked()
            PortalResult.Success(message = if (enabled) "Portal of Power activé" else "Portal of Power désactivé")
        }
    }

    override fun close() {
        disconnectCurrentBinding()
        bridgeScope.cancel()
        _state.value = disconnectedState(
            message = "Dolphin non connecté",
            readiness = SmartPortalReadiness.DOLPHIN_ABSENT,
            connectedPackage = null
        )
    }

    private fun preflightSkylander(skylander: Skylander): SkyPreflight {
        if (skylander.isMasterTemplate) {
            return SkyPreflight.Invalid(
                PortalResult.Error(
                    message = "Le fichier ${skylander.fileName} est un MASTER vierge protégé",
                    diagnosticCode = "MASTER_TEMPLATE_PROTECTED",
                    recoveryHint = "Crée une nouvelle figurine avec le gestionnaire Dolphin et enregistre-la sous un autre nom."
                )
            )
        }
        val attempt = runCatching {
            context.contentResolver.openFileDescriptor(skylander.documentUri, "rw")?.use { descriptor ->
                check(descriptor.fileDescriptor.valid()) { "Descripteur de fichier invalide" }
                check(descriptor.statSize != 0L) { "Le fichier est vide" }
                check(descriptor.statSize == -1L || descriptor.statSize == SkyDumpMetadataParser.DUMP_SIZE_BYTES.toLong()) {
                    "Taille .sky invalide : ${descriptor.statSize} octets"
                }
            } ?: error("Le fournisseur de documents n'a renvoyé aucun fichier")
            context.contentResolver.openInputStream(skylander.documentUri)?.use(SkyDumpMetadataParser::read)
                ?: error("Le fournisseur de documents n’a pas ouvert le fichier en lecture")
        }
        val result = attempt.getOrElse { failure ->
            logFailure("uri-preflight", failure)
            return SkyPreflight.Invalid(
                PortalResult.Error(
                    message = "Le fichier ${skylander.fileName} n'est pas accessible en lecture/écriture",
                    diagnosticCode = if (failure is SecurityException) "SAF_PERMISSION_DENIED" else "SKY_FILE_UNREADABLE",
                    technicalDetails = failure.safeSummary(),
                    recoveryHint = "Touche Dossier, sélectionne à nouveau le dossier Skylanders et accorde l'accès complet."
                )
            )
        }
        if (result is SkyDumpMetadataResult.Invalid) {
            return SkyPreflight.Invalid(
                PortalResult.Error(
                    message = "Le fichier ${skylander.fileName} n’est pas un dump .sky valide",
                    diagnosticCode = when (result.status) {
                        SkyDumpStatus.INVALID_SIZE -> "SKY_DUMP_INVALID_SIZE"
                        SkyDumpStatus.INVALID_HEADER -> "SKY_DUMP_INVALID_HEADER"
                        SkyDumpStatus.INVALID_CHECKSUM -> "SKY_DUMP_INVALID_CHECKSUM"
                        else -> "SKY_DUMP_UNREADABLE"
                    },
                    technicalDetails = result.reason,
                    recoveryHint = "Recrée ce fichier avec le gestionnaire Skylanders de Dolphin sans modifier le MASTER d’origine."
                )
            )
        }
        result as SkyDumpMetadataResult.Valid
        if (
            (skylander.figureId != null && skylander.figureId != result.figureId) ||
            (skylander.variantId != null && skylander.variantId != result.variantId)
        ) {
            return SkyPreflight.Invalid(
                PortalResult.Error(
                    message = "Le fichier ${skylander.fileName} a changé depuis le dernier scan",
                    diagnosticCode = "SKY_FILE_CHANGED_AFTER_SCAN",
                    technicalDetails = "Identité scannée=${skylander.figureId}/${skylander.variantId}; identité actuelle=${result.figureId}/${result.variantId}",
                    recoveryHint = "Rescanne le dossier avant de réessayer."
                )
            )
        }
        return SkyPreflight.Valid(result.figureId, result.variantId)
    }

    private fun grantUriToDolphin(packageName: String, skylander: Skylander): PortalResult.Error? {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val attempt = runCatching {
            context.grantUriPermission(packageName, skylander.documentUri, flags)
        }
        val failure = attempt.exceptionOrNull() ?: return null
        logFailure("uri-grant", failure)
        return PortalResult.Error(
            message = "Android a refusé de partager le fichier avec ${DolphinTargets.label(packageName)}",
            diagnosticCode = "URI_GRANT_FAILED",
            technicalDetails = failure.safeSummary(),
            recoveryHint = "Sélectionne à nouveau le dossier ou réinstalle les deux APK avec la même signature."
        )
    }

    private fun revokeUriGrant(packageName: String, uriString: String) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.revokeUriPermission(packageName, Uri.parse(uriString), flags)
        }.onFailure { logFailure("uri-revoke", it) }
    }

    private fun rememberSlotGrant(packageName: String, logicalSlot: Int, uriString: String) {
        val previous = synchronized(grantLock) {
            grantedUris.put(GrantSlotKey(packageName, logicalSlot), uriString)
        }
        if (previous != null && previous != uriString) revokeTemporaryGrant(packageName, previous)
    }

    private fun releaseSlotGrant(packageName: String, logicalSlot: Int) {
        val uri = synchronized(grantLock) {
            grantedUris.remove(GrantSlotKey(packageName, logicalSlot))
        } ?: return
        revokeTemporaryGrant(packageName, uri)
    }

    private fun revokeTemporaryGrant(packageName: String, uriString: String) {
        val stillUsed = synchronized(grantLock) {
            grantedUris.any { (key, value) -> key.packageName == packageName && value == uriString }
        }
        if (!stillUsed) revokeUriGrant(packageName, uriString)
    }

    private fun revokeAllGrants(packageName: String) {
        val uris = synchronized(grantLock) {
            val matching = grantedUris.filterKeys { it.packageName == packageName }.values.toSet()
            grantedUris.keys.removeAll { it.packageName == packageName }
            matching
        }
        uris.forEach { revokeUriGrant(packageName, it) }
    }

    private fun grantForUri(uriString: String): GrantSlotKey? = synchronized(grantLock) {
        grantedUris.entries.firstOrNull { (_, value) -> value == uriString }?.key
    }

    private fun mapLoadError(code: Int, skylander: Skylander): PortalResult.Error = when (code) {
        PortalProtocol.NATIVE_NO_SLOT -> PortalResult.Error(
            message = "Le portail Dolphin est plein",
            diagnosticCode = "PORTAL_FULL_255",
            technicalDetails = "Dolphin a renvoyé le slot natif 255 pour ${skylander.fileName}.",
            recoveryHint = "Vide le portail. Si des figurines restent orphelines, ferme puis relance Dolphin."
        )
        -2 -> PortalResult.Error(
            message = "Dolphin n'a pas pu ouvrir ou reconnaître le fichier .sky",
            diagnosticCode = "DOLPHIN_OPEN_FAILED",
            technicalDetails = diagnosticContext(skylander),
            recoveryHint = "Vérifie que le fichier est un dump .sky valide et que le dossier est toujours autorisé."
        )
        -3 -> PortalResult.Error(
            message = "Dolphin a refusé le numéro de slot",
            diagnosticCode = "DOLPHIN_BAD_SLOT",
            technicalDetails = diagnosticContext(skylander)
        )
        -4 -> PortalResult.Error(
            message = "Dolphin n'a pas accès au fichier partagé",
            diagnosticCode = "DOLPHIN_URI_ACCESS_DENIED",
            technicalDetails = diagnosticContext(skylander),
            recoveryHint = "Sélectionne à nouveau le dossier Skylanders dans l'application."
        )
        -5 -> PortalResult.Error(
            message = "Dolphin a rejeté les données du fichier .sky",
            diagnosticCode = "DOLPHIN_SKY_DATA_REJECTED",
            technicalDetails = diagnosticContext(skylander),
            recoveryHint = "Teste ce fichier dans le gestionnaire Skylanders de Dolphin ou recrée uniquement ce dump."
        )
        PortalProtocol.ERROR_PORTAL_FULL -> PortalResult.Error(
            message = "Le portail Dolphin est plein",
            diagnosticCode = "PORTAL_FULL",
            technicalDetails = diagnosticContext(skylander),
            recoveryHint = "Vide le portail. Si des figurines restent orphelines, ferme puis relance Dolphin."
        )
        -8 -> PortalResult.Error(
            message = "Cette identité de figurine n’est pas reconnue par Dolphin",
            diagnosticCode = "DOLPHIN_UNKNOWN_FIGURE",
            technicalDetails = diagnosticContext(skylander),
            recoveryHint = "Crée le fichier avec le gestionnaire Skylanders de cette version de Dolphin."
        )
        -9 -> PortalResult.Error(
            message = "Dolphin a refusé cette figurine pour le jeu actuellement lancé",
            diagnosticCode = "DOLPHIN_FIGURE_INCOMPATIBLE",
            technicalDetails = diagnosticContext(skylander),
            recoveryHint = "Choisis un personnage ou un objet compatible avec le jeu détecté."
        )
        PortalProtocol.ERROR_DOLPHIN_NOT_READY -> PortalResult.Error(
            message = "Dolphin termine son initialisation",
            diagnosticCode = "DOLPHIN_NATIVE_NOT_READY",
            technicalDetails = diagnosticContext(skylander),
            recoveryHint = "Patiente quelques secondes, puis réessaie quand le jeu et le portail sont détectés."
        )
        PortalProtocol.ERROR_UNIDENTIFIED_NATIVE_MOUNT -> PortalResult.Error(
            message = "Un slot Dolphin non identifié est déjà occupé",
            diagnosticCode = "DOLPHIN_UNIDENTIFIED_NATIVE_MOUNT",
            technicalDetails = diagnosticContext(skylander),
            recoveryHint = "Retire la figurine ouverte directement dans le Manager Dolphin, actualise SkyPortal puis réessaie."
        )
        else -> PortalResult.Error(
            message = "Dolphin a refusé le chargement",
            diagnosticCode = "DOLPHIN_LOAD_$code",
            technicalDetails = "Code natif : $code. ${diagnosticContext(skylander)}",
            recoveryHint = "Vide le portail puis réessaie."
        )
    }

    private fun notConnectedError() = PortalResult.Error(
        message = "Dolphin n'est pas connecté",
        diagnosticCode = "DOLPHIN_NOT_CONNECTED",
        technicalDetails = "Aucun service Binder actif.",
        recoveryHint = "Lance Dolphin SkyPortal Edition puis touche Reconnecter."
    )

    private fun serviceReplacedError() = PortalResult.Error(
        message = "Dolphin s'est reconnecté pendant l'opération",
        diagnosticCode = "BINDER_REPLACED",
        technicalDetails = "Le service Binder actif a changé avant la confirmation du résultat.",
        recoveryHint = "Attends la fin de la reconnexion puis réessaie."
    )

    private fun binderError(error: Throwable) = PortalResult.Error(
        message = when (error) {
            is SecurityException -> "Android a refusé l'appel vers Dolphin"
            is TimeoutCancellationException -> "Dolphin n’a pas répondu dans le délai prévu"
            else -> "La communication avec Dolphin a échoué"
        },
        diagnosticCode = when (error) {
            is SecurityException -> "BINDER_PERMISSION_DENIED"
            is DeadObjectException -> "BINDER_DISCONNECTED"
            is TimeoutCancellationException -> "BINDER_TIMEOUT"
            else -> "BINDER_CALL_FAILED"
        },
        technicalDetails = error.safeSummary(),
        recoveryHint = when (error) {
            is SecurityException -> "Les APK SkyPortal et Dolphin doivent être signés avec la même clé."
            is TimeoutCancellationException ->
                "L’état de l’opération est incertain. Attends la reconnexion et vérifie les slots avant de réessayer."
            else -> "Reconnecte Dolphin puis réessaie."
        }
    )

    private fun diagnosticContext(skylander: Skylander): String = buildString {
        append("Fichier : ${skylander.fileName}")
        append(" • Cible : ${DolphinTargets.label(activeComponent?.packageName)}")
        append(" • API : ${_state.value.apiVersion ?: "inconnue"}")
    }

    private suspend fun loadCatalog(token: ServiceToken) = binderCall(STATUS_TIMEOUT_MS) {
        token.service.figureCatalogJson
    }.mapCatching { json ->
        val array = JSONObject(json).getJSONArray("figures")
        buildMap {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val metadata = DolphinFigureCatalog.decode(
                    id = item.getInt("id"),
                    variant = item.getInt("variant"),
                    name = item.getString("name"),
                    game = item.getInt("game"),
                    element = item.getInt("element"),
                    type = item.getInt("type")
                )
                put(metadata.key, metadata)
            }
        }
    }.onFailure { logFailure("figure-catalog", it) }.getOrDefault(emptyMap())

    private fun readiness(
        serviceState: DolphinServiceState,
        emulation: EmulationState,
        game: SkylandersGame?,
        portalEnabled: Boolean?,
        portalActivated: Boolean?
    ): SmartPortalReadiness = when {
        serviceState == DolphinServiceState.INITIALIZING -> SmartPortalReadiness.CONNECTING
        emulation == EmulationState.NONE || emulation == EmulationState.STOPPING -> SmartPortalReadiness.NO_GAME
        emulation == EmulationState.STARTING -> SmartPortalReadiness.GAME_DETECTED
        game == null -> SmartPortalReadiness.GAME_DETECTED
        portalEnabled == false -> SmartPortalReadiness.PORTAL_DISABLED
        portalEnabled == true && portalActivated == false -> SmartPortalReadiness.GAME_DETECTED
        portalEnabled == true -> SmartPortalReadiness.READY
        else -> SmartPortalReadiness.GAME_DETECTED
    }

    private fun markDisconnected(message: String) {
        val packageName = synchronized(serviceLock) {
            unlinkDeathRecipientLocked()
            service = null
            serviceGeneration++
            val packageName = activeComponent?.packageName
            _state.value = disconnectedState(
                message = message,
                readiness = SmartPortalReadiness.DOLPHIN_DETECTED,
                connectedPackage = packageName
            )
            packageName
        }
        packageName?.let(::revokeAllGrants)
    }

    private fun markDisconnectedIfCurrent(expected: ServiceToken, message: String) {
        val packageName = synchronized(serviceLock) {
            if (!isCurrentServiceLocked(expected)) return
            unlinkDeathRecipientLocked()
            service = null
            serviceGeneration++
            _state.value = disconnectedState(
                message = message,
                readiness = SmartPortalReadiness.DOLPHIN_DETECTED,
                connectedPackage = expected.packageName
            )
            expected.packageName
        }
        revokeAllGrants(packageName)
    }

    private fun currentServiceToken(): ServiceToken? = synchronized(serviceLock) {
        val current = service ?: return@synchronized null
        val packageName = activeComponent?.packageName ?: return@synchronized null
        ServiceToken(current, current.asBinder(), serviceGeneration, packageName)
    }

    private fun isCurrentService(expected: ServiceToken): Boolean = synchronized(serviceLock) {
        isCurrentServiceLocked(expected)
    }

    private fun isCurrentServiceLocked(expected: ServiceToken): Boolean =
        service?.asBinder() == expected.binder && serviceGeneration == expected.generation &&
            activeComponent?.packageName == expected.packageName

    private inline fun updateStateIfCurrent(
        expected: ServiceToken,
        transform: (PortalState) -> PortalState
    ): Boolean = synchronized(serviceLock) {
        if (!isCurrentServiceLocked(expected)) return@synchronized false
        _state.value = transform(_state.value)
        true
    }

    private fun installService(binder: IBinder, target: ComponentName): ServiceToken? {
        val remote = ISkylanderPortalService.Stub.asInterface(binder) ?: return null
        val token: ServiceToken
        val recipient: IBinder.DeathRecipient
        synchronized(serviceLock) {
            unlinkDeathRecipientLocked()
            serviceGeneration++
            token = ServiceToken(remote, binder, serviceGeneration, target.packageName)
            recipient = IBinder.DeathRecipient {
                bridgeScope.launch {
                    markDisconnectedIfCurrent(token, "Le processus Dolphin ne répond plus")
                }
            }
            service = remote
            deathLink = DeathLink(binder, recipient)
        }
        return runCatching {
            binder.linkToDeath(recipient, 0)
            token
        }.getOrElse { error ->
            logFailure("binder-death-link", error)
            markDisconnectedIfCurrent(token, "Impossible de surveiller le processus Dolphin")
            null
        }
    }

    private fun unlinkDeathRecipientLocked() {
        deathLink?.let { link -> runCatching { link.binder.unlinkToDeath(link.recipient, 0) } }
        deathLink = null
    }

    private fun disconnectCurrentBinding() {
        val packageName = activeComponent?.packageName
        packageName?.let(::revokeAllGrants)
        val conn = connection
        if (conn != null && bound) runCatching { context.unbindService(conn) }
        bound = false
        synchronized(serviceLock) {
            unlinkDeathRecipientLocked()
            service = null
            serviceGeneration++
        }
        connection = null
        activeComponent = null
    }

    private suspend fun <T> binderCall(timeoutMs: Long, block: () -> T): Result<T> = runCatching {
        withTimeout(timeoutMs) {
            runInterruptible(Dispatchers.IO) { block() }
        }
    }

    private fun disconnectedState(
        message: String,
        readiness: SmartPortalReadiness,
        connectedPackage: String?
    ): PortalState = PortalStateReducer.disconnected(
        previous = _state.value,
        message = message,
        readiness = readiness,
        connectedPackage = connectedPackage
    )

    private fun emptyLogicalSlots(): List<PortalSlotState> =
        List(LOGICAL_SLOT_COUNT) { logical -> PortalSlotState(logical) }

    private fun isServiceAvailable(component: ComponentName): Boolean = try {
        context.packageManager.getServiceInfo(component, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private fun logFailure(stage: String, error: Throwable) {
        Log.e(TAG, "$stage failed (${error.javaClass.simpleName})")
    }

    private fun Throwable.safeSummary(): String {
        val safeMessage = message.orEmpty()
            .replace(Regex("content://[^\\s]+"), "content://…")
            .replace(Regex("file:/+[^\\s]+"), "file://…")
            .take(240)
        return "${javaClass.simpleName}: ${safeMessage.ifBlank { "sans détail" }}"
    }

    private data class GrantSlotKey(val packageName: String, val logicalSlot: Int)
    private data class ServiceToken(
        val service: ISkylanderPortalService,
        val binder: IBinder,
        val generation: Long,
        val packageName: String
    )
    private data class DeathLink(val binder: IBinder, val recipient: IBinder.DeathRecipient)
    private sealed interface SkyPreflight {
        data class Valid(val figureId: Int, val variantId: Int) : SkyPreflight
        data class Invalid(val error: PortalResult.Error) : SkyPreflight
    }

    companion object {
        private const val TAG = "SkyPortalBridge"
        private const val CONNECT_TIMEOUT_MS = 5_000L
        private const val HANDSHAKE_TIMEOUT_MS = 5_000L
        private const val STATUS_TIMEOUT_MS = 8_000L
        private const val OPERATION_TIMEOUT_MS = 35_000L
        private const val LOGICAL_SLOT_COUNT = 8
    }
}

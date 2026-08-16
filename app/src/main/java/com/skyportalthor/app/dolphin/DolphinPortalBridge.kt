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
import com.skyportalthor.app.data.FigureCompatibilityEngine
import com.skyportalthor.app.data.FigureKey
import com.skyportalthor.app.data.SkylandersGame
import com.skyportalthor.app.data.SmartPortalReadiness
import com.skyportalthor.app.portal.PortalBridge
import com.skyportalthor.app.portal.PortalResult
import com.skyportalthor.app.portal.PortalProtocol
import com.skyportalthor.app.portal.PortalSlotState
import com.skyportalthor.app.portal.PortalState
import com.skyportalthor.app.portal.NativePortalSlotState
import com.skyportalthor.ipc.ISkylanderPortalService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

class DolphinPortalBridge(private val context: Context) : PortalBridge {
    private val _state = MutableStateFlow(PortalState())
    override val state: StateFlow<PortalState> = _state.asStateFlow()

    private val connectMutex = Mutex()
    private val operationMutex = Mutex()
    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var service: ISkylanderPortalService? = null
    private var activeComponent: ComponentName? = null
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
                _state.update {
                    it.copy(
                        connected = false,
                    connectedPackage = null,
                    readiness = SmartPortalReadiness.DOLPHIN_ABSENT,
                        message = "Dolphin SkyPortal Edition introuvable"
                    )
                }
                return@connectionOperation false
            }

            val alreadyAlive = activeComponent == target && runCatching { service?.ping() == true }
                .getOrDefault(false)
            if (alreadyAlive) {
                refreshLocked()
                return@connectionOperation true
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
                            if (connection !== this || activeComponent != target) return
                            service = ISkylanderPortalService.Stub.asInterface(binder)
                            bound = true
                            val ping = runCatching { service?.ping() == true }
                            val ok = ping.getOrDefault(false)
                            _state.update {
                                it.copy(
                                    connected = ok,
                                    readiness = if (ok) SmartPortalReadiness.DOLPHIN_DETECTED else SmartPortalReadiness.ERROR,
                                    connectedPackage = target.packageName,
                                    apiVersion = runCatching { service?.apiVersion }.getOrNull(),
                                    message = if (ok) {
                                        "${DolphinTargets.label(target.packageName)} connecté"
                                    } else {
                                        "Le service Dolphin ne répond pas"
                                    }
                                )
                            }
                            if (!ok) ping.exceptionOrNull()?.let { logFailure("bind-ping", it) }
                            if (ok) bridgeScope.launch { refresh() }
                            if (continuation.isActive) continuation.resume(ok)
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
                        context.bindService(
                            Intent().setComponent(target),
                            conn,
                            Context.BIND_AUTO_CREATE
                        )
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

    private suspend fun refreshLocked() = withContext(Dispatchers.IO) {
            val currentService = service ?: return@withContext
            val targetPackage = activeComponent?.packageName ?: return@withContext
            val status = runCatching { currentService.statusJson }
            val json = status.getOrElse { error ->
                logFailure("refresh", error)
                if (error is DeadObjectException) {
                    markDisconnectedIfCurrent(currentService, "Connexion Dolphin perdue")
                }
                else if (isCurrentService(currentService)) {
                    _state.update { it.copy(message = "Dolphin connecté, statut illisible") }
                }
                return@withContext
            }
            val root = runCatching { JSONObject(json) }.getOrElse { error ->
                logFailure("status-json", error)
                if (isCurrentService(currentService)) {
                    _state.update { it.copy(message = "Dolphin connecté, réponse de statut invalide") }
                }
                return@withContext
            }
            if (service?.asBinder() != currentService.asBinder() || activeComponent?.packageName != targetPackage) {
                return@withContext
            }

            val slotsArray = root.optJSONArray("slots")
            val apiVersion = root.optInt("apiVersion", _state.value.apiVersion ?: 1)
            val current = _state.value
            val currentFigures = current.slots.associateBy { it.logicalSlot }
            val statusUris = arrayOfNulls<String>(LOGICAL_SLOT_COUNT)
            val statusHasUri = BooleanArray(LOGICAL_SLOT_COUNT)
            var invalidActualSlot: Int? = null
            val newSlots = List(LOGICAL_SLOT_COUNT) { logical ->
                val old = currentFigures[logical]
                val obj = (0 until (slotsArray?.length() ?: 0))
                    .asSequence()
                    .mapNotNull { slotsArray?.optJSONObject(it) }
                    .firstOrNull { it.optInt("logicalSlot", -1) == logical }
                val actual = obj?.optInt("actualSlot", -1) ?: -1
                if (!PortalProtocol.isValidActualSlot(actual)) {
                    if (actual >= 0) invalidActualSlot = actual
                    PortalSlotState(logicalSlot = logical)
                } else {
                    val uriWasReported = obj?.has("uri") == true
                    statusHasUri[logical] = uriWasReported
                    val reportedUri = obj?.optString("uri")?.takeIf { it.isNotBlank() }
                    val sourceUri = if (uriWasReported) {
                        reportedUri
                    } else {
                        old?.sourceUri?.takeIf { old.actualPortalSlot == actual }
                    }
                    statusUris[logical] = sourceUri
                    PortalSlotState(
                        logicalSlot = logical,
                        actualPortalSlot = actual,
                        figure = old?.figure?.takeIf {
                            sourceUri != null && it.documentUri.toString() == sourceUri
                        },
                        label = obj?.optString("label")?.takeIf { it.isNotBlank() }
                            ?: old?.figure?.takeIf {
                                sourceUri != null && it.documentUri.toString() == sourceUri
                            }?.name
                            ?: old?.label?.takeIf { old.actualPortalSlot == actual },
                        sourceUri = sourceUri
                    )
                }
            }
            newSlots.forEach { slot ->
                val uri = statusUris[slot.logicalSlot]
                if (slot.actualPortalSlot < 0) releaseSlotGrant(targetPackage, slot.logicalSlot)
                else if (uri != null) rememberSlotGrant(targetPackage, slot.logicalSlot, uri)
                else if (statusHasUri[slot.logicalSlot]) {
                    // API 2 explicitly reported an unknown/empty identity. Do not retain a stale
                    // source file from the previous occupant. API 1 omits the key altogether.
                    releaseSlotGrant(targetPackage, slot.logicalSlot)
                }
            }
            val emulationState = runCatching {
                EmulationState.valueOf(root.optString("emulationState", "NONE"))
            }.getOrDefault(EmulationState.UNKNOWN)
            val gameId = root.optString("gameId").takeIf { it.isNotBlank() }
            val gameTitle = root.optString("gameTitle").takeIf { it.isNotBlank() }
            val detectedGame = SkylandersGame.detect(gameId, gameTitle)
            val portalEnabled = root.optBooleanOrNull("portalEnabled")
            val canSetPortal = root.optBoolean("canSetPortalEnabled", false) && apiVersion >= 3
            val nativeArray = root.optJSONArray("nativeSlots")
            val nativeSlots = (0 until (nativeArray?.length() ?: 0)).mapNotNull { index ->
                nativeArray?.optJSONObject(index)?.let { item ->
                    NativePortalSlotState(
                        slot = item.optInt("slot", -1),
                        occupied = item.optBoolean("occupied", false),
                        status = item.optInt("status", 0),
                        figureId = item.optInt("id", -1).takeIf { it >= 0 },
                        variantId = item.optInt("variant", -1).takeIf { it >= 0 }
                    )
                }
            }
            val catalog = if (apiVersion >= 3 && current.figureCatalog.isEmpty()) {
                loadCatalog(currentService)
            } else current.figureCatalog
            _state.value = current.copy(
                connected = true,
                connectedPackage = targetPackage,
                apiVersion = apiVersion,
                message = invalidActualSlot?.let {
                    "${DolphinTargets.label(targetPackage)} connecté, statut de portail invalide ($it)"
                } ?: "${DolphinTargets.label(targetPackage)} connecté",
                slots = newSlots,
                emulationState = emulationState,
                gameId = gameId,
                gameTitle = gameTitle,
                skylandersGame = detectedGame,
                portalEnabled = portalEnabled,
                portalActivated = root.optBooleanOrNull("portalActivated"),
                canSetPortalEnabled = canSetPortal,
                nativeSlots = nativeSlots,
                figureCatalog = catalog,
                readiness = readiness(emulationState, detectedGame, portalEnabled)
            )
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

                val currentService = service ?: return@withLock notConnectedError()
                if (!runCatching { currentService.ping() }.getOrDefault(false)) {
                    if (!isCurrentService(currentService)) return@withLock serviceReplacedError()
                    markDisconnectedIfCurrent(currentService, "Connexion Dolphin perdue")
                    return@withLock notConnectedError()
                }

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

                FigureCompatibilityEngine.check(skylander, _state.value.skylandersGame).let { compatibility ->
                    if (!compatibility.compatible) {
                        return@withLock PortalResult.Error(
                            message = compatibility.reason ?: "Cette figurine est incompatible avec le jeu actif",
                            diagnosticCode = "FIGURE_INCOMPATIBLE",
                            technicalDetails = "Jeu=${_state.value.skylandersGame?.displayName}; type=${skylander.typeLabel}; génération=${skylander.generation}",
                            recoveryHint = "Choisis une figurine compatible ou lance le jeu correspondant."
                        )
                    }
                }
                preflightSkylander(skylander)?.let { return@withLock it }
                val packageName = activeComponent?.packageName ?: return@withLock notConnectedError()
                val newUri = skylander.documentUri.toString()
                val duplicateSlot = slotForGrant(packageName, newUri)
                if (duplicateSlot != null && duplicateSlot != logicalSlot) {
                    return@withLock PortalResult.Error(
                        message = "Ce fichier est déjà placé sur le portail",
                        diagnosticCode = "DUPLICATE_SKY_FILE",
                        technicalDetails = "${skylander.fileName} est déjà associé au slot ${duplicateSlot + 1}.",
                        recoveryHint = "Retire d'abord le personnage de son autre slot."
                    )
                }
                grantUriToConnectedDolphin(skylander)?.let { return@withLock it }
                if (!isCurrentService(currentService)) {
                    revokeTemporaryGrant(packageName, newUri)
                    return@withLock serviceReplacedError()
                }

                _state.update { it.copy(message = "Placement de ${skylander.name}…") }
                val loadAttempt = runCatching {
                    currentService.load(logicalSlot, skylander.documentUri.toString(), skylander.name)
                }
                val actual = loadAttempt.getOrElse { error ->
                    revokeTemporaryGrant(packageName, newUri)
                    logFailure("load", error)
                    if (error is DeadObjectException) {
                        markDisconnectedIfCurrent(currentService, "Connexion Dolphin perdue")
                    }
                    return@withLock binderError(error)
                }

                if (!isCurrentService(currentService)) {
                    revokeTemporaryGrant(packageName, newUri)
                    return@withLock serviceReplacedError()
                }

                if (!PortalProtocol.isValidActualSlot(actual)) {
                    revokeTemporaryGrant(packageName, newUri)
                    val error = mapLoadError(actual, skylander)
                    _state.update { it.copy(message = "Échec : ${error.message}") }
                    return@withLock error
                }

                rememberSlotGrant(packageName, logicalSlot, newUri)
                _state.update { current ->
                    val slots = current.slots.toMutableList()
                    slots[logicalSlot] = PortalSlotState(
                        logicalSlot = logicalSlot,
                        actualPortalSlot = actual,
                        figure = skylander,
                        label = skylander.name,
                        sourceUri = newUri
                    )
                    current.copy(
                        connected = true,
                        slots = slots,
                        message = "${skylander.name} est sur le portail"
                    )
                }
                PortalResult.Success(actual, "${skylander.name} chargé avec succès")
            }
        }

    override suspend fun remove(logicalSlot: Int): PortalResult = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            if (logicalSlot !in 0 until LOGICAL_SLOT_COUNT) {
                return@withLock PortalResult.Error("Slot invalide", "INVALID_SLOT")
            }
            val currentService = service ?: return@withLock notConnectedError()
            val attempt = runCatching { currentService.remove(logicalSlot) }
            val ok = attempt.getOrElse { error ->
                logFailure("remove", error)
                if (error is DeadObjectException) {
                    markDisconnectedIfCurrent(currentService, "Connexion Dolphin perdue")
                }
                return@withLock binderError(error)
            }
            if (!isCurrentService(currentService)) return@withLock serviceReplacedError()
            if (!ok) {
                return@withLock PortalResult.Error(
                    message = "Dolphin n'a pas pu retirer ce slot",
                    diagnosticCode = "REMOVE_REJECTED",
                    recoveryHint = "Vérifie que le jeu et le portail émulé sont toujours actifs."
                )
            }
            activeComponent?.packageName?.let { packageName -> releaseSlotGrant(packageName, logicalSlot) }
            _state.update { current ->
                val slots = current.slots.toMutableList()
                slots[logicalSlot] = PortalSlotState(logicalSlot)
                current.copy(slots = slots, message = "Slot ${logicalSlot + 1} retiré")
            }
            PortalResult.Success(message = "Personnage retiré du slot ${logicalSlot + 1}")
        }
    }

    override suspend fun clear(): PortalResult = withContext(Dispatchers.IO) {
        operationMutex.withLock {
            val currentService = service ?: return@withLock notConnectedError()
            val attempt = runCatching { currentService.clear() }
            attempt.exceptionOrNull()?.let { error ->
                logFailure("clear", error)
                if (error is DeadObjectException) {
                    markDisconnectedIfCurrent(currentService, "Connexion Dolphin perdue")
                }
                return@withLock binderError(error)
            }
            if (!isCurrentService(currentService)) return@withLock serviceReplacedError()
            activeComponent?.packageName?.let(::revokeAllGrants)
            _state.update {
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
            val currentService = service ?: return@withLock notConnectedError()
            if ((_state.value.apiVersion ?: 1) < 3 || !_state.value.canSetPortalEnabled) {
                return@withLock PortalResult.Error(
                    "Cette version de Dolphin ne permet pas d’activer le portail depuis SkyPortal",
                    "PORTAL_TOGGLE_UNSUPPORTED",
                    recoveryHint = "Installe le patch Dolphin API 3 ou active le portail dans les réglages Dolphin."
                )
            }
            _state.update { it.copy(readiness = SmartPortalReadiness.ENABLING_PORTAL, message = "Activation du portail…") }
            val code = runCatching { currentService.setPortalEnabled(enabled) }.getOrElse { error ->
                if (error is DeadObjectException) markDisconnectedIfCurrent(currentService, "Connexion Dolphin perdue")
                return@withLock binderError(error)
            }
            if (!isCurrentService(currentService)) return@withLock serviceReplacedError()
            if (code != 0) {
                _state.update { it.copy(readiness = SmartPortalReadiness.ERROR, message = "Activation du portail refusée") }
                return@withLock PortalResult.Error("Dolphin n’a pas pu modifier l’état du portail", "PORTAL_TOGGLE_$code")
            }
            refreshLocked()
            PortalResult.Success(message = if (enabled) "Portal of Power activé" else "Portal of Power désactivé")
        }
    }

    override fun close() {
        disconnectCurrentBinding()
        bridgeScope.cancel()
        _state.update { it.copy(connected = false, connectedPackage = null) }
    }

    private fun preflightSkylander(skylander: Skylander): PortalResult.Error? {
        val attempt = runCatching {
            context.contentResolver.openFileDescriptor(skylander.documentUri, "rw")?.use { descriptor ->
                check(descriptor.fileDescriptor.valid()) { "Descripteur de fichier invalide" }
                check(descriptor.statSize != 0L) { "Le fichier est vide" }
                check(descriptor.statSize == -1L || descriptor.statSize == SKY_DUMP_SIZE_BYTES) {
                    "Taille .sky invalide : ${descriptor.statSize} octets"
                }
            } ?: error("Le fournisseur de documents n'a renvoyé aucun fichier")
        }
        val failure = attempt.exceptionOrNull() ?: return null
        logFailure("uri-preflight", failure)
        return PortalResult.Error(
            message = "Le fichier ${skylander.fileName} n'est pas accessible en lecture/écriture",
            diagnosticCode = if (failure is SecurityException) "SAF_PERMISSION_DENIED" else "SKY_FILE_UNREADABLE",
            technicalDetails = failure.summary(),
            recoveryHint = "Touche Dossier, sélectionne à nouveau le dossier Skylanders et accorde l'accès complet."
        )
    }

    private fun grantUriToConnectedDolphin(skylander: Skylander): PortalResult.Error? {
        val packageName = activeComponent?.packageName ?: return notConnectedError()
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val attempt = runCatching {
            context.grantUriPermission(packageName, skylander.documentUri, flags)
        }
        val failure = attempt.exceptionOrNull() ?: return null
        logFailure("uri-grant", failure)
        return PortalResult.Error(
            message = "Android a refusé de partager le fichier avec ${DolphinTargets.label(packageName)}",
            diagnosticCode = "URI_GRANT_FAILED",
            technicalDetails = failure.summary(),
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

    private fun slotForGrant(packageName: String, uriString: String): Int? = synchronized(grantLock) {
        grantedUris.entries.firstOrNull { (key, value) ->
            key.packageName == packageName && value == uriString
        }?.key?.logicalSlot
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
        message = if (error is SecurityException) {
            "Android a refusé l'appel vers Dolphin"
        } else {
            "La communication avec Dolphin a échoué"
        },
        diagnosticCode = when (error) {
            is SecurityException -> "BINDER_PERMISSION_DENIED"
            is DeadObjectException -> "BINDER_DISCONNECTED"
            else -> "BINDER_CALL_FAILED"
        },
        technicalDetails = error.summary(),
        recoveryHint = if (error is SecurityException) {
            "Les APK SkyPortal et Dolphin doivent être signés avec la même clé."
        } else {
            "Reconnecte Dolphin puis réessaie."
        }
    )

    private fun diagnosticContext(skylander: Skylander): String = buildString {
        append("Fichier : ${skylander.fileName}")
        append(" • Cible : ${DolphinTargets.label(activeComponent?.packageName)}")
        append(" • API : ${_state.value.apiVersion ?: "inconnue"}")
    }

    private fun loadCatalog(currentService: ISkylanderPortalService) = runCatching {
        val array = JSONObject(currentService.figureCatalogJson).getJSONArray("figures")
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
        emulation: EmulationState,
        game: SkylandersGame?,
        portalEnabled: Boolean?
    ): SmartPortalReadiness = when {
        emulation == EmulationState.NONE || emulation == EmulationState.STOPPING -> SmartPortalReadiness.NO_GAME
        emulation == EmulationState.STARTING -> SmartPortalReadiness.GAME_DETECTED
        game == null -> SmartPortalReadiness.GAME_DETECTED
        portalEnabled == false -> SmartPortalReadiness.PORTAL_DISABLED
        portalEnabled == true -> SmartPortalReadiness.READY
        else -> SmartPortalReadiness.GAME_DETECTED
    }

    private fun markDisconnected(message: String) {
        service = null
        _state.update { it.copy(connected = false, readiness = SmartPortalReadiness.DOLPHIN_DETECTED, message = message) }
    }

    private fun markDisconnectedIfCurrent(expected: ISkylanderPortalService, message: String) {
        if (isCurrentService(expected)) markDisconnected(message)
    }

    private fun isCurrentService(expected: ISkylanderPortalService): Boolean =
        service?.asBinder() == expected.asBinder()

    private fun disconnectCurrentBinding() {
        activeComponent?.packageName?.let(::revokeAllGrants)
        val conn = connection
        if (conn != null && bound) runCatching { context.unbindService(conn) }
        bound = false
        service = null
        connection = null
        activeComponent = null
    }

    private fun isServiceAvailable(component: ComponentName): Boolean = try {
        context.packageManager.getServiceInfo(component, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private fun logFailure(stage: String, error: Throwable) {
        Log.e(TAG, "$stage failed: ${error.summary()}")
    }

    private fun Throwable.summary(): String = "${javaClass.simpleName}: ${message ?: "sans détail"}"

    private data class GrantSlotKey(val packageName: String, val logicalSlot: Int)

    companion object {
        private const val TAG = "SkyPortalBridge"
        private const val CONNECT_TIMEOUT_MS = 5_000L
        private const val LOGICAL_SLOT_COUNT = 8
        private const val SKY_DUMP_SIZE_BYTES = 1_024L
    }
}

private fun JSONObject.optBooleanOrNull(name: String): Boolean? =
    if (has(name) && !isNull(name)) optBoolean(name) else null

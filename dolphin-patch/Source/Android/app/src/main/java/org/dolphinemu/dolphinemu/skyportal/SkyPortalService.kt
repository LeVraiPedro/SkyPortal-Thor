// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package org.dolphinemu.dolphinemu.skyportal

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.skyportalthor.ipc.ISkylanderPortalService
import org.dolphinemu.dolphinemu.NativeLibrary
import org.dolphinemu.dolphinemu.features.settings.model.BooleanSetting
import org.dolphinemu.dolphinemu.features.settings.model.NativeConfig
import org.dolphinemu.dolphinemu.features.skylanders.SkylanderConfig
import org.dolphinemu.dolphinemu.utils.DirectoryInitialization
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Minimal local companion API for SkyPortal Thor.
 *
 * The native Skylanders implementation remains owned by Dolphin. This service only exposes the
 * already-existing load/remove functions to a separately signed companion app.
 */
class SkyPortalService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val binder = object : ISkylanderPortalService.Stub() {
        override fun getApiVersion(): Int = API_VERSION

        override fun ping(): Boolean = true

        override fun load(logicalSlot: Int, uri: String?, displayName: String?): Int {
            if (logicalSlot !in 0 until LOGICAL_SLOT_COUNT) return ERROR_BAD_SLOT
            if (uri.isNullOrBlank()) return ERROR_OPEN_FAILED
            if (!nativeRuntimeReady()) return ERROR_DOLPHIN_NOT_READY

            val uriAccess = runCatching {
                contentResolver.openFileDescriptor(Uri.parse(uri), "rw")?.use { descriptor ->
                    check(descriptor.fileDescriptor.valid()) { "Invalid file descriptor" }
                    check(descriptor.statSize != 0L) { "Empty .sky file" }
                    check(descriptor.statSize == -1L || descriptor.statSize == SKY_DUMP_SIZE_BYTES) {
                        "Invalid .sky size: ${descriptor.statSize} bytes"
                    }
                } ?: error("Document provider returned no file descriptor")
            }
            if (uriAccess.isFailure) {
                Log.e(TAG, "Cannot open shared .sky URI (${uriAccess.exceptionOrNull()?.javaClass?.simpleName})")
                return ERROR_URI_ACCESS
            }

            val identity = runCatching { readFigureIdentity(Uri.parse(uri)) }.getOrElse { error ->
                Log.e(TAG, "Invalid .sky identity (${error.javaClass.simpleName})")
                return ERROR_SKY_DATA
            }
            val catalogEntry = findCatalogEntry(identity.first, identity.second)
            if (catalogEntry == null) {
                Log.w(TAG, "Unknown Skylander identity ${identity.first}/${identity.second}")
                return ERROR_UNKNOWN_FIGURE
            }

            return onMainThread {
                synchronized(lock) {
                    val metadataValid = NativeLibrary.IsGameMetadataValid()
                    val gameId = if (metadataValid) NativeLibrary.GetCurrentGameID() else ""
                    val gameTitle = if (metadataValid) NativeLibrary.GetCurrentTitleDescription() else ""
                    if (!isCompatibleWithRunningGame(catalogEntry, gameId, gameTitle)) {
                        Log.w(TAG, "Skylander identity rejected for the running game")
                        return@synchronized ERROR_INCOMPATIBLE_FIGURE
                    }
                    val nativeSnapshot = SkylanderConfig.getPortalSnapshot()
                    if (!reconcileLogicalMappings(nativeSnapshot)) {
                        Log.e(TAG, "Refusing load because the native portal snapshot is invalid")
                        return@synchronized ERROR_UNIDENTIFIED_NATIVE_MOUNT
                    }
                    firstUnclaimedOccupiedNativeSlot(nativeSnapshot)?.let { nativeSlot ->
                        Log.w(TAG, "Refusing load while unclaimed native slot $nativeSlot is occupied")
                        return@synchronized ERROR_UNIDENTIFIED_NATIVE_MOUNT
                    }
                    val emulationState = SkylanderConfig.getEmulationState()
                    val skylandersGameActive = emulationState in EMULATION_PAUSED..EMULATION_RUNNING &&
                        runningGameGeneration(gameId, gameTitle) != null
                    if (skylandersGameActive && BooleanSetting.MAIN_EMULATE_INFINITY_BASE.boolean) {
                        Log.w(TAG, "Refusing load while the Disney Infinity base is enabled")
                        return@synchronized ERROR_CONFLICTING_USB_DEVICE
                    }
                    if (skylandersGameActive) {
                        val portalUsbState = SkylanderConfig.getPortalUsbState()
                        if (!BooleanSetting.MAIN_EMULATE_SKYLANDER_PORTAL.boolean ||
                            portalUsbState and PORTAL_USB_READY_MASK != PORTAL_USB_READY_MASK
                        ) {
                            Log.w(TAG, "Refusing load before the game completes the portal USB handshake")
                            return@synchronized ERROR_PORTAL_USB_NOT_READY
                        }
                    }
                    val previousActual = logicalToActual[logicalSlot]
                    val loadAttempt = runCatching {
                        SkylanderConfig.loadSkylander(previousActual, uri)
                    }
                    val pair = loadAttempt.getOrElse { error ->
                        Log.e(TAG, "SkylanderConfig rejected the .sky data (${error.javaClass.simpleName})")
                        return@synchronized ERROR_SKY_DATA
                    } ?: return@synchronized ERROR_SKY_DATA

                    val actual = pair.first ?: return@synchronized ERROR_SKY_DATA
                    if (actual == NATIVE_NO_SLOT || actual !in 0 until MAX_PORTAL_SLOTS) {
                        Log.w(TAG, "Portal is full; native slot=$actual")
                        // JNI has already removed the previous native slot before attempting the
                        // replacement, so keeping the old logical mapping would create a phantom.
                        clearLogicalSlot(logicalSlot)
                        return@synchronized ERROR_PORTAL_FULL
                    }
                    val mountedFigure = nativeSlot(
                        SkylanderConfig.getPortalSnapshot(),
                        actual
                    )
                    if (mountedFigure == null) {
                        // The native call returned a slot, so ownership is uncertain until an exact
                        // snapshot can be read. Keep the URI/mapping and throw: the companion treats
                        // Binder exceptions as uncertain and retains its URI grant.
                        rememberLogicalMount(logicalSlot, actual, displayName, uri, identity)
                        throw IllegalStateException(
                            "Native portal snapshot became invalid after loading slot $actual"
                        )
                    }
                    if (!mountedFigure.mounted) {
                        Log.e(
                            TAG,
                            "Native load returned an unmounted slot $actual"
                        )
                        clearLogicalSlot(logicalSlot)
                        return@synchronized ERROR_SKY_DATA
                    }
                    if (mountedFigure.figureId != identity.first ||
                        mountedFigure.variantId != identity.second
                    ) {
                        Log.e(TAG, "Native load identity mismatch at slot $actual; attempting cleanup")
                        runCatching { SkylanderConfig.removeSkylander(actual) }
                        val afterCleanup = nativeSlot(SkylanderConfig.getPortalSnapshot(), actual)
                        if (afterCleanup != null && !afterCleanup.mounted) {
                            clearLogicalSlot(logicalSlot)
                            return@synchronized ERROR_SKY_DATA
                        }
                        val retainedIdentity = if (afterCleanup?.mounted == true) {
                            afterCleanup.figureId to afterCleanup.variantId
                        } else {
                            identity
                        }
                        rememberLogicalMount(
                            logicalSlot,
                            actual,
                            displayName,
                            uri,
                            retainedIdentity
                        )
                        throw IllegalStateException(
                            "Native load identity could not be cleaned up at slot $actual"
                        )
                    }
                    for (otherLogical in 0 until LOGICAL_SLOT_COUNT) {
                        if (otherLogical != logicalSlot && logicalToActual[otherLogical] == actual) {
                            Log.w(TAG, "Dropping duplicate logical mapping for native slot $actual")
                            clearLogicalSlot(otherLogical)
                        }
                    }
                    rememberLogicalMount(
                        logicalSlot,
                        actual,
                        pair.second ?: displayName,
                        uri,
                        identity
                    )
                    actual
                }
            }
        }

        override fun remove(logicalSlot: Int): Boolean {
            if (logicalSlot !in 0 until LOGICAL_SLOT_COUNT) return false
            if (!nativeRuntimeReady()) {
                synchronized(lock) { clearLogicalSlot(logicalSlot) }
                return true
            }
            return onMainThread {
                synchronized(lock) {
                    val previouslyMapped = logicalToActual[logicalSlot]
                    val nativeSnapshot = SkylanderConfig.getPortalSnapshot()
                    if (!reconcileLogicalMappings(nativeSnapshot)) {
                        Log.e(TAG, "Refusing removal because the native portal snapshot is invalid")
                        return@synchronized false
                    }
                    if (previouslyMapped >= 0 && logicalToActual[logicalSlot] < 0) {
                        Log.w(TAG, "Refusing removal because logical slot $logicalSlot changed natively")
                        return@synchronized false
                    }
                    firstUnclaimedOccupiedNativeSlot(nativeSnapshot)?.let { nativeSlot ->
                        Log.w(TAG, "Refusing removal while unclaimed native slot $nativeSlot is occupied")
                        return@synchronized false
                    }
                    val actual = logicalToActual[logicalSlot]
                    if (actual < 0) return@synchronized true
                    val removal = runCatching { SkylanderConfig.removeSkylander(actual) }
                    val nativeAccepted = removal.getOrElse { error ->
                        Log.e(TAG, "Failed to remove native portal slot $actual", error)
                        return@synchronized false
                    }
                    val remaining = nativeSlot(SkylanderConfig.getPortalSnapshot(), actual)
                    when {
                        remaining == null -> {
                            Log.e(TAG, "Cannot confirm removal: native snapshot is invalid")
                            false
                        }
                        !remaining.mounted -> {
                            clearLogicalSlot(logicalSlot)
                            true
                        }
                        remaining.figureId != figureIds[logicalSlot] ||
                            remaining.variantId != variantIds[logicalSlot] -> {
                            Log.w(TAG, "Native slot $actual changed identity during removal")
                            clearLogicalSlot(logicalSlot)
                            false
                        }
                        else -> {
                            Log.w(
                                TAG,
                                "Native slot $actual is still mounted after removal " +
                                    "(accepted=$nativeAccepted, status=${remaining.status})"
                            )
                            false
                        }
                    }
                }
            }
        }

        override fun clear() {
            if (!nativeRuntimeReady()) {
                synchronized(lock) {
                    for (logical in 0 until LOGICAL_SLOT_COUNT) clearLogicalSlot(logical)
                }
                return
            }
            onMainThread {
                synchronized(lock) {
                    val initialSnapshot = SkylanderConfig.getPortalSnapshot()
                    if (!reconcileLogicalMappings(initialSnapshot)) {
                        throw IllegalStateException("Native portal snapshot is invalid; clear refused")
                    }
                    firstUnclaimedOccupiedNativeSlot(initialSnapshot)?.let { nativeSlot ->
                        throw IllegalStateException(
                            "Native portal slot $nativeSlot is mounted without a logical owner; clear refused"
                        )
                    }
                    var firstFailure: Throwable? = null
                    for (logical in 0 until LOGICAL_SLOT_COUNT) {
                        val actual = logicalToActual[logical]
                        if (actual < 0) {
                            clearLogicalSlot(logical)
                            continue
                        }
                        val removal = runCatching { SkylanderConfig.removeSkylander(actual) }
                        removal.onFailure { error ->
                            Log.e(TAG, "Failed to clear native portal slot $actual", error)
                            if (firstFailure == null) firstFailure = error
                        }.onSuccess { accepted ->
                            val remaining = nativeSlot(SkylanderConfig.getPortalSnapshot(), actual)
                            if (remaining == null) {
                                Log.e(TAG, "Cannot confirm clear: native snapshot is invalid")
                                if (firstFailure == null) {
                                    firstFailure = IllegalStateException(
                                        "Native portal snapshot became invalid while clearing"
                                    )
                                }
                            } else if (!remaining.mounted) {
                                clearLogicalSlot(logical)
                            } else {
                                val sameFigure = remaining.figureId == figureIds[logical] &&
                                    remaining.variantId == variantIds[logical]
                                if (!sameFigure) clearLogicalSlot(logical)
                                Log.w(
                                    TAG,
                                    "Native slot $actual remained mounted while clearing " +
                                        "(accepted=$accepted, sameFigure=$sameFigure)"
                                )
                                if (firstFailure == null) {
                                    firstFailure = IllegalStateException(
                                        "Native portal slot $actual remained mounted"
                                    )
                                }
                            }
                        }
                    }
                    val finalSnapshot = SkylanderConfig.getPortalSnapshot()
                    if (!isValidNativeSnapshot(finalSnapshot)) {
                        if (firstFailure == null) {
                            firstFailure = IllegalStateException(
                                "Native portal snapshot became invalid after clearing"
                            )
                        }
                    } else {
                        val remainingMount = (0 until MAX_PORTAL_SLOTS).firstOrNull { slot ->
                            nativeSlot(finalSnapshot, slot)?.mounted == true
                        }
                        if (remainingMount != null && firstFailure == null) {
                            firstFailure = IllegalStateException(
                                "Native portal slot $remainingMount remained mounted after clearing"
                            )
                        }
                    }
                    if (logicalToActual.any { it >= 0 } && firstFailure == null) {
                        firstFailure = IllegalStateException(
                            "One or more logical portal slots remained mapped after clearing"
                        )
                    }
                    firstFailure?.let { throw IllegalStateException("One or more portal slots could not be cleared", it) }
                }
            }
        }

        override fun setPortalEnabled(enabled: Boolean): Int {
            if (!nativeRuntimeReady()) return ERROR_DOLPHIN_NOT_READY
            return onMainThread {
                runCatching {
                    // Persist the global choice, and also update the current emulation layer so the
                    // USB scanner observes the change immediately even with per-game settings active.
                    BooleanSetting.MAIN_EMULATE_SKYLANDER_PORTAL.setBoolean(
                        NativeConfig.LAYER_BASE,
                        enabled
                    )
                    NativeConfig.save(NativeConfig.LAYER_BASE)
                    if (SkylanderConfig.getEmulationState() != EMULATION_UNINITIALIZED) {
                        BooleanSetting.MAIN_EMULATE_SKYLANDER_PORTAL.setBoolean(
                            NativeConfig.LAYER_CURRENT,
                            enabled
                        )
                    }
                    if (!enabled) SkylanderConfig.getPortalSnapshot()
                    PORTAL_TOGGLE_OK
                }.getOrElse { error ->
                    Log.e(TAG, "Failed to change emulated Skylanders portal setting", error)
                    ERROR_PORTAL_TOGGLE
                }
            }
        }

        override fun getFigureCatalogJson(): String {
            if (!nativeRuntimeReady()) return emptyCatalogJson()
            return onMainThread {
                val entries = JSONArray()
                SkylanderConfig.getSkylanderCatalog().forEach { packed ->
                    val parts = packed.split('|', limit = 6)
                    if (parts.size == 6) {
                        entries.put(
                            JSONObject()
                                .put("id", parts[0].toIntOrNull() ?: -1)
                                .put("variant", parts[1].toIntOrNull() ?: -1)
                                .put("name", parts[2])
                                .put("game", parts[3].toIntOrNull() ?: -1)
                                .put("element", parts[4].toIntOrNull() ?: -1)
                                .put("type", parts[5].toIntOrNull() ?: -1)
                        )
                    }
                }
                JSONObject().put("version", 1).put("figures", entries).toString()
            }
        }

        override fun getPortalLedStateJson(): String {
            if (!nativeRuntimeReady()) return emptyPortalLedStateJson()
            return onMainThread {
                encodePortalLedState(SkylanderConfig.getPortalLedState())
            }
        }

        override fun getStatusJson(): String = onMainThread { synchronized(lock) {
            if (!nativeRuntimeReady()) return@synchronized uninitializedStatusJson()
            val nativeSnapshot = SkylanderConfig.getPortalSnapshot()
            if (!reconcileLogicalMappings(nativeSnapshot)) {
                Log.e(TAG, "Native portal snapshot has an invalid layout")
                // Do not synthesize empty slots: a Binder failure makes the companion retain its
                // last confirmed ownership instead of reconciling against false emptiness.
                throw IllegalStateException("Native portal snapshot has an invalid layout")
            }
            val nativeSlots = JSONArray()
            for (slot in 0 until MAX_PORTAL_SLOTS) {
                val nativeSlot = nativeSlot(nativeSnapshot, slot) ?: continue
                nativeSlots.put(
                    JSONObject()
                        .put("slot", nativeSlot.slot)
                        // `occupied` means Dolphin owns an open .sky file. It deliberately remains
                        // true while status walks REMOVING/REMOVED/ADDED during replacement.
                        .put("occupied", nativeSlot.mounted)
                        .put("status", nativeSlot.status)
                        .put("id", nativeSlot.figureId)
                        .put("variant", nativeSlot.variantId)
                )
            }
            val slots = JSONArray()
            for (logical in 0 until LOGICAL_SLOT_COUNT) {
                slots.put(
                    JSONObject()
                        .put("logicalSlot", logical)
                        .put("actualSlot", logicalToActual[logical])
                        .put("label", labels[logical] ?: "")
                        .put("uri", uris[logical] ?: "")
                )
            }
            val emulationState = SkylanderConfig.getEmulationState()
            val metadataValid = NativeLibrary.IsGameMetadataValid()
            val portalEnabled = BooleanSetting.MAIN_EMULATE_SKYLANDER_PORTAL.boolean
            val portalUsbState = SkylanderConfig.getPortalUsbState()
            val portalUsbPresent = portalUsbState and PORTAL_USB_PRESENT != 0
            val portalUsbAttached = portalUsbState and PORTAL_USB_ATTACHED != 0
            val portalUsbHandshakeSeen = portalUsbState and PORTAL_USB_HANDSHAKE_SEEN != 0
            val conflictingUsbDevices = JSONArray()
            if (BooleanSetting.MAIN_EMULATE_INFINITY_BASE.boolean) {
                conflictingUsbDevices.put(CONFLICT_DISNEY_INFINITY_BASE)
            }
            val portalProtocolActivated = SkylanderConfig.isPortalActivated()
            val portalEffectiveActivated = portalEnabled && portalUsbPresent &&
                portalUsbAttached && portalUsbHandshakeSeen && portalProtocolActivated &&
                conflictingUsbDevices.length() == 0
            JSONObject()
                .put("apiVersion", API_VERSION)
                // Schema 2 guarantees nativeSlots[].occupied is FileIsOpen ownership and is
                // independent from nativeSlots[].status. API 3 payloads without this capability
                // used status as occupancy and must not confirm a replacement in transition.
                .put("nativeSlotSchemaVersion", NATIVE_SLOT_SCHEMA_VERSION)
                .put("slots", slots)
                .put("nativeSlots", nativeSlots)
                .put("emulationState", emulationStateName(emulationState))
                .put("gameId", if (metadataValid) NativeLibrary.GetCurrentGameID() else "")
                .put("gameTitle", if (metadataValid) NativeLibrary.GetCurrentTitleDescription() else "")
                .put("portalEnabled", portalEnabled)
                .put("portalUsbPresent", portalUsbPresent)
                .put("portalUsbAttached", portalUsbAttached)
                .put("portalUsbHandshakeSeen", portalUsbHandshakeSeen)
                // IsActivated defaults to true in Dolphin's native portal object. Gate the legacy
                // field on a real protocol exchange so it can no longer create a false READY state.
                .put("portalActivated", portalEffectiveActivated)
                .put("portalProtocolActivated", portalProtocolActivated)
                .put("conflictingUsbDevices", conflictingUsbDevices)
                .put("canSetPortalEnabled", true)
                .put("serviceState", "READY")
                .toString()
        } }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun nativeRuntimeReady(): Boolean = DirectoryInitialization.areDolphinDirectoriesReady()

    /**
     * The exported service can be rebound while Dolphin's process is starting. NativeConfig must
     * not be touched until DirectoryInitialization has called NativeLibrary.Initialize(), because
     * doing so dereferences an uninitialised configuration layer in JNI. Return a complete, empty
     * API 3 snapshot instead; the companion polls and will observe the live state once ready.
     */
    private fun uninitializedStatusJson(): String {
        val slots = JSONArray()
        for (logical in 0 until LOGICAL_SLOT_COUNT) {
            clearLogicalSlot(logical)
            slots.put(
                JSONObject()
                    .put("logicalSlot", logical)
                    .put("actualSlot", -1)
                    .put("label", "")
                    .put("uri", "")
            )
        }
        val nativeSlots = JSONArray()
        for (slot in 0 until MAX_PORTAL_SLOTS) {
            nativeSlots.put(
                JSONObject()
                    .put("slot", slot)
                    .put("occupied", false)
                    .put("status", 0)
                    .put("id", -1)
                    .put("variant", -1)
            )
        }
        return JSONObject()
            .put("apiVersion", API_VERSION)
            .put("nativeSlotSchemaVersion", NATIVE_SLOT_SCHEMA_VERSION)
            .put("slots", slots)
            .put("nativeSlots", nativeSlots)
            .put("emulationState", "NONE")
            .put("gameId", "")
            .put("gameTitle", "")
            .put("portalEnabled", JSONObject.NULL)
            .put("portalUsbPresent", false)
            .put("portalUsbAttached", false)
            .put("portalUsbHandshakeSeen", false)
            .put("portalActivated", JSONObject.NULL)
            .put("portalProtocolActivated", JSONObject.NULL)
            .put("conflictingUsbDevices", JSONArray())
            .put("canSetPortalEnabled", false)
            .put("serviceState", "INITIALIZING")
            .toString()
    }

    private fun emptyCatalogJson(): String =
        JSONObject().put("version", 1).put("figures", JSONArray()).toString()

    private fun emptyPortalLedStateJson(): String = JSONObject()
        .put("schemaVersion", PORTAL_LED_SCHEMA_VERSION)
        .put("active", false)
        .put("sequence", 0L)
        .toString()

    private fun encodePortalLedState(packed: LongArray): String {
        check(packed.size == PORTAL_LED_STATE_SIZE) {
            "Invalid native portal LED state size: ${packed.size}"
        }
        val schemaVersion = packed[PORTAL_LED_SCHEMA_OFFSET]
        check(schemaVersion == PORTAL_LED_SCHEMA_VERSION.toLong()) {
            "Unsupported native portal LED schema: $schemaVersion"
        }
        val activeRaw = packed[PORTAL_LED_ACTIVE_OFFSET]
        check(activeRaw in 0L..1L) { "Invalid native portal LED active flag: $activeRaw" }
        val sequence = packed[PORTAL_LED_SEQUENCE_OFFSET]
        check(sequence >= 0L) { "Invalid native portal LED sequence: $sequence" }

        return JSONObject()
            .put("schemaVersion", PORTAL_LED_SCHEMA_VERSION)
            .put("active", activeRaw == 1L)
            .put("sequence", sequence)
            .put("left", colorJson(packed, PORTAL_LED_LEFT_OFFSET))
            .put("right", colorJson(packed, PORTAL_LED_RIGHT_OFFSET))
            .put("trap", colorJson(packed, PORTAL_LED_TRAP_OFFSET))
            .toString()
    }

    private fun colorJson(packed: LongArray, offset: Int): JSONObject {
        val red = packed[offset]
        val green = packed[offset + 1]
        val blue = packed[offset + 2]
        check(red in 0L..255L && green in 0L..255L && blue in 0L..255L) {
            "Invalid native portal LED channel at offset $offset"
        }
        return JSONObject()
            .put("r", red.toInt())
            .put("g", green.toInt())
            .put("b", blue.toInt())
    }

    private fun clearLogicalSlot(logical: Int) {
        logicalToActual[logical] = -1
        labels[logical] = null
        uris[logical] = null
        figureIds[logical] = -1
        variantIds[logical] = -1
    }

    private fun rememberLogicalMount(
        logical: Int,
        actual: Int,
        label: String?,
        uri: String,
        identity: Pair<Int, Int>
    ) {
        logicalToActual[logical] = actual
        labels[logical] = label ?: "Unknown"
        uris[logical] = uri
        figureIds[logical] = identity.first
        variantIds[logical] = identity.second
    }

    /**
     * The Android Skylanders Manager can mutate native slots while the companion is connected.
     * Never use a cached native slot for a destructive operation until its identity is confirmed.
     */
    private fun reconcileLogicalMappings(nativeSnapshot: IntArray): Boolean {
        if (!isValidNativeSnapshot(nativeSnapshot)) return false
        val claimedNativeSlots = mutableSetOf<Int>()
        for (logical in 0 until LOGICAL_SLOT_COUNT) {
            val actual = logicalToActual[logical]
            if (actual < 0) continue
            val mountedSlot = nativeSlot(nativeSnapshot, actual)?.takeIf { it.mounted }
            val nativeOccupied = mountedSlot != null
            val nativeId = mountedSlot?.figureId ?: -1
            val nativeVariant = mountedSlot?.variantId ?: -1
            val identityMatches = figureIds[logical] >= 0 &&
                figureIds[logical] == nativeId && variantIds[logical] == nativeVariant
            val uniqueMapping = claimedNativeSlots.add(actual)
            if (!nativeOccupied || !identityMatches || !uniqueMapping) {
                Log.w(TAG, "Dropping stale logical portal mapping $logical -> $actual")
                clearLogicalSlot(logical)
            }
        }
        return true
    }

    private fun firstUnclaimedOccupiedNativeSlot(nativeSnapshot: IntArray): Int? {
        val claimed = logicalToActual.filter { it in 0 until MAX_PORTAL_SLOTS }.toSet()
        return (0 until MAX_PORTAL_SLOTS).firstOrNull { slot ->
            nativeSlot(nativeSnapshot, slot)?.mounted == true && slot !in claimed
        }
    }

    private fun nativeSlot(nativeSnapshot: IntArray, slot: Int): NativeSlotState? {
        if (slot !in 0 until MAX_PORTAL_SLOTS) return null
        if (!isValidNativeSnapshot(nativeSnapshot)) return null
        val offset = slot * NATIVE_SNAPSHOT_STRIDE
        if (nativeSnapshot[offset + NATIVE_SLOT_OFFSET] != slot) return null
        val mounted = nativeSnapshot[offset + NATIVE_MOUNTED_OFFSET] != 0
        return NativeSlotState(
            slot = slot,
            mounted = mounted,
            status = nativeSnapshot[offset + NATIVE_STATUS_OFFSET],
            figureId = if (mounted) nativeSnapshot[offset + NATIVE_FIGURE_ID_OFFSET] else -1,
            variantId = if (mounted) nativeSnapshot[offset + NATIVE_VARIANT_OFFSET] else -1
        )
    }

    private fun isValidNativeSnapshot(nativeSnapshot: IntArray): Boolean {
        if (nativeSnapshot.size != MAX_PORTAL_SLOTS * NATIVE_SNAPSHOT_STRIDE) return false
        return (0 until MAX_PORTAL_SLOTS).all { slot ->
            val offset = slot * NATIVE_SNAPSHOT_STRIDE
            nativeSnapshot[offset + NATIVE_SLOT_OFFSET] == slot &&
                nativeSnapshot[offset + NATIVE_MOUNTED_OFFSET] in 0..1 &&
                nativeSnapshot[offset + NATIVE_STATUS_OFFSET] in NATIVE_STATUS_REMOVED..NATIVE_STATUS_ADDED
        }
    }

    private fun readFigureIdentity(uri: Uri): Pair<Int, Int> {
        val bytes = contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(SKY_DUMP_SIZE_BYTES.toInt() + 1)
            var offset = 0
            var consecutiveEmptyReads = 0
            while (offset < buffer.size) {
                val count = input.read(buffer, offset, buffer.size - offset)
                if (count < 0) break
                if (count == 0) {
                    if (++consecutiveEmptyReads >= MAX_EMPTY_READS) break
                    continue
                }
                consecutiveEmptyReads = 0
                offset += count
            }
            buffer.copyOf(offset)
        } ?: error("Document provider returned no input stream")
        check(bytes.size == SKY_DUMP_SIZE_BYTES.toInt()) { "Invalid .sky size" }
        val bcc = (bytes[0].toInt() xor bytes[1].toInt() xor bytes[2].toInt() xor bytes[3].toInt()) and 0xff
        check((bytes[4].toInt() and 0xff) == bcc) { "Invalid BCC" }
        check((bytes[5].toInt() and 0xff) == 0x81 && (bytes[6].toInt() and 0xff) == 0x01) { "Invalid ATQA" }
        check((bytes[7].toInt() and 0xff) == 0x0f) { "Invalid SAK" }
        val storedCrc = littleEndianU16(bytes, 0x1e)
        check(storedCrc == crc16(bytes, 0x1e)) { "Invalid identity checksum" }
        return littleEndianU16(bytes, 0x10) to littleEndianU16(bytes, 0x1c)
    }

    private fun littleEndianU16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun crc16(bytes: ByteArray, length: Int): Int {
        var crc = 0xffff
        for (index in 0 until length) {
            crc = crc xor ((bytes[index].toInt() and 0xff) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xffff
                } else {
                    (crc shl 1) and 0xffff
                }
            }
        }
        return crc
    }

    private fun findCatalogEntry(id: Int, variant: Int): NativeFigureMetadata? =
        SkylanderConfig.getSkylanderCatalog().asSequence().mapNotNull { packed ->
            val parts = packed.split('|', limit = 6)
            if (parts.size != 6) return@mapNotNull null
            NativeFigureMetadata(
                id = parts[0].toIntOrNull() ?: return@mapNotNull null,
                variant = parts[1].toIntOrNull() ?: return@mapNotNull null,
                generation = parts[3].toIntOrNull() ?: return@mapNotNull null,
                type = parts[5].toIntOrNull() ?: return@mapNotNull null
            )
        }.firstOrNull { it.id == id && it.variant == variant }

    private fun isCompatibleWithRunningGame(
        figure: NativeFigureMetadata,
        gameId: String,
        gameTitle: String
    ): Boolean {
        val gameGeneration = runningGameGeneration(gameId, gameTitle) ?: return true
        if (figure.generation > gameGeneration) return false
        return when (gameGeneration) {
            0, 1, 2 -> figure.type in 1..6
            3 -> figure.type in 1..6 || figure.type == 9
            else -> figure.type in 1..9
        }
    }

    private fun runningGameGeneration(gameId: String, gameTitle: String): Int? {
        val normalizedId = gameId.uppercase()
        return when {
            normalizedId.startsWith("SSP") -> 0
            normalizedId.startsWith("SKY") -> 1
            normalizedId.startsWith("SVX") -> 2
            normalizedId.startsWith("SK8") -> 3
            normalizedId.startsWith("SKN") || normalizedId.startsWith("BS5") -> 4
            normalizedId.startsWith("BL6") -> 5
            !gameTitle.contains("skylander", ignoreCase = true) -> null
            gameTitle.contains("spyro", ignoreCase = true) -> 0
            gameTitle.contains("giant", ignoreCase = true) -> 1
            gameTitle.contains("swap", ignoreCase = true) -> 2
            gameTitle.contains("trap", ignoreCase = true) -> 3
            gameTitle.contains("supercharger", ignoreCase = true) -> 4
            gameTitle.contains("imaginator", ignoreCase = true) -> 5
            else -> null
        }
    }

    private fun emulationStateName(state: Int): String = when (state) {
        0 -> "NONE"
        1 -> "PAUSED"
        2 -> "RUNNING"
        3 -> "STOPPING"
        4 -> "STARTING"
        else -> "UNKNOWN"
    }

    private fun <T> onMainThread(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()

        val result = AtomicReference<T>()
        val error = AtomicReference<Throwable?>()
        val latch = CountDownLatch(1)
        val executionState = AtomicInteger(TASK_QUEUED)
        val task = Runnable {
            if (!executionState.compareAndSet(TASK_QUEUED, TASK_RUNNING)) {
                latch.countDown()
                return@Runnable
            }
            try {
                result.set(block())
            } catch (t: Throwable) {
                error.set(t)
            } finally {
                latch.countDown()
            }
        }
        check(mainHandler.post(task)) { "SkyPortal Dolphin main thread rejected the IPC task" }

        if (!latch.await(MAIN_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            if (!executionState.compareAndSet(TASK_QUEUED, TASK_CANCELLED)) {
                if (!latch.await(RUNNING_TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    Log.e(TAG, "SkyPortal IPC exceeded the hard native execution limit")
                    throw IllegalStateException("SkyPortal Dolphin IPC result is uncertain after timeout")
                }
            } else {
                mainHandler.removeCallbacks(task)
                throw IllegalStateException("SkyPortal Dolphin IPC timed out before execution")
            }
        }
        error.get()?.let { throw it }
        return result.get()
    }

    companion object {
        private const val TAG = "SkyPortalService"
        const val API_VERSION = 4
        const val LOGICAL_SLOT_COUNT = 8
        const val ERROR_OPEN_FAILED = -2
        const val ERROR_BAD_SLOT = -3
        const val ERROR_URI_ACCESS = -4
        const val ERROR_SKY_DATA = -5
        const val ERROR_PORTAL_FULL = -6
        const val ERROR_PORTAL_TOGGLE = -7
        const val ERROR_UNKNOWN_FIGURE = -8
        const val ERROR_INCOMPATIBLE_FIGURE = -9
        const val ERROR_DOLPHIN_NOT_READY = -10
        const val ERROR_UNIDENTIFIED_NATIVE_MOUNT = -11
        const val ERROR_PORTAL_USB_NOT_READY = -12
        const val ERROR_CONFLICTING_USB_DEVICE = -13
        const val PORTAL_TOGGLE_OK = 0
        const val EMULATION_UNINITIALIZED = 0
        const val EMULATION_PAUSED = 1
        const val EMULATION_RUNNING = 2
        const val MAX_PORTAL_SLOTS = 16
        const val NATIVE_NO_SLOT = 255
        const val MAIN_THREAD_TIMEOUT_SECONDS = 5L
        const val RUNNING_TASK_TIMEOUT_SECONDS = 25L
        private const val NATIVE_SLOT_SCHEMA_VERSION = 2
        private const val PORTAL_USB_PRESENT = 1 shl 0
        private const val PORTAL_USB_ATTACHED = 1 shl 1
        private const val PORTAL_USB_HANDSHAKE_SEEN = 1 shl 2
        private const val PORTAL_USB_READY_MASK = 0b111
        private const val CONFLICT_DISNEY_INFINITY_BASE = "DISNEY_INFINITY_BASE"
        private const val SKY_DUMP_SIZE_BYTES = 1_024L
        private const val MAX_EMPTY_READS = 3
        private const val NATIVE_SNAPSHOT_STRIDE = 5
        private const val NATIVE_SLOT_OFFSET = 0
        private const val NATIVE_MOUNTED_OFFSET = 1
        private const val NATIVE_STATUS_OFFSET = 2
        private const val NATIVE_FIGURE_ID_OFFSET = 3
        private const val NATIVE_VARIANT_OFFSET = 4
        private const val NATIVE_STATUS_REMOVED = 0
        private const val NATIVE_STATUS_ADDED = 3
        private const val PORTAL_LED_SCHEMA_VERSION = 1
        private const val PORTAL_LED_STATE_SIZE = 12
        private const val PORTAL_LED_SCHEMA_OFFSET = 0
        private const val PORTAL_LED_ACTIVE_OFFSET = 1
        private const val PORTAL_LED_SEQUENCE_OFFSET = 2
        private const val PORTAL_LED_LEFT_OFFSET = 3
        private const val PORTAL_LED_RIGHT_OFFSET = 6
        private const val PORTAL_LED_TRAP_OFFSET = 9
        private const val TASK_QUEUED = 0
        private const val TASK_RUNNING = 1
        private const val TASK_CANCELLED = 2

        // Keep the logical mapping across Service recreation while Dolphin's process/native portal
        // remains alive. A process restart resets both this state and the native portal together.
        private val lock = Any()
        private val logicalToActual = IntArray(LOGICAL_SLOT_COUNT) { -1 }
        private val labels = arrayOfNulls<String>(LOGICAL_SLOT_COUNT)
        private val uris = arrayOfNulls<String>(LOGICAL_SLOT_COUNT)
        private val figureIds = IntArray(LOGICAL_SLOT_COUNT) { -1 }
        private val variantIds = IntArray(LOGICAL_SLOT_COUNT) { -1 }
    }

    private data class NativeFigureMetadata(
        val id: Int,
        val variant: Int,
        val generation: Int,
        val type: Int
    )

    private data class NativeSlotState(
        val slot: Int,
        val mounted: Boolean,
        val status: Int,
        val figureId: Int,
        val variantId: Int
    )
}

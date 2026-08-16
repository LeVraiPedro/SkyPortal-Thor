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
                    reconcileLogicalMappings(nativeSnapshot)
                    firstUnclaimedOccupiedNativeSlot(nativeSnapshot)?.let { nativeSlot ->
                        Log.w(TAG, "Refusing load while unclaimed native slot $nativeSlot is occupied")
                        return@synchronized ERROR_UNIDENTIFIED_NATIVE_MOUNT
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
                    for (otherLogical in 0 until LOGICAL_SLOT_COUNT) {
                        if (otherLogical != logicalSlot && logicalToActual[otherLogical] == actual) {
                            Log.w(TAG, "Dropping duplicate logical mapping for native slot $actual")
                            clearLogicalSlot(otherLogical)
                        }
                    }
                    logicalToActual[logicalSlot] = actual
                    labels[logicalSlot] = pair.second ?: displayName ?: "Unknown"
                    uris[logicalSlot] = uri
                    figureIds[logicalSlot] = identity.first
                    variantIds[logicalSlot] = identity.second
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
                    reconcileLogicalMappings(nativeSnapshot)
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
                    removal.exceptionOrNull()?.let { error ->
                        Log.e(TAG, "Failed to remove native portal slot $actual", error)
                        return@synchronized false
                    }
                    // A false native result means the slot is already empty/removing. In both cases
                    // the logical mapping is stale and can be cleared safely.
                    clearLogicalSlot(logicalSlot)
                    true
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
                    reconcileLogicalMappings(SkylanderConfig.getPortalSnapshot())
                    var firstFailure: Throwable? = null
                    for (logical in 0 until LOGICAL_SLOT_COUNT) {
                        val actual = logicalToActual[logical]
                        if (actual < 0) {
                            clearLogicalSlot(logical)
                            continue
                        }
                        val removal = runCatching { SkylanderConfig.removeSkylander(actual) }
                        removal.onSuccess {
                            clearLogicalSlot(logical)
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to clear native portal slot $actual", error)
                            if (firstFailure == null) firstFailure = error
                        }
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

        override fun getStatusJson(): String = onMainThread { synchronized(lock) {
            if (!nativeRuntimeReady()) return@synchronized uninitializedStatusJson()
            val nativeSnapshot = SkylanderConfig.getPortalSnapshot()
            reconcileLogicalMappings(nativeSnapshot)
            val nativeSlots = JSONArray()
            for (offset in nativeSnapshot.indices step 4) {
                val slot = nativeSnapshot[offset]
                val status = nativeSnapshot[offset + 1]
                nativeSlots.put(
                    JSONObject()
                        .put("slot", slot)
                        .put("occupied", status != 0)
                        .put("status", status)
                        .put("id", nativeSnapshot[offset + 2])
                        .put("variant", nativeSnapshot[offset + 3])
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
            JSONObject()
                .put("apiVersion", API_VERSION)
                .put("slots", slots)
                .put("nativeSlots", nativeSlots)
                .put("emulationState", emulationStateName(emulationState))
                .put("gameId", if (metadataValid) NativeLibrary.GetCurrentGameID() else "")
                .put("gameTitle", if (metadataValid) NativeLibrary.GetCurrentTitleDescription() else "")
                .put("portalEnabled", BooleanSetting.MAIN_EMULATE_SKYLANDER_PORTAL.boolean)
                .put("portalActivated", SkylanderConfig.isPortalActivated())
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
            .put("slots", slots)
            .put("nativeSlots", nativeSlots)
            .put("emulationState", "NONE")
            .put("gameId", "")
            .put("gameTitle", "")
            .put("portalEnabled", JSONObject.NULL)
            .put("portalActivated", JSONObject.NULL)
            .put("canSetPortalEnabled", false)
            .put("serviceState", "INITIALIZING")
            .toString()
    }

    private fun emptyCatalogJson(): String =
        JSONObject().put("version", 1).put("figures", JSONArray()).toString()

    private fun clearLogicalSlot(logical: Int) {
        logicalToActual[logical] = -1
        labels[logical] = null
        uris[logical] = null
        figureIds[logical] = -1
        variantIds[logical] = -1
    }

    /**
     * The Android Skylanders Manager can mutate native slots while the companion is connected.
     * Never use a cached native slot for a destructive operation until its identity is confirmed.
     */
    private fun reconcileLogicalMappings(nativeSnapshot: IntArray) {
        val claimedNativeSlots = mutableSetOf<Int>()
        for (logical in 0 until LOGICAL_SLOT_COUNT) {
            val actual = logicalToActual[logical]
            if (actual < 0) continue
            val nativeOccupied = actual in 0 until MAX_PORTAL_SLOTS &&
                nativeSnapshot.getOrElse(actual * 4 + 1) { 0 } != 0
            val nativeId = if (nativeOccupied) nativeSnapshot.getOrElse(actual * 4 + 2) { -1 } else -1
            val nativeVariant = if (nativeOccupied) nativeSnapshot.getOrElse(actual * 4 + 3) { -1 } else -1
            val identityMatches = figureIds[logical] >= 0 &&
                figureIds[logical] == nativeId && variantIds[logical] == nativeVariant
            val uniqueMapping = claimedNativeSlots.add(actual)
            if (!nativeOccupied || !identityMatches || !uniqueMapping) {
                Log.w(TAG, "Dropping stale logical portal mapping $logical -> $actual")
                clearLogicalSlot(logical)
            }
        }
    }

    private fun firstUnclaimedOccupiedNativeSlot(nativeSnapshot: IntArray): Int? {
        val claimed = logicalToActual.filter { it in 0 until MAX_PORTAL_SLOTS }.toSet()
        return (0 until MAX_PORTAL_SLOTS).firstOrNull { slot ->
            nativeSnapshot.getOrElse(slot * 4 + 1) { 0 } != 0 && slot !in claimed
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
        const val API_VERSION = 3
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
        const val PORTAL_TOGGLE_OK = 0
        const val EMULATION_UNINITIALIZED = 0
        const val MAX_PORTAL_SLOTS = 16
        const val NATIVE_NO_SLOT = 255
        const val MAIN_THREAD_TIMEOUT_SECONDS = 5L
        const val RUNNING_TASK_TIMEOUT_SECONDS = 25L
        private const val SKY_DUMP_SIZE_BYTES = 1_024L
        private const val MAX_EMPTY_READS = 3
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
}

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
import org.dolphinemu.dolphinemu.features.skylanders.SkylanderConfig
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
                Log.e(TAG, "Cannot open shared .sky URI", uriAccess.exceptionOrNull())
                return ERROR_URI_ACCESS
            }

            return onMainThread {
                synchronized(lock) {
                    val previousActual = logicalToActual[logicalSlot]
                    val loadAttempt = runCatching {
                        SkylanderConfig.loadSkylander(previousActual, uri)
                    }
                    val pair = loadAttempt.getOrElse { error ->
                        Log.e(TAG, "SkylanderConfig rejected the .sky data", error)
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
                    logicalToActual[logicalSlot] = actual
                    labels[logicalSlot] = pair.second ?: displayName ?: "Unknown"
                    uris[logicalSlot] = uri
                    actual
                }
            }
        }

        override fun remove(logicalSlot: Int): Boolean {
            if (logicalSlot !in 0 until LOGICAL_SLOT_COUNT) return false
            return onMainThread {
                synchronized(lock) {
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
            onMainThread {
                synchronized(lock) {
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

        override fun getStatusJson(): String = synchronized(lock) {
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
            JSONObject()
                .put("apiVersion", API_VERSION)
                .put("slots", slots)
                .toString()
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun clearLogicalSlot(logical: Int) {
        logicalToActual[logical] = -1
        labels[logical] = null
        uris[logical] = null
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
                // Once native execution has started, wait for its real result. Reporting a timeout
                // here would let a load/remove happen later after the companion already showed an
                // error (a confusing "ghost" portal action).
                latch.await()
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
        const val API_VERSION = 2
        const val LOGICAL_SLOT_COUNT = 8
        const val ERROR_OPEN_FAILED = -2
        const val ERROR_BAD_SLOT = -3
        const val ERROR_URI_ACCESS = -4
        const val ERROR_SKY_DATA = -5
        const val ERROR_PORTAL_FULL = -6
        const val MAX_PORTAL_SLOTS = 16
        const val NATIVE_NO_SLOT = 255
        const val MAIN_THREAD_TIMEOUT_SECONDS = 5L
        private const val SKY_DUMP_SIZE_BYTES = 1_024L
        private const val TASK_QUEUED = 0
        private const val TASK_RUNNING = 1
        private const val TASK_CANCELLED = 2

        // Keep the logical mapping across Service recreation while Dolphin's process/native portal
        // remains alive. A process restart resets both this state and the native portal together.
        private val lock = Any()
        private val logicalToActual = IntArray(LOGICAL_SLOT_COUNT) { -1 }
        private val labels = arrayOfNulls<String>(LOGICAL_SLOT_COUNT)
        private val uris = arrayOfNulls<String>(LOGICAL_SLOT_COUNT)
    }
}

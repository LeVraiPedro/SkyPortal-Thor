// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led.bifrost

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.skyportalthor.app.portal.led.LedOutputFrame
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Public broadcast API only. A receiver acknowledgement is NOT a hardware acknowledgement. */
internal class AndroidBifrostTransport(context: Context) : BifrostTransport {
    private val app = context.applicationContext

    @Suppress("DEPRECATION")
    override fun availability(): BifrostAvailability {
        if (Build.VERSION.SDK_INT < 33) return BifrostAvailability.UNAVAILABLE
        return try {
            val info = app.packageManager.getPackageInfo(PACKAGE, 0)
            // The renewable lease is an implementation detail audited in 1.3.1, not API 1.
            // Do not send an indefinite override to unaudited versions.
            if (info.longVersionCode != 16L || info.versionName != "1.3.1") {
                BifrostAvailability.UNSUPPORTED_VERSION
            } else {
                val receiver = app.packageManager.getReceiverInfo(ComponentName(PACKAGE, RECEIVER), 0)
                if (receiver.enabled && receiver.exported && receiver.applicationInfo.enabled) {
                    BifrostAvailability.AVAILABLE
                } else BifrostAvailability.UNAVAILABLE
            }
        } catch (_: PackageManager.NameNotFoundException) {
            BifrostAvailability.NOT_INSTALLED
        } catch (_: SecurityException) {
            BifrostAvailability.UNAVAILABLE
        }
    }

    override suspend fun display(frame: LedOutputFrame): BifrostReply = send("ACTION_DISPLAY") {
        putExtra("effect", "STATIC")
        putExtra("color", frame.left.toArgb())
        putExtra("colorRight", frame.right.toArgb())
        putExtra("intensity", frame.intensity)
        putExtra("priority", 20)
        putExtra("until", "EXPLICIT_CLEAR")
    }

    override suspend fun clear(): BifrostReply = send("ACTION_CLEAR")

    private suspend fun send(action: String, fill: Intent.() -> Unit = {}): BifrostReply =
        withContext(Dispatchers.Main.immediate) {
            withTimeoutOrNull(1_000L) {
                suspendCancellableCoroutine { continuation ->
                    val request = UUID.randomUUID().toString()
                    val intent = Intent("$PACKAGE.api.$action").apply {
                        component = ComponentName(PACKAGE, RECEIVER)
                        putExtra("apiVersion", 1)
                        putExtra("requestId", request)
                        fill()
                    }
                    val reply = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            if (continuation.isActive) {
                                continuation.resume(mapBifrostReply(resultCode, resultData, request))
                            }
                        }
                    }
                    try {
                        app.sendOrderedBroadcast(intent, PERMISSION, reply, null, BIFROST_NO_RESULT, null, null)
                    } catch (_: SecurityException) {
                        if (continuation.isActive) continuation.resume(BifrostReply.UNAUTHORIZED)
                    } catch (_: RuntimeException) {
                        if (continuation.isActive) continuation.resume(BifrostReply.NO_RESPONSE)
                    }
                }
            } ?: BifrostReply.NO_RESPONSE
        }

    companion object {
        const val PACKAGE = "com.moonbench.bifrost"
        private const val RECEIVER = "$PACKAGE.external.ExternalApiReceiver"
        private const val PERMISSION = "$PACKAGE.permission.CONTROL_LEDS"
    }
}

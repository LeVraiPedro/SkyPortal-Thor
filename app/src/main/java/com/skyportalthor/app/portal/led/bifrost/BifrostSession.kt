// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led.bifrost

import com.skyportalthor.app.portal.led.LedOutputFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class BifrostAvailability {
    AVAILABLE,
    NOT_INSTALLED,
    UNSUPPORTED_VERSION,
    UNAVAILABLE
}

enum class BifrostReply {
    ACCEPTED_UNCONFIRMED,
    CONTROL_DISABLED,
    VERSION_REJECTED,
    INVALID_COMMAND,
    UNAUTHORIZED,
    RATE_LIMITED,
    NO_RESPONSE
}

/** The adapter must bound both suspended calls, including during cancellation cleanup. */
interface BifrostTransport {
    fun availability(): BifrostAvailability
    suspend fun display(frame: LedOutputFrame): BifrostReply
    suspend fun clear(): BifrostReply
}

/** Never initialise an ordered broadcast with an API success or rejection code. */
const val BIFROST_NO_RESULT: Int = Int.MIN_VALUE

/** API acceptance is only receiver acknowledgement, never proof of physical LED output. */
fun mapBifrostReply(code: Int, echo: String?, expected: String): BifrostReply = when (code) {
    0 -> if (expected.isNotBlank() && expected.length <= 64 && echo == expected) {
        BifrostReply.ACCEPTED_UNCONFIRMED
    } else {
        BifrostReply.NO_RESPONSE
    }
    -1 -> BifrostReply.CONTROL_DISABLED
    -2 -> BifrostReply.VERSION_REJECTED
    -3 -> BifrostReply.INVALID_COMMAND
    -4 -> BifrostReply.UNAUTHORIZED
    -5 -> BifrostReply.RATE_LIMITED
    -6 -> BifrostReply.INVALID_COMMAND
    else -> BifrostReply.NO_RESPONSE
}

enum class BifrostSessionState {
    IDLE,
    ACCEPTED_UNCONFIRMED,
    NOT_INSTALLED,
    UNSUPPORTED_VERSION,
    UNAVAILABLE,
    CONTROL_DISABLED,
    INVALID_COMMAND,
    UNAUTHORIZED,
    RATE_LIMITED,
    NO_RESPONSE,
    RELEASED_UNCONFIRMED
}

data class BifrostSessionStatus(val state: BifrostSessionState, val message: String)

/**
 * One visible SkyPortal session owns a short-lived Bifrost override.
 *
 * A monotonic clock is supplied by the caller. Every eligible tick sends the latest
 * frame, even unchanged, to renew Bifrost 1.3.1's lease; missed ticks are never replayed.
 * No coroutine, Context, hardware state or user preset is owned by this class.
 */
class BifrostSession(private val transport: BifrostTransport) {
    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow(
        BifrostSessionStatus(BifrostSessionState.IDLE, "Synchronisation Bifrost en veille.")
    )
    val status: StateFlow<BifrostSessionStatus> = mutableStatus.asStateFlow()

    private var lastObservedAtMs = 0L
    private var lastDisplayAtMs: Long? = null
    private var lastRejectedAtMs: Long? = null
    private var clearNeeded = false

    suspend fun tick(frame: LedOutputFrame?, nowMs: Long) {
        require(nowMs >= 0L) { "nowMs must not be negative" }
        mutex.withLock {
            // A regressing test/device clock must not accidentally permit a burst.
            val now = maxOf(nowMs, lastObservedAtMs)
            lastObservedAtMs = now
            if (frame == null) {
                clearLocked()
                return@withLock
            }

            val available = try {
                transport.availability()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                BifrostAvailability.UNAVAILABLE
            }
            if (available != BifrostAvailability.AVAILABLE) {
                // Best effort relinquishment, once, if a previous call may have landed.
                clearLocked()
                mutableStatus.value = when (available) {
                    BifrostAvailability.NOT_INSTALLED -> BifrostSessionStatus(
                        BifrostSessionState.NOT_INSTALLED, "Bifrost n’est pas installé."
                    )
                    BifrostAvailability.UNSUPPORTED_VERSION -> BifrostSessionStatus(
                        BifrostSessionState.UNSUPPORTED_VERSION,
                        "Cette version de Bifrost n’est pas prise en charge."
                    )
                    else -> BifrostSessionStatus(
                        BifrostSessionState.UNAVAILABLE, "Bifrost est indisponible."
                    )
                }
                return@withLock
            }
            if (lastRejectedAtMs?.let { now - it < REJECTION_BACKOFF_MS } == true) {
                return@withLock
            }
            if (lastDisplayAtMs?.let { now - it < HEARTBEAT_INTERVAL_MS } == true) {
                return@withLock
            }

            lastDisplayAtMs = now
            // Set before dispatch: a timeout or cancellation does not undo a broadcast.
            clearNeeded = true
            val reply = safely { transport.display(frame) }
            lastRejectedAtMs = if (reply == BifrostReply.ACCEPTED_UNCONFIRMED) null else now
            mutableStatus.value = statusFor(reply, clearing = false)
        }
    }

    /** Safe to call in finally after cancellation; the adapter still enforces its timeout. */
    suspend fun release() = withContext(NonCancellable) {
        mutex.withLock { clearLocked() }
    }

    private suspend fun clearLocked() {
        if (!clearNeeded) return
        // At most one CLEAR per potentially applied DISPLAY, including failed cleanup.
        clearNeeded = false
        val reply = safely { transport.clear() }
        mutableStatus.value = statusFor(reply, clearing = true)
    }

    private suspend fun safely(action: suspend () -> BifrostReply): BifrostReply = try {
        action()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        BifrostReply.NO_RESPONSE
    }

    private fun statusFor(reply: BifrostReply, clearing: Boolean): BifrostSessionStatus = when (reply) {
        BifrostReply.ACCEPTED_UNCONFIRMED -> if (clearing) {
            BifrostSessionStatus(
                BifrostSessionState.RELEASED_UNCONFIRMED,
                "Restitution demandée à Bifrost ; éclairage non confirmé."
            )
        } else {
            BifrostSessionStatus(
                BifrostSessionState.ACCEPTED_UNCONFIRMED,
                "Commandes acceptées par Bifrost ; éclairage non confirmé."
            )
        }
        BifrostReply.CONTROL_DISABLED -> BifrostSessionStatus(
            BifrostSessionState.CONTROL_DISABLED,
            "Autorisez le contrôle LED par les applications tierces dans Bifrost."
        )
        BifrostReply.VERSION_REJECTED -> BifrostSessionStatus(
            BifrostSessionState.UNSUPPORTED_VERSION, "L’API de Bifrost n’est pas compatible."
        )
        BifrostReply.INVALID_COMMAND -> BifrostSessionStatus(
            BifrostSessionState.INVALID_COMMAND, "Bifrost a refusé la commande LED."
        )
        BifrostReply.UNAUTHORIZED -> BifrostSessionStatus(
            BifrostSessionState.UNAUTHORIZED, "L’autorisation de contrôle Bifrost est indisponible."
        )
        BifrostReply.RATE_LIMITED -> BifrostSessionStatus(
            BifrostSessionState.RATE_LIMITED, "Bifrost reçoit trop de commandes ; nouvelle tentative différée."
        )
        BifrostReply.NO_RESPONSE -> BifrostSessionStatus(
            BifrostSessionState.NO_RESPONSE, "Bifrost n’a pas confirmé la réception de la commande."
        )
    }

    companion object {
        const val HEARTBEAT_INTERVAL_MS = 500L
        const val REJECTION_BACKOFF_MS = 5_000L
    }
}

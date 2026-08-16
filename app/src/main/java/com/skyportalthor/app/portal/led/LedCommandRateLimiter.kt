// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.portal.led

sealed interface LedEmissionDecision {
    object Emit : LedEmissionDecision
    object Unchanged : LedEmissionDecision

    data class Deferred(
        val retryAfterMillis: Long
    ) : LedEmissionDecision
}

class LedCommandRateLimiter(
    val minIntervalMillis: Long = DEFAULT_MIN_INTERVAL_MILLIS
) {
    init {
        require(minIntervalMillis > 0L) { "minIntervalMillis must be positive" }
    }

    private var lastFrame: LedOutputFrame? = null
    private var lastEmissionAtMillis: Long? = null

    @Synchronized
    fun evaluate(
        frame: LedOutputFrame,
        nowMillis: Long,
        allowRepeat: Boolean = false
    ): LedEmissionDecision {
        require(nowMillis >= 0L) { "nowMillis must not be negative" }

        if (!allowRepeat && lastFrame == frame) {
            return LedEmissionDecision.Unchanged
        }

        val lastEmission = lastEmissionAtMillis ?: return LedEmissionDecision.Emit
        val elapsed = nowMillis - lastEmission
        if (elapsed < 0L || elapsed >= minIntervalMillis) {
            return LedEmissionDecision.Emit
        }

        return LedEmissionDecision.Deferred(
            retryAfterMillis = minIntervalMillis - elapsed
        )
    }

    @Synchronized
    fun markEmitted(frame: LedOutputFrame, nowMillis: Long) {
        require(nowMillis >= 0L) { "nowMillis must not be negative" }
        lastFrame = frame
        lastEmissionAtMillis = nowMillis
    }

    @Synchronized
    fun reset() {
        lastFrame = null
        lastEmissionAtMillis = null
    }

    companion object {
        const val DEFAULT_MIN_INTERVAL_MILLIS = 250L
    }
}

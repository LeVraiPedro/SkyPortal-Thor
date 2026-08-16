package com.skyportalthor.app.dolphin

import java.util.concurrent.atomic.AtomicBoolean

internal class BridgeLifecycleGate {
    private val closed = AtomicBoolean(false)

    fun beginClose(): Boolean = closed.compareAndSet(false, true)

    fun allowsMutation(): Boolean = !closed.get()
}

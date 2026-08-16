package com.skyportalthor.app.dolphin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeLifecycleGateTest {
    @Test
    fun closeIsAtomicPermanentAndStopsFutureMutations() {
        val gate = BridgeLifecycleGate()

        assertTrue(gate.allowsMutation())
        assertTrue(gate.beginClose())
        assertFalse(gate.allowsMutation())
        assertFalse(gate.beginClose())
        assertFalse(gate.allowsMutation())
    }
}

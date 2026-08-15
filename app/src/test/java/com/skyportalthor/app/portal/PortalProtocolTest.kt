package com.skyportalthor.app.portal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalProtocolTest {
    @Test
    fun acceptsOnlyRealNativeSlots() {
        assertTrue(PortalProtocol.isValidActualSlot(0))
        assertTrue(PortalProtocol.isValidActualSlot(15))
        assertFalse(PortalProtocol.isValidActualSlot(-1))
        assertFalse(PortalProtocol.isValidActualSlot(16))
        assertFalse(PortalProtocol.isValidActualSlot(255))
    }

    @Test
    fun recognizesLegacyAndV3PortalFullSignals() {
        assertTrue(PortalProtocol.isPortalFull(255))
        assertTrue(PortalProtocol.isPortalFull(-6))
        assertFalse(PortalProtocol.isPortalFull(-2))
    }
}

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
        assertFalse(PortalProtocol.isPortalFull(PortalProtocol.ERROR_UNIDENTIFIED_NATIVE_MOUNT))
    }

    @Test
    fun requiresARefreshedLogicalSlotForEveryApiAndNativeIdentityForApi3() {
        assertFalse(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 2,
                refreshSucceeded = false,
                expectedActualSlot = 3,
                logicalActualSlot = 3,
                nativeOccupied = null,
                expectedFigureId = 10,
                expectedVariantId = 20,
                nativeFigureId = null,
                nativeVariantId = null
            )
        )
        assertTrue(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 2,
                refreshSucceeded = true,
                expectedActualSlot = 3,
                logicalActualSlot = 3,
                nativeOccupied = null,
                expectedFigureId = 10,
                expectedVariantId = 20,
                nativeFigureId = null,
                nativeVariantId = null
            )
        )
        assertFalse(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 3,
                refreshSucceeded = true,
                expectedActualSlot = 3,
                logicalActualSlot = 3,
                nativeOccupied = true,
                expectedFigureId = 10,
                expectedVariantId = 20,
                nativeFigureId = 10,
                nativeVariantId = 21
            )
        )
        assertTrue(
            PortalProtocol.isConfirmedLoad(
                apiVersion = 3,
                refreshSucceeded = true,
                expectedActualSlot = 3,
                logicalActualSlot = 3,
                nativeOccupied = true,
                expectedFigureId = 10,
                expectedVariantId = 20,
                nativeFigureId = 10,
                nativeVariantId = 20
            )
        )
    }
}

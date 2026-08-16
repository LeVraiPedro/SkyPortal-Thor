package com.skyportalthor.app.portal

internal object PortalProtocol {
    const val MAX_PORTAL_SLOTS = 16
    const val NATIVE_NO_SLOT = 255
    const val ERROR_PORTAL_FULL = -6
    const val ERROR_DOLPHIN_NOT_READY = -10
    const val ERROR_UNIDENTIFIED_NATIVE_MOUNT = -11

    fun isValidActualSlot(value: Int): Boolean = value in 0 until MAX_PORTAL_SLOTS

    fun isPortalFull(value: Int): Boolean = value == NATIVE_NO_SLOT || value == ERROR_PORTAL_FULL

    fun isConfirmedLoad(
        apiVersion: Int,
        refreshSucceeded: Boolean,
        expectedActualSlot: Int,
        logicalActualSlot: Int?,
        nativeOccupied: Boolean?,
        expectedFigureId: Int,
        expectedVariantId: Int,
        nativeFigureId: Int?,
        nativeVariantId: Int?
    ): Boolean {
        if (!refreshSucceeded || logicalActualSlot != expectedActualSlot) return false
        if (apiVersion < 3) return true
        return nativeOccupied == true &&
            nativeFigureId == expectedFigureId &&
            nativeVariantId == expectedVariantId
    }
}

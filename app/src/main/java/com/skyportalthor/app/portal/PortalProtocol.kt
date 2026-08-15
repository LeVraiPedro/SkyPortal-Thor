package com.skyportalthor.app.portal

internal object PortalProtocol {
    const val MAX_PORTAL_SLOTS = 16
    const val NATIVE_NO_SLOT = 255
    const val ERROR_PORTAL_FULL = -6

    fun isValidActualSlot(value: Int): Boolean = value in 0 until MAX_PORTAL_SLOTS

    fun isPortalFull(value: Int): Boolean = value == NATIVE_NO_SLOT || value == ERROR_PORTAL_FULL
}

package com.skyportalthor.app.portal

import com.skyportalthor.app.data.Skylander
import kotlinx.coroutines.flow.StateFlow

data class PortalSlotState(
    val logicalSlot: Int,
    val actualPortalSlot: Int = -1,
    val figure: Skylander? = null,
    val label: String? = null,
    val sourceUri: String? = null
)

data class PortalState(
    val connected: Boolean = false,
    val apiVersion: Int? = null,
    val message: String = "Dolphin non connecté",
    val connectedPackage: String? = null,
    val availablePackages: List<String> = emptyList(),
    val slots: List<PortalSlotState> = List(8) { PortalSlotState(it) }
)

sealed class PortalResult {
    data class Success(
        val actualPortalSlot: Int = -1,
        val message: String? = null
    ) : PortalResult()

    data class Error(
        val message: String,
        val diagnosticCode: String = "UNKNOWN",
        val technicalDetails: String? = null,
        val recoveryHint: String? = null
    ) : PortalResult()
}

interface PortalBridge {
    val state: StateFlow<PortalState>
    suspend fun connect(preferredPackage: String? = null): Boolean
    suspend fun refresh()
    suspend fun load(logicalSlot: Int, skylander: Skylander): PortalResult
    suspend fun remove(logicalSlot: Int): PortalResult
    suspend fun clear(): PortalResult
    fun close()
}

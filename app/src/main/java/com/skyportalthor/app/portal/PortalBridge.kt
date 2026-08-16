package com.skyportalthor.app.portal

import com.skyportalthor.app.data.Skylander
import com.skyportalthor.app.data.EmulationState
import com.skyportalthor.app.data.DolphinServiceState
import com.skyportalthor.app.data.FigureKey
import com.skyportalthor.app.data.FigureMetadata
import com.skyportalthor.app.data.SkylandersGame
import com.skyportalthor.app.data.SmartPortalReadiness
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
    val slots: List<PortalSlotState> = List(8) { PortalSlotState(it) },
    val readiness: SmartPortalReadiness = SmartPortalReadiness.DOLPHIN_ABSENT,
    val serviceState: DolphinServiceState = DolphinServiceState.UNKNOWN,
    val emulationState: EmulationState = EmulationState.NONE,
    val gameId: String? = null,
    val gameTitle: String? = null,
    val skylandersGame: SkylandersGame? = null,
    val portalEnabled: Boolean? = null,
    val portalActivated: Boolean? = null,
    val canSetPortalEnabled: Boolean = false,
    val nativeSlots: List<NativePortalSlotState> = emptyList(),
    val figureCatalog: Map<FigureKey, FigureMetadata> = emptyMap()
)

data class NativePortalSlotState(
    val slot: Int,
    val occupied: Boolean,
    val status: Int,
    val figureId: Int? = null,
    val variantId: Int? = null
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
    suspend fun setPortalEnabled(enabled: Boolean): PortalResult
    fun close()
}

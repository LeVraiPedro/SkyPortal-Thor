package com.skyportalthor.app.portal

internal enum class PortalTargetSwitchDecision {
    SWITCH_WITHOUT_CLEAR,
    CLEAR_AND_VERIFY,
    REFUSE_UNVERIFIED
}

internal object PortalTargetSwitchPolicy {
    fun decide(
        apiVersion: Int,
        nativeSlotSchemaVersion: Int,
        hasOwnedUris: Boolean,
        logicalSlots: List<PortalSlotState>,
        nativeSlots: List<NativePortalSlotState>
    ): PortalTargetSwitchDecision {
        val hasPortalContent = hasOwnedUris ||
            logicalSlots.any { PortalProtocol.isValidActualSlot(it.actualPortalSlot) || it.sourceUri != null } ||
            nativeSlots.any { it.occupied }
        val reliableApi3 = apiVersion >= 3 &&
            PortalProtocol.hasReliableNativeMountSchema(apiVersion, nativeSlotSchemaVersion)
        if (!reliableApi3) return PortalTargetSwitchDecision.REFUSE_UNVERIFIED
        if (hasPortalContent) return PortalTargetSwitchDecision.CLEAR_AND_VERIFY
        val completeEmptySnapshot = nativeSlots.size == PortalProtocol.MAX_PORTAL_SLOTS &&
            nativeSlots.none { it.occupied } &&
            logicalSlots.none { PortalProtocol.isValidActualSlot(it.actualPortalSlot) || it.sourceUri != null }
        return if (completeEmptySnapshot) {
            PortalTargetSwitchDecision.SWITCH_WITHOUT_CLEAR
        } else {
            PortalTargetSwitchDecision.REFUSE_UNVERIFIED
        }
    }
}

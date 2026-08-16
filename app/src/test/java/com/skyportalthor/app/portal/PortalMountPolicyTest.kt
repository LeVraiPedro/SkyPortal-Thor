package com.skyportalthor.app.portal

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PortalMountPolicyTest {
    @Test
    fun blocksUnknownLogicalAndUnclaimedNativeMountsButAllowsIdentifiedSlots() {
        val unknownLogical = listOf(
            PortalSlotState(0),
            PortalSlotState(1, actualPortalSlot = 4, sourceUri = null)
        )
        assertNotNull(
            PortalMountPolicy.unidentifiedMountReason(
                apiVersion = 1,
                requestedLogicalSlot = 0,
                logicalSlots = unknownLogical,
                nativeSlots = emptyList()
            )
        )

        val identified = listOf(
            PortalSlotState(0),
            PortalSlotState(1, actualPortalSlot = 4, sourceUri = "content://fixture/spyro")
        )
        assertNull(
            PortalMountPolicy.unidentifiedMountReason(
                apiVersion = 3,
                requestedLogicalSlot = 0,
                logicalSlots = identified,
                nativeSlots = listOf(NativePortalSlotState(4, true, 1, 16, 0))
            )
        )
        assertNotNull(
            PortalMountPolicy.unidentifiedMountReason(
                apiVersion = 3,
                requestedLogicalSlot = 0,
                logicalSlots = identified,
                nativeSlots = listOf(
                    NativePortalSlotState(4, true, 1, 16, 0),
                    NativePortalSlotState(7, true, 1, 112, 4614)
                )
            )
        )
    }
}

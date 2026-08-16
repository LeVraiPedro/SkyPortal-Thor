package com.skyportalthor.app.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionScanPolicyTest {
    @Test
    fun excludesBackupsAndControlledFixtures() {
        assertFalse(CollectionScanPolicy.shouldDescendInto("99_Backups"))
        assertFalse(CollectionScanPolicy.shouldDescendInto("device-backups"))
        assertFalse(CollectionScanPolicy.shouldDescendInto("test-fixtures"))
        assertFalse(CollectionScanPolicy.shouldDescendInto(".skyportal-test-fixtures"))
        assertTrue(CollectionScanPolicy.shouldDescendInto("01_Spyros_Adventure"))
    }

    @Test
    fun protectsMasterTemplatesRegardlessOfCase() {
        assertTrue(CollectionScanPolicy.isMasterTemplate("Tree_Rex_MASTER_BLANK.sky"))
        assertTrue(CollectionScanPolicy.isMasterTemplate("trap_master_blank.SKY"))
        assertFalse(CollectionScanPolicy.isMasterTemplate("Tree_Rex_Giant.sky"))
    }
}

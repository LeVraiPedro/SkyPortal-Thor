// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyDumpMetadataParserTest {
    @Test
    fun parsesValidLittleEndianIdentity() {
        val result = SkyDumpMetadataParser.parse(validDump(0x1234, 0xabcd))

        assertEquals(SkyDumpMetadataResult.Valid(0x1234, 0xabcd), result)
    }

    @Test
    fun rejectsWrongSizeAndAllZeroFile() {
        val short = SkyDumpMetadataParser.parse(ByteArray(30))
        val zero = SkyDumpMetadataParser.parse(ByteArray(SkyDumpMetadataParser.DUMP_SIZE_BYTES))

        assertEquals(SkyDumpStatus.INVALID_SIZE, (short as SkyDumpMetadataResult.Invalid).status)
        assertEquals(SkyDumpStatus.INVALID_HEADER, (zero as SkyDumpMetadataResult.Invalid).status)
    }

    @Test
    fun rejectsCorruptedIdentityChecksum() {
        val dump = validDump(112, 0x1206)
        dump[0x10] = (dump[0x10].toInt() xor 1).toByte()

        val result = SkyDumpMetadataParser.parse(dump)

        assertTrue(result is SkyDumpMetadataResult.Invalid)
        assertEquals(SkyDumpStatus.INVALID_CHECKSUM, (result as SkyDumpMetadataResult.Invalid).status)
    }

    private fun validDump(id: Int, variant: Int): ByteArray {
        val bytes = ByteArray(SkyDumpMetadataParser.DUMP_SIZE_BYTES)
        bytes[0] = 1
        bytes[1] = 2
        bytes[2] = 3
        bytes[3] = 4
        bytes[4] = (1 xor 2 xor 3 xor 4).toByte()
        bytes[5] = 0x81.toByte()
        bytes[6] = 0x01
        bytes[7] = 0x0f
        bytes[0x10] = id.toByte()
        bytes[0x11] = (id ushr 8).toByte()
        bytes[0x1c] = variant.toByte()
        bytes[0x1d] = (variant ushr 8).toByte()
        val crc = crc16(bytes, 0x1e)
        bytes[0x1e] = crc.toByte()
        bytes[0x1f] = (crc ushr 8).toByte()
        return bytes
    }

    private fun crc16(bytes: ByteArray, length: Int): Int {
        var crc = 0xffff
        for (index in 0 until length) {
            crc = crc xor ((bytes[index].toInt() and 0xff) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xffff
                } else {
                    (crc shl 1) and 0xffff
                }
            }
        }
        return crc
    }
}

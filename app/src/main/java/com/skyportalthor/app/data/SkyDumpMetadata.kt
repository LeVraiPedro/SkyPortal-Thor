// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.data

import java.io.InputStream

enum class SkyDumpStatus {
    UNKNOWN,
    VALID,
    INVALID_SIZE,
    INVALID_HEADER,
    INVALID_CHECKSUM,
    UNREADABLE
}

sealed interface SkyDumpMetadataResult {
    data class Valid(val figureId: Int, val variantId: Int) : SkyDumpMetadataResult
    data class Invalid(val status: SkyDumpStatus, val reason: String) : SkyDumpMetadataResult
}

/**
 * Read-only validation of the unencrypted MIFARE manufacturer block used by Dolphin.
 *
 * This deliberately validates only immutable metadata. Progress blocks remain Dolphin-owned and
 * are never decrypted or written by SkyPortal.
 */
object SkyDumpMetadataParser {
    const val DUMP_SIZE_BYTES = 0x400

    fun read(input: InputStream): SkyDumpMetadataResult {
        val bytes = ByteArray(DUMP_SIZE_BYTES + 1)
        var offset = 0
        var consecutiveEmptyReads = 0
        while (offset < bytes.size) {
            val count = input.read(bytes, offset, bytes.size - offset)
            if (count < 0) break
            if (count == 0) {
                // A broken DocumentsProvider must not keep a collection scan alive forever.
                if (++consecutiveEmptyReads >= MAX_EMPTY_READS) break
                continue
            }
            consecutiveEmptyReads = 0
            offset += count
        }
        return parse(bytes.copyOf(offset))
    }

    fun parse(bytes: ByteArray): SkyDumpMetadataResult {
        if (bytes.size != DUMP_SIZE_BYTES) {
            return SkyDumpMetadataResult.Invalid(
                SkyDumpStatus.INVALID_SIZE,
                "Taille invalide : ${bytes.size} octets au lieu de $DUMP_SIZE_BYTES."
            )
        }

        val expectedBcc = (bytes[0].toInt() xor bytes[1].toInt() xor
            bytes[2].toInt() xor bytes[3].toInt()) and 0xff
        val validManufacturerBlock = (bytes[4].toInt() and 0xff) == expectedBcc &&
            (bytes[5].toInt() and 0xff) == 0x81 &&
            (bytes[6].toInt() and 0xff) == 0x01 &&
            (bytes[7].toInt() and 0xff) == 0x0f
        if (!validManufacturerBlock) {
            return SkyDumpMetadataResult.Invalid(
                SkyDumpStatus.INVALID_HEADER,
                "L’en-tête NFC du dump n’est pas valide."
            )
        }

        val storedCrc = littleEndianU16(bytes, 0x1e)
        val calculatedCrc = crc16(bytes, 0, 0x1e)
        if (storedCrc != calculatedCrc) {
            return SkyDumpMetadataResult.Invalid(
                SkyDumpStatus.INVALID_CHECKSUM,
                "Le checksum d’identité du dump ne correspond pas."
            )
        }

        return SkyDumpMetadataResult.Valid(
            figureId = littleEndianU16(bytes, 0x10),
            variantId = littleEndianU16(bytes, 0x1c)
        )
    }

    private fun littleEndianU16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun crc16(bytes: ByteArray, offset: Int, length: Int): Int {
        var crc = 0xffff
        for (index in offset until offset + length) {
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

    private const val MAX_EMPTY_READS = 3
}

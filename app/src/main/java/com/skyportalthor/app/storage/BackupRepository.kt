// Copyright 2026 LeVraiPedro and SkyPortal Thor contributors
// SPDX-License-Identifier: GPL-2.0-or-later
package com.skyportalthor.app.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.skyportalthor.app.data.Skylander
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupRepository(private val context: Context) {
    suspend fun backup(rootUri: Uri, figure: Skylander): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val root = DocumentFile.fromTreeUri(context, rootUri)
                ?: error("Dossier Skylanders inaccessible")

            val backupRoot = root.findFile("99_Backups")
                ?: root.createDirectory("99_Backups")
                ?: error("Impossible de créer 99_Backups")
            val appBackup = backupRoot.findFile("SkyPortal")
                ?: backupRoot.createDirectory("SkyPortal")
                ?: error("Impossible de créer 99_Backups/SkyPortal")

            val characterDirName = safeFolderName(figure.name)
            val characterDir = appBackup.findFile(characterDirName)
                ?: appBackup.createDirectory(characterDirName)
                ?: error("Impossible de créer le dossier de backup")

            val stem = figure.fileName.removeSuffix(".sky")
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val fileName = "${stem}_backup_$stamp.sky"
            val destination = characterDir.createFile("application/octet-stream", fileName)
                ?: error("Impossible de créer le fichier de backup")

            try {
                context.contentResolver.openInputStream(figure.documentUri).use { input ->
                    requireNotNull(input) { "Impossible de lire ${figure.fileName}" }
                    context.contentResolver.openOutputStream(destination.uri, "w").use { output ->
                        requireNotNull(output) { "Impossible d'écrire le backup" }
                        val copied = input.copyTo(output)
                        check(copied == SKY_DUMP_SIZE_BYTES) {
                            "Backup incomplet : $copied octets copiés au lieu de $SKY_DUMP_SIZE_BYTES"
                        }
                    }
                }
                check(destination.length() == SKY_DUMP_SIZE_BYTES) {
                    "Backup incomplet après écriture : ${destination.length()} octets"
                }
            } catch (error: Throwable) {
                // Never leave a partial file that could later be mistaken for a valid backup.
                runCatching { destination.delete() }
                throw error
            }

            "99_Backups/SkyPortal/$characterDirName/$fileName"
        }
    }

    private fun safeFolderName(value: String): String = value
        .replace(Regex("[^A-Za-z0-9À-ÿ._ -]+"), "_")
        .trim()
        .ifBlank { "Unknown" }

    private companion object {
        const val SKY_DUMP_SIZE_BYTES = 1_024L
    }
}

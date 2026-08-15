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

            context.contentResolver.openInputStream(figure.documentUri).use { input ->
                requireNotNull(input) { "Impossible de lire ${figure.fileName}" }
                context.contentResolver.openOutputStream(destination.uri, "w").use { output ->
                    requireNotNull(output) { "Impossible d'écrire le backup" }
                    input.copyTo(output)
                }
            }

            "99_Backups/SkyPortal/$characterDirName/$fileName"
        }
    }

    private fun safeFolderName(value: String): String = value
        .replace(Regex("[^A-Za-z0-9À-ÿ._ -]+"), "_")
        .trim()
        .ifBlank { "Unknown" }
}

package com.skyportalthor.app.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.skyportalthor.app.data.Skylander
import com.skyportalthor.app.data.SkyDumpMetadataParser
import com.skyportalthor.app.data.SkyDumpMetadataResult
import com.skyportalthor.app.data.SkyDumpStatus
import com.skyportalthor.app.data.SkylanderPathParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SkylanderCollectionRepository(private val context: Context) {
    suspend fun scan(rootUri: Uri): List<Skylander> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: error("Le dossier sélectionné n'est plus accessible")
        check(root.exists() && root.isDirectory && root.canRead()) {
            "L'autorisation de lecture du dossier a été révoquée"
        }
        val results = mutableListOf<Skylander>()
        walk(root, emptyList(), results)
        results.sortedWith(
            compareBy<Skylander> { generationOrder(it.generation) }
                .thenBy { it.element }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun walk(node: DocumentFile, segments: List<String>, out: MutableList<Skylander>) {
        node.listFiles().forEach { child ->
            val childName = child.name ?: return@forEach
            if (child.isDirectory) {
                if (CollectionScanPolicy.shouldDescendInto(childName)) {
                    walk(child, segments + childName, out)
                }
            } else if (child.isFile && childName.endsWith(".sky", ignoreCase = true)) {
                val relativePath = (segments + childName).joinToString("/")
                val meta = SkylanderPathParser.parse(childName, segments)
                val dumpMetadata = readFigureMetadata(child)
                val validMetadata = dumpMetadata as? SkyDumpMetadataResult.Valid
                val invalidMetadata = dumpMetadata as? SkyDumpMetadataResult.Invalid
                out += Skylander(
                    name = meta.name,
                    element = meta.element,
                    generation = meta.generation,
                    fileName = childName,
                    documentUri = child.uri,
                    relativePath = relativePath,
                    kind = meta.kind,
                    typeLabel = meta.typeLabel,
                    figureId = validMetadata?.figureId,
                    variantId = validMetadata?.variantId,
                    generationNumber = generationOrder(meta.generation),
                    dumpStatus = invalidMetadata?.status ?: SkyDumpStatus.VALID,
                    dumpProblem = invalidMetadata?.reason,
                    isMasterTemplate = CollectionScanPolicy.isMasterTemplate(childName)
                )
            }
        }
    }

    private fun readFigureMetadata(file: DocumentFile): SkyDumpMetadataResult = runCatching {
        context.contentResolver.openInputStream(file.uri)?.use(SkyDumpMetadataParser::read)
            ?: SkyDumpMetadataResult.Invalid(
                SkyDumpStatus.UNREADABLE,
                "Le fournisseur de documents n’a pas ouvert le fichier."
            )
    }.getOrElse {
        SkyDumpMetadataResult.Invalid(
            SkyDumpStatus.UNREADABLE,
            "Le fichier n’est plus accessible."
        )
    }

    private fun generationOrder(name: String): Int = when (name) {
        "Spyro's Adventure" -> 1
        "Giants" -> 2
        "Swap Force" -> 3
        "Trap Team" -> 4
        "SuperChargers" -> 5
        "Imaginators" -> 6
        else -> 99
    }

}

internal object CollectionScanPolicy {
    private val excludedDirectories = setOf(
        "99_backups",
        "device-backups",
        "test-fixtures",
        ".skyportal-test-fixtures"
    )

    fun shouldDescendInto(directoryName: String): Boolean =
        directoryName.lowercase() !in excludedDirectories

    fun isMasterTemplate(fileName: String): Boolean =
        fileName.contains("MASTER_BLANK", ignoreCase = true)
}

package com.skyportalthor.app.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.skyportalthor.app.data.Skylander
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
                if (!childName.equals(BACKUP_DIRECTORY, ignoreCase = true)) {
                    walk(child, segments + childName, out)
                }
            } else if (child.isFile && childName.endsWith(".sky", ignoreCase = true)) {
                val relativePath = (segments + childName).joinToString("/")
                val meta = SkylanderPathParser.parse(childName, segments)
                out += Skylander(
                    name = meta.name,
                    element = meta.element,
                    generation = meta.generation,
                    fileName = childName,
                    documentUri = child.uri,
                    relativePath = relativePath,
                    kind = meta.kind,
                    typeLabel = meta.typeLabel
                )
            }
        }
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

    companion object {
        private const val BACKUP_DIRECTORY = "99_Backups"
    }
}

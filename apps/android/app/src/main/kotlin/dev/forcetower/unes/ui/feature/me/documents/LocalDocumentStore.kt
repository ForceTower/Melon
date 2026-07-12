package dev.forcetower.unes.ui.feature.me.documents

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.melon.feature.me.domain.model.AcademicDocument
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

// The offline copy kept on this device so the document opens without a
// connection. `version` bumps on every successful refresh. Mirrors iOS
// `StoredAcademicDocument`.
internal data class StoredAcademicDocument(
    val file: File,
    val version: Int,
    val savedAtMs: Long,
)

// The device-local slot each official document is kept in for offline
// access — one file per kind under `filesDir/documents`, replaced on every
// successful refresh, with a sidecar meta json carrying version + save
// stamp. Mirrors iOS `LocalDocumentStore`.
@Singleton
internal class LocalDocumentStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun load(document: AcademicDocument): StoredAcademicDocument? {
        val file = slot(document)
        if (!file.exists()) return null
        val meta = readMeta(document)
        return StoredAcademicDocument(
            file = file,
            version = meta?.optInt("version", 1) ?: 1,
            savedAtMs = meta?.optLong("savedAtMs", 0L) ?: 0L,
        )
    }

    fun save(document: AcademicDocument, bytes: ByteArray): StoredAcademicDocument {
        val file = slot(document)
        file.parentFile?.mkdirs()
        // Write-then-rename so a crash mid-write never corrupts the slot.
        val staging = File(file.parentFile, "${file.name}.tmp")
        staging.writeBytes(bytes)
        if (!staging.renameTo(file)) {
            file.writeBytes(bytes)
            staging.delete()
        }

        val version = (readMeta(document)?.optInt("version", 0) ?: 0) + 1
        val savedAtMs = System.currentTimeMillis()
        metaFile(document).writeText(
            JSONObject().put("version", version).put("savedAtMs", savedAtMs).toString(),
        )
        return StoredAcademicDocument(file = file, version = version, savedAtMs = savedAtMs)
    }

    private fun readMeta(document: AcademicDocument): JSONObject? {
        val file = metaFile(document)
        if (!file.exists()) return null
        return runCatching { JSONObject(file.readText()) }.getOrNull()
    }

    private fun slot(document: AcademicDocument): File =
        File(directory(), document.fileName)

    private fun metaFile(document: AcademicDocument): File =
        File(directory(), "${document.fileName}.meta.json")

    private fun directory(): File = File(context.filesDir, "documents")
}

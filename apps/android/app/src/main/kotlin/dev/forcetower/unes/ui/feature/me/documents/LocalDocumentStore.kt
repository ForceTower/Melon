package dev.forcetower.unes.ui.feature.me.documents

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.melon.feature.me.domain.model.AcademicDocument
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// The offline copy kept on this device so the document opens without a
// connection. `version` bumps on every successful refresh. Mirrors iOS
// `StoredAcademicDocument`.
internal data class StoredAcademicDocument(
    val file: File,
    val version: Int,
    val savedAtMs: Long,
)

@Serializable
private data class DocumentMeta(
    val version: Int,
    val savedAtMs: Long,
    val ownerId: String? = null,
)

// The device-local slot each official document is kept in for offline
// access — one file per kind under `filesDir/documents`, replaced on every
// successful refresh, with a sidecar meta json carrying version + save
// stamp + the account that saved it. Mirrors iOS `LocalDocumentStore`.
@Singleton
internal class LocalDocumentStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(document: AcademicDocument): StoredAcademicDocument? =
        withContext(Dispatchers.IO) {
            val file = slot(document)
            val owner = signedInUserId() ?: return@withContext null
            val meta = readMeta(document) ?: return@withContext null
            // Copies saved before owners were recorded have none, so they never show.
            if (!file.exists() || meta.ownerId != owner) return@withContext null
            StoredAcademicDocument(file = file, version = meta.version, savedAtMs = meta.savedAtMs)
        }

    suspend fun save(
        document: AcademicDocument,
        bytes: ByteArray,
    ): StoredAcademicDocument =
        withContext(Dispatchers.IO) {
            val file = slot(document)
            file.parentFile?.mkdirs()
            // Write-then-rename so a crash mid-write never corrupts the slot.
            val staging = File(file.parentFile, "${file.name}.tmp")
            staging.writeBytes(bytes)
            if (!staging.renameTo(file)) {
                file.writeBytes(bytes)
                staging.delete()
            }

            val meta = DocumentMeta(
                version = (readMeta(document)?.version ?: 0) + 1,
                savedAtMs = System.currentTimeMillis(),
                ownerId = signedInUserId(),
            )
            metaFile(document).writeText(json.encodeToString(meta))
            StoredAcademicDocument(file = file, version = meta.version, savedAtMs = meta.savedAtMs)
        }

    suspend fun clear() {
        withContext(Dispatchers.IO) { directory().deleteRecursively() }
    }

    private suspend fun signedInUserId(): String? =
        (sessionStore.currentAuthState() as? AuthState.Authenticated)?.user?.id

    private fun readMeta(document: AcademicDocument): DocumentMeta? {
        val file = metaFile(document)
        if (!file.exists()) return null
        return runCatching { json.decodeFromString<DocumentMeta>(file.readText()) }.getOrNull()
    }

    private fun slot(document: AcademicDocument): File = File(directory(), document.fileName)

    private fun metaFile(document: AcademicDocument): File = File(directory(), "${document.fileName}.meta.json")

    private fun directory(): File = File(context.filesDir, "documents")
}

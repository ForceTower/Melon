package dev.forcetower.melon.feature.me.data.network

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.me.domain.model.AcademicDocument
import dev.forcetower.melon.feature.me.domain.model.DocumentFetchError
import dev.forcetower.melon.feature.me.domain.model.FetchedAcademicDocument
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

// `POST api/documents/fetch` pulls the PDF fresh from the university portal
// (carrying the reCAPTCHA answer when one was demanded) and stores it
// server-side; the response is a short-lived presigned URL this client then
// downloads. Mirrors iOS `DocumentsRepository`.
@Inject
internal class DocumentsService(
    private val client: HttpClient,
    engine: HttpClientEngine,
) {
    // The presigned URL is its own bearer token — R2 rejects requests that
    // also carry an Authorization header, so the download goes through a
    // bare client without the session/machine-id interceptors.
    private val downloadClient by lazy { HttpClient(engine) { expectSuccess = false } }

    suspend fun fetch(
        document: AcademicDocument,
        captchaToken: String?,
    ): Outcome<FetchedAcademicDocument, DocumentFetchError> {
        val response = try {
            client.post("api/documents/fetch") {
                contentType(ContentType.Application.Json)
                setBody(DocumentFetchRequest(kind = document.kind, captchaToken = captchaToken))
            }
        } catch (_: Exception) {
            return Outcome.Err(DocumentFetchError.Connection)
        }
        if (response.status == HttpStatusCode.NotFound) {
            return Outcome.Err(DocumentFetchError.Unavailable)
        }
        if (!response.status.isSuccess()) {
            return Outcome.Err(DocumentFetchError.Connection)
        }
        val payload = runCatching { response.body<ApiEnvelope<DocumentFetchResponse>>() }
            .getOrNull()?.data
            ?: return Outcome.Err(DocumentFetchError.Connection)

        val bytes = try {
            val download = downloadClient.get(payload.download.url)
            if (!download.status.isSuccess()) return Outcome.Err(DocumentFetchError.Connection)
            download.readRawBytes()
        } catch (_: Exception) {
            return Outcome.Err(DocumentFetchError.Connection)
        }
        return Outcome.Ok(
            FetchedAcademicDocument(
                bytes = bytes,
                fileName = payload.document.filename,
                fresh = payload.fresh,
                generatedAtIso = payload.document.createdAt,
            ),
        )
    }
}

@Serializable
private data class DocumentFetchRequest(
    val kind: String,
    val captchaToken: String?,
)

@Serializable
internal data class DocumentFetchResponse(
    val document: Document,
    val download: Download,
    val fresh: Boolean,
) {
    @Serializable
    internal data class Document(
        val filename: String,
        val createdAt: String,
    )

    @Serializable
    internal data class Download(
        val url: String,
    )
}

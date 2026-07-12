package dev.forcetower.melon.feature.materials.data.network

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.materials.data.dto.MaterialBody
import dev.forcetower.melon.feature.materials.data.dto.MaterialDownloadBody
import dev.forcetower.melon.feature.materials.data.dto.MaterialReportRequest
import dev.forcetower.melon.feature.materials.data.dto.MaterialSavedRequest
import dev.forcetower.melon.feature.materials.data.dto.MaterialSubmitRequest
import dev.forcetower.melon.feature.materials.data.dto.MaterialUploadSlotBody
import dev.forcetower.melon.feature.materials.data.dto.MaterialUploadSlotRequest
import dev.forcetower.melon.feature.materials.data.dto.MaterialUsefulBody
import dev.forcetower.melon.feature.materials.data.dto.MaterialUsefulRequest
import dev.forcetower.melon.feature.materials.data.dto.MaterialsListBody
import dev.forcetower.melon.feature.materials.data.dto.MaterialsOverviewBody
import dev.forcetower.melon.feature.materials.data.dto.MaterialsShelfBody
import dev.forcetower.melon.feature.materials.data.dto.toDomain
import dev.forcetower.melon.feature.materials.domain.model.FetchedMaterialFile
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialDisciplineRef
import dev.forcetower.melon.feature.materials.domain.model.MaterialFileKind
import dev.forcetower.melon.feature.materials.domain.model.MaterialReportReason
import dev.forcetower.melon.feature.materials.domain.model.MaterialStatus
import dev.forcetower.melon.feature.materials.domain.model.MaterialSubmission
import dev.forcetower.melon.feature.materials.domain.model.MaterialUploader
import dev.forcetower.melon.feature.materials.domain.model.MaterialsDisciplineDetails
import dev.forcetower.melon.feature.materials.domain.model.MaterialsError
import dev.forcetower.melon.feature.materials.domain.model.MaterialsOverview
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

// `api/materials/*` — the collaborative study-materials shelf. All reads are
// live (nothing mirrors into the local DB); mutations are fire-and-confirm so
// callers can apply optimistic UI and roll back on Err. Mirrors iOS
// `MaterialsRepository+Live.swift`, which defined the contract client-first.
@Inject
internal class MaterialsService(
    private val client: HttpClient,
    engine: HttpClientEngine,
) {
    // Presigned URLs are their own bearer token — R2 rejects requests that
    // also carry an Authorization header, so uploads/downloads go through a
    // bare client without the session/machine-id interceptors.
    private val transferClient by lazy { HttpClient(engine) { expectSuccess = false } }

    suspend fun overview(): Outcome<MaterialsOverview, MaterialsError> =
        envelope<MaterialsOverviewBody> { client.get("api/materials/overview") }
            .map { body ->
                MaterialsOverview(
                    semester = body.semester,
                    disciplines = body.disciplines.map { it.toDomain() },
                    savedCount = body.savedCount ?: 0,
                )
            }

    suspend fun discipline(id: String): Outcome<MaterialsDisciplineDetails, MaterialsError> =
        envelope<MaterialsShelfBody> {
            client.get("api/materials/discipline") { parameter("id", id) }
        }.map { body ->
            MaterialsDisciplineDetails(
                discipline = body.discipline.toDomain(),
                materials = body.materials.mapNotNull { it.toDomain() },
            )
        }

    suspend fun saved(): Outcome<List<Material>, MaterialsError> =
        envelope<MaterialsListBody> { client.get("api/materials/saved") }
            .map { body -> body.materials.mapNotNull { it.toDomain() } }

    // Returns the authoritative new count.
    suspend fun setUseful(materialId: String, useful: Boolean): Outcome<Int, MaterialsError> =
        envelope<MaterialUsefulBody> {
            client.post("api/materials/useful") {
                parameter("id", materialId)
                contentType(ContentType.Application.Json)
                setBody(MaterialUsefulRequest(useful))
            }
        }.map { it.count }

    suspend fun setSaved(materialId: String, saved: Boolean): Outcome<Unit, MaterialsError> =
        confirm {
            client.post("api/materials/save") {
                parameter("id", materialId)
                contentType(ContentType.Application.Json)
                setBody(MaterialSavedRequest(saved))
            }
        }

    suspend fun report(
        materialId: String,
        reason: MaterialReportReason,
    ): Outcome<Unit, MaterialsError> = confirm {
        client.post("api/materials/report") {
            parameter("id", materialId)
            contentType(ContentType.Application.Json)
            setBody(MaterialReportRequest(reason.wire))
        }
    }

    // Two hops: the API mints a short-lived presigned URL (counting the
    // download server-side), then the bytes come straight from storage.
    suspend fun open(materialId: String): Outcome<FetchedMaterialFile, MaterialsError> {
        val download = envelope<MaterialDownloadBody> {
            client.post("api/materials/open") {
                parameter("id", materialId)
                contentType(ContentType.Application.Json)
                setBody(EmptyRequest)
            }
        }
        val payload = when (download) {
            is Outcome.Ok -> download.value
            is Outcome.Err -> return download
        }
        val bytes = try {
            val response = transferClient.get(payload.url)
            if (!response.status.isSuccess()) return Outcome.Err(MaterialsError.Connection)
            response.readRawBytes()
        } catch (_: Exception) {
            return Outcome.Err(MaterialsError.Connection)
        }
        return Outcome.Ok(FetchedMaterialFile(bytes = bytes, fileName = payload.filename))
    }

    // Three hops: request a presigned slot, PUT the raw bytes to it, then
    // register the metadata. The response is the created material, always
    // `pending` until moderation publishes it.
    suspend fun submit(submission: MaterialSubmission): Outcome<Material, MaterialsError> {
        val contentType = when (submission.fileKind) {
            MaterialFileKind.Pdf -> ContentType.Application.Pdf
            MaterialFileKind.Photo -> ContentType.Image.JPEG
        }
        val slot = envelope<MaterialUploadSlotBody> {
            client.post("api/materials/uploads") {
                contentType(ContentType.Application.Json)
                setBody(
                    MaterialUploadSlotRequest(
                        fileName = submission.fileName,
                        byteCount = submission.bytes.size,
                        contentType = contentType.toString(),
                    ),
                )
            }
        }
        val payload = when (slot) {
            is Outcome.Ok -> slot.value
            is Outcome.Err -> return slot
        }
        try {
            val upload = transferClient.put(payload.url) {
                contentType(contentType)
                setBody(submission.bytes)
            }
            if (!upload.status.isSuccess()) return Outcome.Err(MaterialsError.Connection)
        } catch (_: Exception) {
            return Outcome.Err(MaterialsError.Connection)
        }
        return envelope<MaterialBody> {
            client.post("api/materials") {
                contentType(ContentType.Application.Json)
                setBody(
                    MaterialSubmitRequest(
                        uploadId = payload.uploadId,
                        disciplineId = submission.disciplineId,
                        type = submission.type.wire,
                        title = submission.title,
                        semester = submission.semester,
                        teacherName = submission.teacherName,
                        fileKind = submission.fileKind.wire,
                        pages = submission.pages,
                    ),
                )
            }
        }.map { body ->
            body.toDomain() ?: Material(
                // Decodable-but-unknown enums on our own fresh submission
                // shouldn't happen; fall back to the request's view of it so
                // the success step still renders.
                id = body.id,
                discipline = MaterialDisciplineRef(id = submission.disciplineId, code = "", name = ""),
                type = submission.type,
                title = submission.title,
                teacherName = submission.teacherName,
                semester = submission.semester,
                pages = submission.pages,
                fileKind = submission.fileKind,
                usefulCount = 0,
                downloadCount = 0,
                uploader = MaterialUploader(course = "", entryYear = 0),
                note = null,
                isMine = true,
                status = MaterialStatus.Pending,
                rejectionReason = null,
                isUseful = false,
                isSaved = false,
            )
        }
    }

    // Unwraps the `{ ok, data }` envelope; any transport/decoding hiccup or
    // `ok=false` collapses to Connection — the screens only distinguish
    // "worked" from "retry".
    private suspend inline fun <reified T> envelope(
        request: () -> HttpResponse,
    ): Outcome<T, MaterialsError> {
        val response = try {
            request()
        } catch (_: Exception) {
            return Outcome.Err(MaterialsError.Connection)
        }
        if (!response.status.isSuccess()) return Outcome.Err(MaterialsError.Connection)
        val data = runCatching { response.body<ApiEnvelope<T>>() }
            .getOrNull()
            ?.takeIf { it.ok }
            ?.data
            ?: return Outcome.Err(MaterialsError.Connection)
        return Outcome.Ok(data)
    }

    // For mutations whose envelope carries no data payload.
    private suspend inline fun confirm(request: () -> HttpResponse): Outcome<Unit, MaterialsError> {
        val response = try {
            request()
        } catch (_: Exception) {
            return Outcome.Err(MaterialsError.Connection)
        }
        return if (response.status.isSuccess()) Outcome.Ok(Unit)
        else Outcome.Err(MaterialsError.Connection)
    }
}

private fun <T, R, E> Outcome<T, E>.map(transform: (T) -> R): Outcome<R, E> = when (this) {
    is Outcome.Ok -> Outcome.Ok(transform(value))
    is Outcome.Err -> this
}

@kotlinx.serialization.Serializable
private object EmptyRequest

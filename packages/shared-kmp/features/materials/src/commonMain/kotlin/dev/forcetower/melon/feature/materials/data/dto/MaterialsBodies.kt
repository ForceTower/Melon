package dev.forcetower.melon.feature.materials.data.dto

import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialDisciplineRef
import dev.forcetower.melon.feature.materials.domain.model.MaterialFileKind
import dev.forcetower.melon.feature.materials.domain.model.MaterialStatus
import dev.forcetower.melon.feature.materials.domain.model.MaterialType
import dev.forcetower.melon.feature.materials.domain.model.MaterialUploader
import dev.forcetower.melon.feature.materials.domain.model.MaterialsDiscipline
import kotlinx.serialization.Serializable

// Wire bodies for `api/materials/*`. Field names/optionality mirror the iOS
// DTOs in `MaterialsRepository+Live.swift` — that file defined the contract
// client-first, so both platforms must decode the same shapes.

@Serializable
internal data class MaterialsOverviewBody(
    val semester: String,
    val disciplines: List<MaterialsDisciplineBody> = emptyList(),
    val savedCount: Int? = null,
)

@Serializable
internal data class MaterialsDisciplineBody(
    val id: String,
    val code: String,
    val name: String,
    val teacherName: String? = null,
    val counts: Map<String, Int>? = null,
)

@Serializable
internal data class MaterialsShelfBody(
    val discipline: MaterialsDisciplineBody,
    val materials: List<MaterialBody> = emptyList(),
)

@Serializable
internal data class MaterialsListBody(
    val materials: List<MaterialBody> = emptyList(),
)

@Serializable
internal data class MaterialBody(
    val id: String,
    val discipline: MaterialDisciplineRefBody,
    val type: String,
    val title: String,
    val teacherName: String? = null,
    val semester: String,
    val pages: Int? = null,
    val fileKind: String,
    val usefulCount: Int? = null,
    val downloadCount: Int? = null,
    val uploader: MaterialUploaderBody,
    val note: String? = null,
    val mine: Boolean? = null,
    val status: String? = null,
    val rejectionReason: String? = null,
    val useful: Boolean? = null,
    val saved: Boolean? = null,
)

@Serializable
internal data class MaterialDisciplineRefBody(
    val id: String,
    val code: String,
    val name: String,
)

@Serializable
internal data class MaterialUploaderBody(
    val course: String,
    val entryYear: Int,
)

@Serializable
internal data class MaterialUsefulRequest(val useful: Boolean)

@Serializable
internal data class MaterialUsefulBody(val count: Int)

@Serializable
internal data class MaterialSavedRequest(val saved: Boolean)

@Serializable
internal data class MaterialReportRequest(val reason: String)

@Serializable
internal data class MaterialDownloadBody(
    val url: String,
    val filename: String,
)

@Serializable
internal data class MaterialUploadSlotRequest(
    val fileName: String,
    val byteCount: Int,
    val contentType: String,
)

@Serializable
internal data class MaterialUploadSlotBody(
    val uploadId: String,
    val url: String,
)

@Serializable
internal data class MaterialSubmitRequest(
    val uploadId: String,
    val disciplineId: String,
    val type: String,
    val title: String,
    val semester: String,
    val teacherName: String?,
    val fileKind: String,
    val pages: Int,
)

// ───────── wire → domain ─────────
// Unknown enum raw values drop the whole element (same `compactMap` posture
// as iOS) so a future server-side type never renders as garbage.

internal fun MaterialBody.toDomain(): Material? {
    val type = MaterialType.fromWire(type) ?: return null
    val fileKind = MaterialFileKind.fromWire(fileKind) ?: return null
    val status = status?.let { MaterialStatus.fromWire(it) ?: return null }
        ?: MaterialStatus.Published
    return Material(
        id = id,
        discipline = MaterialDisciplineRef(
            id = discipline.id,
            code = discipline.code,
            name = discipline.name,
        ),
        type = type,
        title = title,
        teacherName = teacherName?.takeIf { it.isNotBlank() },
        semester = semester,
        pages = pages ?: 1,
        fileKind = fileKind,
        usefulCount = usefulCount ?: 0,
        downloadCount = downloadCount ?: 0,
        uploader = MaterialUploader(course = uploader.course, entryYear = uploader.entryYear),
        note = note?.takeIf { it.isNotBlank() },
        isMine = mine ?: false,
        status = status,
        rejectionReason = rejectionReason?.takeIf { it.isNotBlank() },
        isUseful = useful ?: false,
        isSaved = saved ?: false,
    )
}

internal fun MaterialsDisciplineBody.toDomain(): MaterialsDiscipline = MaterialsDiscipline(
    id = id,
    code = code,
    name = name,
    teacherName = teacherName?.takeIf { it.isNotBlank() },
    counts = counts.orEmpty().mapNotNull { (key, value) ->
        MaterialType.fromWire(key)?.let { it to value }
    }.toMap(),
)

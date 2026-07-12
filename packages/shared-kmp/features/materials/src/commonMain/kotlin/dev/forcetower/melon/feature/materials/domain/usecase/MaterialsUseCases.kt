package dev.forcetower.melon.feature.materials.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.materials.data.network.MaterialsService
import dev.forcetower.melon.feature.materials.domain.model.FetchedMaterialFile
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialReportReason
import dev.forcetower.melon.feature.materials.domain.model.MaterialSubmission
import dev.forcetower.melon.feature.materials.domain.model.MaterialsDisciplineDetails
import dev.forcetower.melon.feature.materials.domain.model.MaterialsError
import dev.forcetower.melon.feature.materials.domain.model.MaterialsOverview
import dev.zacsweers.metro.Inject

// Materiais is online-only, so every use case is a live request — no flows,
// no mirror. Grouped in one file because each is a one-line delegation; the
// behavioral contract lives on `MaterialsService`.

// Hub payload: the student's current-semester disciplines with per-type
// tallies, plus the saved-bookmarks count.
@Inject
class GetMaterialsOverviewUseCase internal constructor(
    private val service: MaterialsService,
) {
    suspend operator fun invoke(): Outcome<MaterialsOverview, MaterialsError> = service.overview()
}

// One discipline's shelf: published acervo + the student's own
// pending/rejected submissions.
@Inject
class GetMaterialsDisciplineUseCase internal constructor(
    private val service: MaterialsService,
) {
    suspend operator fun invoke(
        disciplineId: String,
    ): Outcome<MaterialsDisciplineDetails, MaterialsError> = service.discipline(disciplineId)
}

// The server-side bookmark shelf, in server order.
@Inject
class GetSavedMaterialsUseCase internal constructor(
    private val service: MaterialsService,
) {
    suspend operator fun invoke(): Outcome<List<Material>, MaterialsError> = service.saved()
}

// "Útil" vote toggle — returns the authoritative new count so callers can
// reconcile their optimistic ±1.
@Inject
class SetMaterialUsefulUseCase internal constructor(
    private val service: MaterialsService,
) {
    suspend operator fun invoke(
        materialId: String,
        isUseful: Boolean,
    ): Outcome<Int, MaterialsError> = service.setUseful(materialId, isUseful)
}

// "Salvar" server bookmark toggle.
@Inject
class SetMaterialSavedUseCase internal constructor(
    private val service: MaterialsService,
) {
    suspend operator fun invoke(
        materialId: String,
        isSaved: Boolean,
    ): Outcome<Unit, MaterialsError> = service.setSaved(materialId, isSaved)
}

// Anonymous moderation report.
@Inject
class ReportMaterialUseCase internal constructor(
    private val service: MaterialsService,
) {
    suspend operator fun invoke(
        materialId: String,
        reason: MaterialReportReason,
    ): Outcome<Unit, MaterialsError> = service.report(materialId, reason)
}

// Downloads the file behind a material (counts as a download server-side).
// The caller writes the bytes to a temp file and hands it to the platform
// viewer.
@Inject
class OpenMaterialUseCase internal constructor(
    private val service: MaterialsService,
) {
    suspend operator fun invoke(
        materialId: String,
    ): Outcome<FetchedMaterialFile, MaterialsError> = service.open(materialId)
}

// Presigned-slot upload + metadata registration; returns the created material
// as `pending`.
@Inject
class SubmitMaterialUseCase internal constructor(
    private val service: MaterialsService,
) {
    suspend operator fun invoke(
        submission: MaterialSubmission,
    ): Outcome<Material, MaterialsError> = service.submit(submission)
}

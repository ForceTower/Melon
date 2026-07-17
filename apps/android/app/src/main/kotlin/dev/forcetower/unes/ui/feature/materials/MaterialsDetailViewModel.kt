package dev.forcetower.unes.ui.feature.materials

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialFileKind
import dev.forcetower.melon.feature.materials.domain.model.MaterialReportReason
import dev.forcetower.melon.feature.materials.domain.model.MaterialStatus
import dev.forcetower.melon.feature.materials.domain.usecase.GetMaterialsDisciplineUseCase
import dev.forcetower.melon.feature.materials.domain.usecase.OpenMaterialUseCase
import dev.forcetower.melon.feature.materials.domain.usecase.ReportMaterialUseCase
import dev.forcetower.melon.feature.materials.domain.usecase.SetMaterialSavedUseCase
import dev.forcetower.melon.feature.materials.domain.usecase.SetMaterialUsefulUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import dev.forcetower.unes.ui.feature.materials.components.MaterialsToastKind
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Drives the material detail (and its moderation-status variant for the
// student's own pending/rejected uploads). The material itself travels
// in-memory from the list tap (`Seed`); `Ensure` re-hydrates it from the
// discipline shelf when the seed is gone (process death restore). Útil/Salvar
// are optimistic with rollback — mirrors iOS `MaterialsDetailFeature`.
internal data class MaterialsDetailUiState(
    val material: Material? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val isOpening: Boolean = false,
    val isReportOpen: Boolean = false,
    val reportReason: MaterialReportReason? = null,
    val toast: MaterialsToastKind? = null,
) : UiState {
    // Own upload still in moderation → render the status screen instead of
    // the public detail.
    val showsModerationStatus: Boolean
        get() = material?.let { it.isMine && it.status != MaterialStatus.Published } == true
}

internal sealed interface MaterialsDetailIntent : UiIntent {
    // In-memory handoff fired by the list/saved row tap before navigation.
    data class Seed(val material: Material) : MaterialsDetailIntent

    // Route-driven fallback: refetch the discipline shelf and pick the
    // material by id when there's no seed (or a different one).
    data class Ensure(val materialId: String, val disciplineId: String) : MaterialsDetailIntent

    data object ToggleUseful : MaterialsDetailIntent
    data object ToggleSave : MaterialsDetailIntent
    data object OpenFile : MaterialsDetailIntent
    data object OpenReport : MaterialsDetailIntent
    data object CloseReport : MaterialsDetailIntent
    data class PickReportReason(val reason: MaterialReportReason) : MaterialsDetailIntent
    data object ConfirmReport : MaterialsDetailIntent
}

internal sealed interface MaterialsDetailEffect : UiEffect {
    // Bytes are on disk; the screen resolves the FileProvider grant and fires
    // the ACTION_VIEW intent.
    data class ViewFile(val file: File, val mimeType: String) : MaterialsDetailEffect
}

@HiltViewModel
internal class MaterialsDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getDiscipline: GetMaterialsDisciplineUseCase,
    private val setUseful: SetMaterialUsefulUseCase,
    private val setSaved: SetMaterialSavedUseCase,
    private val reportMaterial: ReportMaterialUseCase,
    private val openMaterial: OpenMaterialUseCase,
) : MviViewModel<MaterialsDetailUiState, MaterialsDetailIntent, MaterialsDetailEffect>(
    MaterialsDetailUiState(),
) {
    private var toastJob: Job? = null

    override fun onIntent(intent: MaterialsDetailIntent) {
        when (intent) {
            is MaterialsDetailIntent.Seed -> setState {
                MaterialsDetailUiState(material = intent.material)
            }
            is MaterialsDetailIntent.Ensure -> ensure(intent)
            MaterialsDetailIntent.ToggleUseful -> toggleUseful()
            MaterialsDetailIntent.ToggleSave -> toggleSave()
            MaterialsDetailIntent.OpenFile -> openFile()
            MaterialsDetailIntent.OpenReport -> setState {
                copy(isReportOpen = true, reportReason = null)
            }
            MaterialsDetailIntent.CloseReport -> setState { copy(isReportOpen = false) }
            is MaterialsDetailIntent.PickReportReason -> setState {
                copy(reportReason = intent.reason)
            }
            MaterialsDetailIntent.ConfirmReport -> confirmReport()
        }
    }

    private fun ensure(intent: MaterialsDetailIntent.Ensure) {
        if (currentState.material?.id == intent.materialId) return
        viewModelScope.launch {
            setState { MaterialsDetailUiState(isLoading = true) }
            when (val outcome = getDiscipline(intent.disciplineId)) {
                is Outcome.Ok -> {
                    val material = outcome.value.materials.firstOrNull { it.id == intent.materialId }
                    setState {
                        copy(material = material, isLoading = false, loadFailed = material == null)
                    }
                }
                is Outcome.Err -> setState { copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    private fun toggleUseful() {
        val material = currentState.material ?: return
        val wasUseful = material.isUseful
        val optimistic = material.copy(
            isUseful = !wasUseful,
            usefulCount = (material.usefulCount + if (wasUseful) -1 else 1).coerceAtLeast(0),
        )
        setState { copy(material = optimistic) }
        viewModelScope.launch {
            when (setUseful(material.id, !wasUseful)) {
                is Outcome.Ok -> Unit
                is Outcome.Err -> {
                    setState { copy(material = this.material?.copy(isUseful = wasUseful, usefulCount = material.usefulCount)) }
                    flash(MaterialsToastKind.SyncFailed)
                }
            }
        }
    }

    private fun toggleSave() {
        val material = currentState.material ?: return
        val wasSaved = material.isSaved
        setState { copy(material = material.copy(isSaved = !wasSaved)) }
        flash(if (wasSaved) MaterialsToastKind.Unsaved else MaterialsToastKind.Saved)
        viewModelScope.launch {
            when (setSaved(material.id, !wasSaved)) {
                is Outcome.Ok -> Unit
                is Outcome.Err -> {
                    setState { copy(material = this.material?.copy(isSaved = wasSaved)) }
                    flash(MaterialsToastKind.SyncFailed)
                }
            }
        }
    }

    private fun openFile() {
        val material = currentState.material ?: return
        if (currentState.isOpening) return
        setState { copy(isOpening = true) }
        viewModelScope.launch {
            when (val outcome = openMaterial(material.id)) {
                is Outcome.Ok -> {
                    val file = withContext(Dispatchers.IO) {
                        materialCacheFile(material.id, outcome.value.fileName)
                            .apply { parentFile?.mkdirs() }
                            .apply { writeBytes(outcome.value.bytes) }
                    }
                    setState {
                        copy(
                            isOpening = false,
                            material = this.material?.let { it.copy(downloadCount = it.downloadCount + 1) },
                        )
                    }
                    val mime = when (material.fileKind) {
                        MaterialFileKind.Pdf -> "application/pdf"
                        MaterialFileKind.Photo -> "image/*"
                    }
                    emitEffect(MaterialsDetailEffect.ViewFile(file, mime))
                }
                is Outcome.Err -> {
                    setState { copy(isOpening = false) }
                    flash(MaterialsToastKind.OpenFailed)
                }
            }
        }
    }

    // The name is uploader-supplied and not a trusted path. Every scan is named
    // "digitalizacao.pdf", so the id keys the directory to stop distinct
    // materials overwriting each other.
    private fun materialCacheFile(materialId: String, fileName: String): File {
        val root = File(context.cacheDir, "materials")
        val target = File(root, "${basename(materialId)}/${basename(fileName)}")
        if (!target.canonicalPath.startsWith(root.canonicalPath + File.separator)) {
            return File(root, "untrusted/material.pdf")
        }
        return target
    }

    private fun basename(value: String) = File(value).name
        .takeUnless { it.isBlank() || it == "." || it == ".." }
        ?: "material.pdf"

    private fun confirmReport() {
        val material = currentState.material ?: return
        val reason = currentState.reportReason ?: return
        setState { copy(isReportOpen = false) }
        flash(MaterialsToastKind.Reported)
        viewModelScope.launch {
            when (reportMaterial(material.id, reason)) {
                is Outcome.Ok -> Unit
                is Outcome.Err -> flash(MaterialsToastKind.SyncFailed)
            }
        }
    }

    private fun flash(kind: MaterialsToastKind) {
        toastJob?.cancel()
        setState { copy(toast = kind) }
        toastJob = viewModelScope.launch {
            delay(2200)
            setState { copy(toast = null) }
        }
    }
}

package dev.forcetower.unes.ui.feature.materials.upload

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.content.edit
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialFileKind
import dev.forcetower.melon.feature.materials.domain.model.MaterialSubmission
import dev.forcetower.melon.feature.materials.domain.model.MaterialType
import dev.forcetower.melon.feature.materials.domain.model.MaterialsDiscipline
import dev.forcetower.melon.feature.materials.domain.usecase.SubmitMaterialUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import dev.forcetower.unes.ui.feature.materials.MaterialsFormat
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// The "Contribuir" wizard (M3 bottom sheet): discipline → source → details →
// guidelines → success. Opened from the hub it starts on the discipline pick;
// opened from a discipline shelf it's locked to that discipline and starts on
// the source step. Guidelines show only until the student's first successful
// submission (`materials_guidelines_acknowledged`), mirroring iOS
// `MaterialsUploadFeature`.
internal enum class MaterialsUploadStep {
    PickDiscipline,
    Source,
    Details,
    Guidelines,
    Success,
}

internal data class MaterialsPickedFile(
    val fileName: String,
    val byteCount: Int,
    val pages: Int,
    val bytes: ByteArray,
    val isScan: Boolean,
)

internal data class MaterialsUploadUiState(
    val isOpen: Boolean = false,
    val step: MaterialsUploadStep = MaterialsUploadStep.PickDiscipline,
    val options: List<MaterialsDiscipline> = emptyList(),
    val discipline: MaterialsDiscipline? = null,
    // Locked = opened from a discipline shelf; back never re-opens the pick.
    val isLocked: Boolean = false,
    val file: MaterialsPickedFile? = null,
    val fileReadFailed: Boolean = false,
    val type: MaterialType = MaterialType.Exam,
    val title: String = "",
    val semester: String = "",
    val semesterOptions: List<String> = emptyList(),
    val teacherName: String = "",
    val isGuidelinesAccepted: Boolean = false,
    val guidelinesAlreadyAcknowledged: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitFailed: Boolean = false,
    val submitted: Material? = null,
) : UiState {
    val canContinue: Boolean get() = title.trim().length > 1 && file != null
    val showsBack: Boolean
        get() = when (step) {
            MaterialsUploadStep.Source -> !isLocked
            MaterialsUploadStep.Details, MaterialsUploadStep.Guidelines -> true
            else -> false
        }
    val showsProgress: Boolean
        get() = step == MaterialsUploadStep.Details || step == MaterialsUploadStep.Guidelines
}

internal sealed interface MaterialsUploadIntent : UiIntent {
    // Hub entry — free discipline pick over the overview's options.
    data class StartFromHub(val options: List<MaterialsDiscipline>) : MaterialsUploadIntent

    // Shelf entry — locked to the discipline the student is looking at.
    data class StartFromDiscipline(val discipline: MaterialsDiscipline) : MaterialsUploadIntent

    data class PickDiscipline(val disciplineId: String) : MaterialsUploadIntent
    data class FilePicked(val uri: Uri) : MaterialsUploadIntent

    // ML Kit document scanner result — already a PDF in app storage, with the
    // page count reported by the scanner.
    data class ScanPicked(val pdfUri: Uri, val pages: Int) : MaterialsUploadIntent
    data class TypeChanged(val type: MaterialType) : MaterialsUploadIntent
    data class TitleChanged(val title: String) : MaterialsUploadIntent
    data class SemesterChanged(val semester: String) : MaterialsUploadIntent
    data class TeacherChanged(val teacher: String) : MaterialsUploadIntent
    data object ToggleGuidelines : MaterialsUploadIntent
    data object Back : MaterialsUploadIntent
    // Details CTA — advances to guidelines, or submits directly once they
    // were acknowledged on a previous upload.
    data object Continue : MaterialsUploadIntent
    // Guidelines CTA.
    data object Publish : MaterialsUploadIntent
    data object Dismiss : MaterialsUploadIntent
}

internal sealed interface MaterialsUploadEffect : UiEffect {
    // Fired when the sheet closes after a successful submission so hosts can
    // refetch (the new material lands under "Meus envios").
    data class Finished(val disciplineId: String) : MaterialsUploadEffect
}

@HiltViewModel
internal class MaterialsUploadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val submitMaterial: SubmitMaterialUseCase,
    private val analytics: Analytics,
) : MviViewModel<MaterialsUploadUiState, MaterialsUploadIntent, MaterialsUploadEffect>(
    MaterialsUploadUiState(),
) {
    override fun onIntent(intent: MaterialsUploadIntent) {
        when (intent) {
            is MaterialsUploadIntent.StartFromHub -> setState {
                freshState().copy(
                    isOpen = true,
                    options = intent.options,
                    step = MaterialsUploadStep.PickDiscipline,
                )
            }
            is MaterialsUploadIntent.StartFromDiscipline -> setState {
                freshState().copy(
                    isOpen = true,
                    discipline = intent.discipline,
                    isLocked = true,
                    step = MaterialsUploadStep.Source,
                )
            }
            is MaterialsUploadIntent.PickDiscipline -> {
                val choice = currentState.options.firstOrNull { it.id == intent.disciplineId }
                    ?: return
                setState { copy(discipline = choice, step = MaterialsUploadStep.Source) }
            }
            is MaterialsUploadIntent.FilePicked -> readFile(intent.uri)
            is MaterialsUploadIntent.ScanPicked -> readScan(intent)
            is MaterialsUploadIntent.TypeChanged -> setState { copy(type = intent.type) }
            is MaterialsUploadIntent.TitleChanged -> setState { copy(title = intent.title) }
            is MaterialsUploadIntent.SemesterChanged -> setState { copy(semester = intent.semester) }
            is MaterialsUploadIntent.TeacherChanged -> setState { copy(teacherName = intent.teacher) }
            MaterialsUploadIntent.ToggleGuidelines -> setState {
                copy(isGuidelinesAccepted = !isGuidelinesAccepted)
            }
            MaterialsUploadIntent.Back -> back()
            MaterialsUploadIntent.Continue -> {
                if (!currentState.canContinue || currentState.isSubmitting) return
                if (currentState.guidelinesAlreadyAcknowledged) {
                    submit()
                } else {
                    setState { copy(step = MaterialsUploadStep.Guidelines) }
                }
            }
            MaterialsUploadIntent.Publish -> {
                if (currentState.isGuidelinesAccepted && !currentState.isSubmitting) submit()
            }
            MaterialsUploadIntent.Dismiss -> dismiss()
        }
    }

    private fun freshState() = MaterialsUploadUiState(
        semester = MaterialsFormat.currentSemester(),
        semesterOptions = MaterialsFormat.uploadSemesters(),
        guidelinesAlreadyAcknowledged = prefs().getBoolean(KEY_GUIDELINES, false),
    )

    private fun back() {
        setState {
            when (step) {
                MaterialsUploadStep.Source ->
                    if (isLocked) this else copy(step = MaterialsUploadStep.PickDiscipline, discipline = null)
                MaterialsUploadStep.Details -> copy(step = MaterialsUploadStep.Source)
                MaterialsUploadStep.Guidelines -> copy(step = MaterialsUploadStep.Details)
                else -> this
            }
        }
    }

    private fun dismiss() {
        val submitted = currentState.submitted
        setState { copy(isOpen = false) }
        if (submitted != null) {
            emitEffect(MaterialsUploadEffect.Finished(submitted.discipline.id))
        }
    }

    // SAF grant → bytes + display name + page count off the main thread.
    private fun readFile(uri: Uri) {
        viewModelScope.launch {
            val picked = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = context.contentResolver
                    val name = resolver.query(uri, null, null, null, null)?.use { cursor ->
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                    } ?: "material.pdf"
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("unreadable")
                    val pages = runCatching {
                        resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                            pageCount(descriptor)
                        }
                    }.getOrNull() ?: 1
                    MaterialsPickedFile(
                        fileName = name,
                        byteCount = bytes.size,
                        pages = pages,
                        bytes = bytes,
                        isScan = false,
                    )
                }.getOrNull()
            }
            setState {
                if (picked == null) {
                    copy(fileReadFailed = true)
                } else {
                    copy(file = picked, fileReadFailed = false, step = MaterialsUploadStep.Details)
                }
            }
        }
    }

    private fun pageCount(descriptor: ParcelFileDescriptor): Int =
        PdfRenderer(descriptor).use { it.pageCount }

    // The scanner flattens the captured sheets into a PDF and reports its
    // page count, so only the bytes need lifting. Fixed name mirrors iOS
    // ("digitalizacao.pdf").
    private fun readScan(intent: MaterialsUploadIntent.ScanPicked) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(intent.pdfUri)?.use { it.readBytes() }
                }.getOrNull()
            }
            setState {
                if (bytes == null) {
                    copy(fileReadFailed = true)
                } else {
                    copy(
                        file = MaterialsPickedFile(
                            fileName = "digitalizacao.pdf",
                            byteCount = bytes.size,
                            pages = intent.pages.coerceAtLeast(1),
                            bytes = bytes,
                            isScan = true,
                        ),
                        fileReadFailed = false,
                        step = MaterialsUploadStep.Details,
                    )
                }
            }
        }
    }

    private fun submit() {
        val state = currentState
        val discipline = state.discipline ?: return
        val file = state.file ?: return
        setState { copy(isSubmitting = true, submitFailed = false) }
        viewModelScope.launch {
            val submission = MaterialSubmission(
                disciplineId = discipline.id,
                type = state.type,
                title = state.title.trim(),
                semester = state.semester,
                teacherName = state.teacherName.trim().ifBlank { null },
                fileKind = MaterialFileKind.Pdf,
                pages = file.pages,
                fileName = file.fileName,
                bytes = file.bytes,
            )
            when (val outcome = submitMaterial(submission)) {
                is Outcome.Ok -> {
                    analytics.selectContent(
                        contentType = ContentTypes.MATERIAL,
                        itemId = outcome.value.id,
                        properties = mapOf("action" to "submit"),
                    )
                    prefs().edit { putBoolean(KEY_GUIDELINES, true) }
                    setState {
                        copy(
                            isSubmitting = false,
                            submitted = outcome.value,
                            step = MaterialsUploadStep.Success,
                            guidelinesAlreadyAcknowledged = true,
                        )
                    }
                }
                is Outcome.Err -> setState { copy(isSubmitting = false, submitFailed = true) }
            }
        }
    }

    private fun prefs() = context.getSharedPreferences("materials", Context.MODE_PRIVATE)

    private companion object {
        // Same key iOS uses in appStorage so the semantics stay recognizable.
        const val KEY_GUIDELINES = "materials_guidelines_acknowledged"
    }
}

package dev.forcetower.unes.ui.feature.materials

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialType
import dev.forcetower.melon.feature.materials.domain.model.MaterialsDisciplineDetails
import dev.forcetower.melon.feature.materials.domain.usecase.GetMaterialsDisciplineUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import java.text.Normalizer
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Drives one discipline's shelf. The route only carries the discipline id
// (plus optional code/name seeds so the header renders before the fetch), so
// the screen is directly pushable from anywhere that knows a discipline —
// the hub today; discipline detail / schedule quick actions later. Mirrors
// iOS `MaterialsListFeature`.
internal data class MaterialsListUiState(
    val disciplineId: String = "",
    val seedCode: String = "",
    val seedName: String = "",
    val details: MaterialsDisciplineDetails? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val query: String = "",
    val typeFilter: MaterialType? = null,
) : UiState {
    val code: String get() = details?.discipline?.code ?: seedCode
    val name: String get() = details?.discipline?.name ?: seedName

    val published: List<Material> get() = details?.published.orEmpty()
    val mine: List<Material> get() = details?.mine.orEmpty()

    // Diacritic/case-insensitive search over title + professor, newest
    // semester first (string compare works for "2025.2" codes).
    val filtered: List<Material>
        get() {
            val folded = query.fold()
            return published
                .filter { typeFilter == null || it.type == typeFilter }
                .filter {
                    folded.isBlank() ||
                        it.title.fold().contains(folded) ||
                        it.teacherName.orEmpty().fold().contains(folded)
                }
                .sortedByDescending { it.semester }
        }

    val countsByType: Map<MaterialType, Int>
        get() = published.groupingBy { it.type }.eachCount()

    // The shelf is empty-empty (no acervo AND nothing of mine) → hero pitch.
    val isEmpty: Boolean get() = details != null && published.isEmpty() && mine.isEmpty()
}

internal sealed interface MaterialsListIntent : UiIntent {
    // Route entry — resets state when the discipline changes and refetches.
    data class Open(
        val disciplineId: String,
        val seedCode: String?,
        val seedName: String?,
    ) : MaterialsListIntent

    data object Reload : MaterialsListIntent
    data class QueryChanged(val query: String) : MaterialsListIntent
    data class FilterChanged(val filter: MaterialType?) : MaterialsListIntent
}

internal sealed interface MaterialsListEffect : UiEffect

@HiltViewModel
internal class MaterialsListViewModel @Inject constructor(
    private val getDiscipline: GetMaterialsDisciplineUseCase,
    private val analytics: Analytics,
) : MviViewModel<MaterialsListUiState, MaterialsListIntent, MaterialsListEffect>(
    MaterialsListUiState(),
) {
    private var loadJob: Job? = null

    override fun onIntent(intent: MaterialsListIntent) {
        when (intent) {
            is MaterialsListIntent.Open -> open(intent)
            MaterialsListIntent.Reload -> load()
            is MaterialsListIntent.QueryChanged -> setState { copy(query = intent.query) }
            is MaterialsListIntent.FilterChanged -> setState { copy(typeFilter = intent.filter) }
        }
    }

    fun trackMaterialOpen(materialId: String) {
        analytics.selectContent(contentType = ContentTypes.MATERIAL, itemId = materialId)
    }

    private fun open(intent: MaterialsListIntent.Open) {
        val changed = currentState.disciplineId != intent.disciplineId
        setState {
            if (changed) {
                MaterialsListUiState(
                    disciplineId = intent.disciplineId,
                    seedCode = intent.seedCode.orEmpty(),
                    seedName = intent.seedName.orEmpty(),
                )
            } else {
                copy(
                    seedCode = intent.seedCode ?: seedCode,
                    seedName = intent.seedName ?: seedName,
                )
            }
        }
        load()
    }

    private fun load() {
        val disciplineId = currentState.disciplineId
        if (disciplineId.isBlank()) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            setState { copy(isLoading = true, loadFailed = false) }
            when (val outcome = getDiscipline(disciplineId)) {
                is Outcome.Ok -> setState {
                    copy(details = outcome.value, isLoading = false, loadFailed = false)
                }
                is Outcome.Err -> setState {
                    copy(isLoading = false, loadFailed = details == null)
                }
            }
        }
    }
}

private fun String.fold(): String = Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")

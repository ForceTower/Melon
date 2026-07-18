package dev.forcetower.unes.ui.feature.materials

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.usecase.GetSavedMaterialsUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// The "Salvos" shelf — server-side bookmarks grouped by discipline, in server
// order. Mirrors iOS `MaterialsSavedFeature`.
internal data class MaterialsSavedUiState(
    val materials: List<Material>? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
) : UiState {
    // Grouped preserving server order of first appearance.
    val groups: List<Pair<String, List<Material>>>
        get() = materials.orEmpty()
            .groupBy { it.discipline.id }
            .map { (_, items) -> items.first().discipline.name to items }
}

internal sealed interface MaterialsSavedIntent : UiIntent {
    data object Load : MaterialsSavedIntent
}

internal sealed interface MaterialsSavedEffect : UiEffect

@HiltViewModel
internal class MaterialsSavedViewModel @Inject constructor(
    private val getSaved: GetSavedMaterialsUseCase,
    private val analytics: Analytics,
) : MviViewModel<MaterialsSavedUiState, MaterialsSavedIntent, MaterialsSavedEffect>(
    MaterialsSavedUiState(),
) {
    private var loadJob: Job? = null

    override fun onIntent(intent: MaterialsSavedIntent) {
        when (intent) {
            MaterialsSavedIntent.Load -> load()
        }
    }

    fun trackMaterialOpen(materialId: String) {
        analytics.selectContent(contentType = ContentTypes.MATERIAL, itemId = materialId)
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            setState { copy(isLoading = true, loadFailed = false) }
            when (val outcome = getSaved()) {
                is Outcome.Ok -> setState {
                    copy(materials = outcome.value, isLoading = false, loadFailed = false)
                }
                is Outcome.Err -> setState {
                    copy(isLoading = false, loadFailed = materials == null)
                }
            }
        }
    }
}

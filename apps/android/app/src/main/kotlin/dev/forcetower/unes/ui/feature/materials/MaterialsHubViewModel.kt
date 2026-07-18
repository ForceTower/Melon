package dev.forcetower.unes.ui.feature.materials

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.materials.domain.model.MaterialsOverview
import dev.forcetower.melon.feature.materials.domain.usecase.GetMaterialsOverviewUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Drives the Materiais hub. Online-only: every entry refetches the overview,
// but stale content keeps rendering while a reload is in flight — `failed`
// only shows when there's nothing at all to draw ("a stale screen beats an
// error screen", same posture as iOS `MaterialsFeature`).
internal data class MaterialsHubUiState(
    val overview: MaterialsOverview? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
) : UiState

internal sealed interface MaterialsHubIntent : UiIntent {
    data object Load : MaterialsHubIntent
    data object Retry : MaterialsHubIntent
}

internal sealed interface MaterialsHubEffect : UiEffect

@HiltViewModel
internal class MaterialsHubViewModel @Inject constructor(
    private val getOverview: GetMaterialsOverviewUseCase,
    private val analytics: Analytics,
) : MviViewModel<MaterialsHubUiState, MaterialsHubIntent, MaterialsHubEffect>(
    MaterialsHubUiState(),
) {
    private var loadJob: Job? = null

    override fun onIntent(intent: MaterialsHubIntent) {
        when (intent) {
            MaterialsHubIntent.Load, MaterialsHubIntent.Retry -> load()
        }
    }

    fun trackDisciplineOpen(disciplineId: String) {
        analytics.selectContent(contentType = ContentTypes.DISCIPLINE, itemId = disciplineId)
    }

    fun trackSavedOpen() {
        analytics.selectContent(contentType = ContentTypes.HUB, itemId = "materials_saved")
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            setState { copy(isLoading = true, loadFailed = false) }
            when (val outcome = getOverview()) {
                is Outcome.Ok -> setState {
                    copy(overview = outcome.value, isLoading = false, loadFailed = false)
                }
                is Outcome.Err -> setState {
                    copy(isLoading = false, loadFailed = overview == null)
                }
            }
        }
    }
}

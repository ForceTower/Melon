package dev.forcetower.unes.ui.feature.disciplinedetail

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplineDetailUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Drives `DisciplineDetailScreen`. Subscribes to the same KMP detail flow iOS
// uses (see `DisciplineDetailViewModel.swift`); each emission is mapped into
// the local UI projection by `mapDetail`. Until the first non-null emission
// lands, the screen renders the seed Discipline carried over from the list.
internal sealed interface DisciplineDetailIntent : UiIntent {
    data class Open(val offerId: String, val seed: Discipline?) : DisciplineDetailIntent
    data object Close : DisciplineDetailIntent
    data class SelectGroup(val code: String?) : DisciplineDetailIntent
}

internal sealed interface DisciplineDetailEffect : UiEffect

internal data class DisciplineDetailUiState(
    val offerId: String? = null,
    val seed: Discipline? = null,
    val hydrated: Discipline? = null,
    // Currently-selected group pill on multi-group disciplines. Null = "Tudo".
    val selectedGroup: String? = null,
) : UiState {
    // What the screen actually renders. Hydrated payload wins over the seed
    // once it lands; until then we paint the list-card data so the screen
    // doesn't open to a blank shell.
    val discipline: Discipline?
        get() = hydrated ?: seed
}

@HiltViewModel
internal class DisciplineDetailViewModel @Inject constructor(
    private val observeDetail: ObserveDisciplineDetailUseCase,
) : MviViewModel<DisciplineDetailUiState, DisciplineDetailIntent, DisciplineDetailEffect>(
    DisciplineDetailUiState(),
) {
    private var detailJob: Job? = null

    override fun onIntent(intent: DisciplineDetailIntent) {
        when (intent) {
            is DisciplineDetailIntent.Open -> open(intent.offerId, intent.seed)
            DisciplineDetailIntent.Close -> close()
            is DisciplineDetailIntent.SelectGroup -> setState { copy(selectedGroup = intent.code) }
        }
    }

    private fun open(offerId: String, seed: Discipline?) {
        if (currentState.offerId == offerId) {
            // Same offer reopened — refresh the seed (it may have been updated
            // by a list re-emission) but keep the existing hydrated payload so
            // the screen doesn't flash back to the seed while waiting.
            if (seed != null) setState { copy(seed = seed) }
            return
        }
        detailJob?.cancel()
        setState {
            copy(offerId = offerId, seed = seed, hydrated = null, selectedGroup = null)
        }
        detailJob = viewModelScope.launch {
            observeDetail(offerId).collect { raw ->
                if (raw == null) return@collect
                val mapped = mapDetail(raw, seed = currentState.seed)
                setState { copy(hydrated = mapped) }
            }
        }
    }

    private fun close() {
        detailJob?.cancel()
        detailJob = null
        setState { DisciplineDetailUiState() }
    }
}

package dev.forcetower.unes.ui.feature.paradoxo

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoDisciplineDetail
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoTeacherDetail
import dev.forcetower.melon.feature.paradoxo.domain.usecase.GetParadoxoDisciplineUseCase
import dev.forcetower.melon.feature.paradoxo.domain.usecase.GetParadoxoIndexUseCase
import dev.forcetower.melon.feature.paradoxo.domain.usecase.GetParadoxoOverviewUseCase
import dev.forcetower.melon.feature.paradoxo.domain.usecase.GetParadoxoTeacherUseCase
import dev.forcetower.unes.mvi.MviViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

// One activity-scoped ViewModel for the whole Paradoxo stack (home, search,
// explore, discipline/teacher details). Nav3 entries all resolve the same
// instance — same trick as `MessagesViewModel` — so pushed details keep their
// data on back navigation and the overview/index are fetched once per
// session. Data is live/uncached beyond that: the aggregates change once a
// semester (mirrors iOS `ParadoxoRepository`).
@HiltViewModel
internal class ParadoxoViewModel @Inject constructor(
    private val getOverview: GetParadoxoOverviewUseCase,
    private val getIndex: GetParadoxoIndexUseCase,
    private val getDiscipline: GetParadoxoDisciplineUseCase,
    private val getTeacher: GetParadoxoTeacherUseCase,
    observeMeProfile: ObserveMeProfileUseCase,
) : MviViewModel<ParadoxoUiState, ParadoxoIntent, ParadoxoEffect>(ParadoxoUiState()) {

    init {
        viewModelScope.launch {
            observeMeProfile().collect { profile ->
                val first = profile.identity.firstName.ifBlank { profile.identity.userName }
                setState { copy(avatarInitial = first.firstOrNull()?.uppercaseChar()?.toString()) }
            }
        }
    }

    override fun onIntent(intent: ParadoxoIntent) {
        when (intent) {
            ParadoxoIntent.Load -> load(force = false)
            ParadoxoIntent.Retry -> load(force = true)
            is ParadoxoIntent.QueryChanged -> setState { copy(query = intent.query) }
            is ParadoxoIntent.LoadDiscipline -> loadDiscipline(intent.id, force = false)
            is ParadoxoIntent.RetryDiscipline -> loadDiscipline(intent.id, force = true)
            is ParadoxoIntent.LoadTeacher -> loadTeacher(intent.id, force = false)
            is ParadoxoIntent.RetryTeacher -> loadTeacher(intent.id, force = true)
        }
    }

    private fun load(force: Boolean) {
        if (!force && (currentState.overview != null || currentState.loading)) return
        setState { copy(loading = true, failed = false) }
        viewModelScope.launch {
            val overviewCall = async { getOverview() }
            val indexCall = async { getIndex() }
            when (val outcome = overviewCall.await()) {
                is Outcome.Ok -> setState { copy(loading = false, overview = outcome.value) }
                // Keep a stale overview on refresh failures; only surface the
                // failure state when there's nothing to show.
                is Outcome.Err -> setState { copy(loading = false, failed = overview == null) }
            }
            when (val outcome = indexCall.await()) {
                is Outcome.Ok -> setState {
                    copy(
                        index = outcome.value.map { entry ->
                            ParadoxoIndexItem(
                                entry = entry,
                                searchKey = paradoxoFold("${entry.code.orEmpty()} ${entry.name}"),
                            )
                        },
                    )
                }
                // Search degrades silently without an index, same as iOS.
                is Outcome.Err -> Unit
            }
        }
    }

    private fun loadDiscipline(id: String, force: Boolean) {
        val current = currentState.disciplines[id]
        if (!force && current != null && current !is ParadoxoDetail.Failed) return
        setState { copy(disciplines = disciplines + (id to ParadoxoDetail.Loading)) }
        viewModelScope.launch {
            val detail: ParadoxoDetail<ParadoxoDisciplineDetail> = when (val outcome = getDiscipline(id)) {
                is Outcome.Ok -> ParadoxoDetail.Loaded(outcome.value)
                is Outcome.Err -> ParadoxoDetail.Failed
            }
            setState { copy(disciplines = disciplines + (id to detail)) }
        }
    }

    private fun loadTeacher(id: String, force: Boolean) {
        val current = currentState.teachers[id]
        if (!force && current != null && current !is ParadoxoDetail.Failed) return
        setState { copy(teachers = teachers + (id to ParadoxoDetail.Loading)) }
        viewModelScope.launch {
            val detail: ParadoxoDetail<ParadoxoTeacherDetail> = when (val outcome = getTeacher(id)) {
                is Outcome.Ok -> ParadoxoDetail.Loaded(outcome.value)
                is Outcome.Err -> ParadoxoDetail.Failed
            }
            setState { copy(teachers = teachers + (id to detail)) }
        }
    }
}

package dev.forcetower.unes.ui.feature.onboarding.ready

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.feature.dashboard.domain.model.NextClassInfo
import dev.forcetower.melon.feature.dashboard.domain.usecase.GetReadyOverviewUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.launch

data class ReadyUiState(
    val loading: Boolean = true,
    val semesterCode: String? = null,
    val classCount: Int = 0,
    val totalCredits: Int = 0,
    val next: NextClassDisplay? = null,
) : UiState

data class NextClassDisplay(
    val disciplineName: String,
    val timeRaw: String,
    val spaceLocation: String?,
    val teacherFirstName: String?,
    val startsInMinutes: Int,
)

sealed interface ReadyIntent : UiIntent

sealed interface ReadyEffect : UiEffect

@HiltViewModel
class ReadyViewModel @Inject constructor(
    private val getReadyOverview: GetReadyOverviewUseCase,
) : MviViewModel<ReadyUiState, ReadyIntent, ReadyEffect>(ReadyUiState()) {

    init {
        viewModelScope.launch {
            val overview = runCatching { getReadyOverview() }.getOrNull()
            if (overview == null) {
                setState { copy(loading = false) }
                return@launch
            }
            setState {
                copy(
                    loading = false,
                    semesterCode = overview.semesterCode,
                    classCount = overview.classCount,
                    totalCredits = overview.totalCredits,
                    next = overview.nextClass?.toDisplay(),
                )
            }
        }
    }

    override fun onIntent(intent: ReadyIntent) = Unit
}

private fun NextClassInfo.toDisplay(): NextClassDisplay = NextClassDisplay(
    disciplineName = disciplineName,
    timeRaw = startTime,
    spaceLocation = spaceLocation?.takeIf { it.isNotBlank() },
    teacherFirstName = teacherName?.trim()?.split(' ', '\t', '\n')?.firstOrNull()
        ?.takeIf { it.isNotBlank() },
    startsInMinutes = startsInMinutes,
)

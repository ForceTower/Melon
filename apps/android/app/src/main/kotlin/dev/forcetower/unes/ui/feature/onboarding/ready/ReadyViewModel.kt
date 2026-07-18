package dev.forcetower.unes.ui.feature.onboarding.ready

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.analytics.Screens
import dev.forcetower.melon.feature.dashboard.domain.model.NextClassInfo
import dev.forcetower.melon.feature.dashboard.domain.usecase.GetReadyOverviewUseCase
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplinesListState
import dev.forcetower.melon.feature.disciplines.domain.usecase.CalculateOverallScoreUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplinesListUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlin.math.floor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ReadyUiState(
    val loading: Boolean = true,
    val semesterCode: String? = null,
    val classCount: Int = 0,
    val totalCredits: Int = 0,
    val next: NextClassDisplay? = null,
    // Lifetime CR (fallback: hours-weighted partial mean of the current
    // semester) — null hides the Coeficiente card.
    val score: Double? = null,
    // Cumulative CR after each closed semester, oldest first. Fewer than two
    // points hides the sparkline (the value still shows).
    val scoreSpark: List<Double> = emptyList(),
    // Attendance percentage over the current semester — null hides the card.
    val attendancePercent: Int? = null,
) : UiState

data class NextClassDisplay(
    val disciplineName: String,
    val startRaw: String,
    val endRaw: String?,
    val spaceLocation: String?,
    val teacherName: String?,
    val startsInMinutes: Int,
)

sealed interface ReadyIntent : UiIntent

sealed interface ReadyEffect : UiEffect

@HiltViewModel
class ReadyViewModel @Inject constructor(
    private val getReadyOverview: GetReadyOverviewUseCase,
    private val calculateOverallScore: CalculateOverallScoreUseCase,
    private val observeDisciplinesList: ObserveDisciplinesListUseCase,
    private val analytics: Analytics,
) : MviViewModel<ReadyUiState, ReadyIntent, ReadyEffect>(ReadyUiState()) {

    init {
        analytics.screen(Screens.READY)
        viewModelScope.launch {
            val overview = runCatching { getReadyOverview() }.getOrNull()
            if (overview == null) {
                setState { copy(loading = false) }
            } else {
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
        viewModelScope.launch {
            runCatching { loadStats() }
        }
    }

    override fun onIntent(intent: ReadyIntent) = Unit

    fun trackEnter() {
        analytics.selectContent(ContentTypes.CTA, itemId = "ready_enter")
    }

    private suspend fun loadStats() {
        val disciplines = observeDisciplinesList().first()
        val summary = calculateOverallScore.summary().first()
        val score = summary.value ?: currentPartialMean(disciplines)
        setState {
            copy(
                score = score,
                attendancePercent = attendancePercent(disciplines),
            )
        }
        // Sparkline second — each point re-runs the CR query with a cap, so
        // the cards render before this finishes.
        if (summary.value != null) {
            val spark = scoreHistory(disciplines)
            setState { copy(scoreSpark = spark) }
        }
    }

    // Cumulative CR after each semester, oldest first. `capSemesterId` clips
    // to semesters strictly before the cap, so the point that includes
    // semester i uses semester i+1 as the cap; the newest point is uncapped.
    private suspend fun scoreHistory(state: DisciplinesListState): List<Double> {
        val ordered = (state.past + listOfNotNull(state.current))
            .distinctBy { it.semesterId }
            .sortedBy { it.semesterCode }
        if (ordered.size < 2) return emptyList()
        val points = mutableListOf<Double>()
        for (i in 1 until ordered.size) {
            calculateOverallScore(capSemesterId = ordered[i].semesterId).first()
                ?.let { points += it }
        }
        calculateOverallScore().first()?.let { points += it }
        return if (points.size >= 2) points else emptyList()
    }

    // Freshman fallback — no semester has closed grades yet, so surface the
    // hours-weighted mean of the released partial averages instead.
    private fun currentPartialMean(state: DisciplinesListState): Double? {
        val items = state.current?.disciplines
            ?.filter { it.hours > 0 && it.partialAverage != null }
            .orEmpty()
        val hours = items.sumOf { it.hours }
        if (hours == 0) return null
        return items.sumOf { (it.partialAverage ?: 0.0) * it.hours } / hours
    }

    private fun attendancePercent(state: DisciplinesListState): Int? {
        val items = state.current?.disciplines?.filter { it.hours > 0 }.orEmpty()
        val hours = items.sumOf { it.hours }
        if (hours == 0) return null
        val missed = items.sumOf { it.missedHours }
        return floor(100.0 * (hours - missed) / hours).toInt().coerceIn(0, 100)
    }
}

private fun NextClassInfo.toDisplay(): NextClassDisplay = NextClassDisplay(
    disciplineName = disciplineName,
    startRaw = startTime,
    endRaw = endTime,
    spaceLocation = spaceLocation?.takeIf { it.isNotBlank() },
    teacherName = teacherName?.trim()?.takeIf { it.isNotBlank() },
    startsInMinutes = startsInMinutes,
)

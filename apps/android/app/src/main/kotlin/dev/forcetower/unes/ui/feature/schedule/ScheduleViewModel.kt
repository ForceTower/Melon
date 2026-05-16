package dev.forcetower.unes.ui.feature.schedule

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.feature.schedule.domain.usecase.ObserveScheduleWeekUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleWeek as KmpScheduleWeek

internal data class ScheduleUiState(
    val raw: KmpScheduleWeek? = null,
    val clock: Instant = Clock.System.now(),
) : UiState {
    val todayIdx: Int
        get() = raw?.todayDayIndex ?: -1

    val weekNumber: Int
        get() = raw?.weekNumber ?: 0

    val nowMin: Int
        get() {
            val ldt = clock.toLocalDateTime(TimeZone.currentSystemDefault())
            return ldt.hour * 60 + ldt.minute
        }

    val dates: List<Int>
        get() {
            val days = raw?.days ?: return List(7) { 0 }
            val out = MutableList(7) { 0 }
            for (day in days) {
                val idx = day.dayIndex
                if (idx in 0..6) out[idx] = parseDayOfMonth(day.dateIso)
            }
            return out
        }

    val firstIso: String?
        get() = raw?.days?.firstOrNull()?.dateIso

    val lastIso: String?
        get() = raw?.days?.lastOrNull()?.dateIso
}

internal sealed interface ScheduleIntent : UiIntent
internal sealed interface ScheduleEffect : UiEffect

@HiltViewModel
internal class ScheduleViewModel @Inject constructor(
    observeScheduleWeek: ObserveScheduleWeekUseCase,
) : MviViewModel<ScheduleUiState, ScheduleIntent, ScheduleEffect>(ScheduleUiState()) {

    init {
        viewModelScope.launch {
            observeScheduleWeek().collect { value -> setState { copy(raw = value) } }
        }
        // Clock ticker — `nowMin` derives from `state.clock`, so a 30s tick is
        // enough for class state transitions (DONE/NOW/NEXT/LATER) to land
        // close to real time without forcing the KMP flow to re-emit.
        viewModelScope.launch {
            while (isActive) {
                setState { copy(clock = Clock.System.now()) }
                delay(CLOCK_TICK_MS)
            }
        }
    }

    override fun onIntent(intent: ScheduleIntent) = Unit

    private companion object {
        const val CLOCK_TICK_MS = 30_000L
    }
}

// ISO "yyyy-MM-dd" → day-of-month. 0 if malformed (defensive only — upstream
// always emits well-formed ISO dates).
private fun parseDayOfMonth(iso: String): Int {
    if (iso.length < 10) return 0
    return iso.substring(8, 10).toIntOrNull() ?: 0
}

package dev.forcetower.unes.ui.feature.calendar

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.feature.calendar.domain.usecase.ObserveActiveSemesterCodeUseCase
import dev.forcetower.melon.feature.calendar.domain.usecase.ObserveCalendarEventsUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.launch

internal data class CalendarUiState(
    val events: List<CalendarEvent> = emptyList(),
    val semesterCode: String? = null,
) : UiState

internal sealed interface CalendarIntent : UiIntent
internal sealed interface CalendarEffect : UiEffect

// Drives `CalendarScreen`. Subscribes to the events flow and the active-
// semester code in parallel — each emission updates the same state. Mirrors
// `CalendarViewModel` on iOS.
@HiltViewModel
internal class CalendarViewModel @Inject constructor(
    observeEvents: ObserveCalendarEventsUseCase,
    observeActiveSemesterCode: ObserveActiveSemesterCodeUseCase,
) : MviViewModel<CalendarUiState, CalendarIntent, CalendarEffect>(CalendarUiState()) {

    init {
        viewModelScope.launch {
            observeEvents().collect { feed ->
                setState { copy(events = feed.map(::mapCalendarEvent)) }
            }
        }
        viewModelScope.launch {
            observeActiveSemesterCode().collect { code ->
                setState { copy(semesterCode = code) }
            }
        }
    }

    override fun onIntent(intent: CalendarIntent) = Unit
}

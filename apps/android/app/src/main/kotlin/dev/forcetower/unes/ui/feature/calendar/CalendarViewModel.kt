package dev.forcetower.unes.ui.feature.calendar

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.feature.calendar.domain.usecase.ObserveCalendarEventsUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.launch

internal data class CalendarUiState(
    val events: List<CalendarEvent> = emptyList(),
) : UiState

internal sealed interface CalendarIntent : UiIntent
internal sealed interface CalendarEffect : UiEffect

// Drives `CalendarScreen`. Filtering, view mode, and selection are all view
// concerns — the VM only projects the KMP events flow.
@HiltViewModel
internal class CalendarViewModel @Inject constructor(
    observeEvents: ObserveCalendarEventsUseCase,
    private val analytics: Analytics,
) : MviViewModel<CalendarUiState, CalendarIntent, CalendarEffect>(CalendarUiState()) {

    init {
        viewModelScope.launch {
            observeEvents().collect { feed ->
                setState { copy(events = feed.map(::mapCalendarEvent)) }
            }
        }
    }

    override fun onIntent(intent: CalendarIntent) = Unit

    fun trackOpenEvent(event: CalendarEvent) {
        analytics.selectContent(
            contentType = ContentTypes.CALENDAR_EVENT,
            itemId = event.id,
            properties = mapOf("category" to CalendarMath.categorize(event).name.lowercase()),
        )
    }

    fun trackAddToCalendar(event: CalendarEvent) {
        analytics.selectContent(
            contentType = ContentTypes.CALENDAR_EVENT,
            itemId = event.id,
            properties = mapOf("action" to "save"),
        )
    }
}

package dev.forcetower.unes.ui.feature.calendar

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.feature.calendar.domain.usecase.DeletePersonalEventUseCase
import dev.forcetower.melon.feature.calendar.domain.usecase.ObserveCalendarEventsUseCase
import dev.forcetower.melon.feature.calendar.domain.usecase.ObservePersonalEventsUseCase
import dev.forcetower.melon.feature.calendar.domain.usecase.SavePersonalEventUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplinesListUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import dev.forcetower.unes.reminders.PersonalEventReminderScheduler
import javax.inject.Inject
import kotlinx.coroutines.launch

internal data class CalendarUiState(
    val events: List<CalendarEvent> = emptyList(),
    val personal: List<PersonalEntry> = emptyList(),
    // Class picker choices for the composer, from the running semester.
    val disciplines: List<PersonalDisciplineOption> = emptyList(),
) : UiState {
    // Both feeds on one timeline.
    val allEvents: List<CalendarEvent>
        get() = (events + personal.map { it.asCalendarEvent() })
            .sortedWith(compareBy({ it.start }, { it.description }))
}

internal sealed interface CalendarIntent : UiIntent
internal sealed interface CalendarEffect : UiEffect

// Drives `CalendarScreen`. Filtering, view mode, and selection are all view
// concerns — the VM projects the KMP flows and owns the personal-entry writes.
@HiltViewModel
internal class CalendarViewModel @Inject constructor(
    observeEvents: ObserveCalendarEventsUseCase,
    observePersonal: ObservePersonalEventsUseCase,
    observeDisciplines: ObserveDisciplinesListUseCase,
    private val savePersonalEvent: SavePersonalEventUseCase,
    private val deletePersonalEvent: DeletePersonalEventUseCase,
    private val reminders: PersonalEventReminderScheduler,
    private val analytics: Analytics,
) : MviViewModel<CalendarUiState, CalendarIntent, CalendarEffect>(CalendarUiState()) {

    init {
        viewModelScope.launch {
            observeEvents().collect { feed ->
                setState { copy(events = feed.map(::mapCalendarEvent)) }
            }
        }
        viewModelScope.launch {
            observePersonal().collect { entries ->
                setState { copy(personal = entries.map(::mapPersonalEvent)) }
            }
        }
        viewModelScope.launch {
            observeDisciplines().collect { state ->
                val options = state.current?.disciplines.orEmpty().map { item ->
                    PersonalDisciplineOption(
                        discipline = PersonalDiscipline(
                            id = item.disciplineId,
                            code = item.code,
                            name = item.name,
                        ),
                    )
                }
                setState { copy(disciplines = options) }
            }
        }
    }

    override fun onIntent(intent: CalendarIntent) = Unit

    fun savePersonal(entry: PersonalEntry, isNew: Boolean) {
        analytics.selectContent(
            contentType = ContentTypes.CALENDAR_EVENT,
            itemId = entry.id,
            properties = mapOf(
                "action" to if (isNew) "create" else "update",
                "category" to entry.category.name.lowercase(),
            ),
        )
        viewModelScope.launch {
            savePersonalEvent(entry.toKmp())
            reminders.refresh()
        }
    }

    fun deletePersonal(entry: PersonalEntry) {
        analytics.selectContent(
            contentType = ContentTypes.CALENDAR_EVENT,
            itemId = entry.id,
            properties = mapOf("action" to "delete"),
        )
        viewModelScope.launch {
            deletePersonalEvent(entry.id)
            reminders.refresh()
        }
    }

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

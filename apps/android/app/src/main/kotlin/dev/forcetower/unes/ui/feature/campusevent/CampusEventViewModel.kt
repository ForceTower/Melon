package dev.forcetower.unes.ui.feature.campusevent

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEvent
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEventAudience
import dev.forcetower.melon.feature.campusevent.domain.usecase.ObserveCampusEventUseCase
import dev.forcetower.melon.feature.campusevent.domain.usecase.RefreshCampusEventUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

internal data class CampusEventUiState(
    val event: CampusEvent? = null,
    val filter: CampusEventAudience = CampusEventAudience.Everyone,
    val selectedDay: LocalDate? = null,
    val welcomeSeenEventId: String? = null,
    val welcomeLoaded: Boolean = false,
    val welcomeDismissed: Boolean = false,
    val isRefreshing: Boolean = false,
) : UiState {
    // `welcomeLoaded` keeps the reveal from flashing over the hub before the
    // persisted "already seen" id arrives from DataStore.
    val isShowingWelcome: Boolean
        get() = event != null && welcomeLoaded && !welcomeDismissed &&
            welcomeSeenEventId != event.id
}

internal sealed interface CampusEventIntent : UiIntent {
    data class DayTapped(val day: LocalDate) : CampusEventIntent
    data class FilterChanged(val filter: CampusEventAudience) : CampusEventIntent
    data object WelcomeContinueTapped : CampusEventIntent
    data object RefreshPulled : CampusEventIntent
}

internal sealed interface CampusEventEffect : UiEffect

// Shared across the hub and every detail push. Activity-scoped (resolved via
// plain `hiltViewModel()`, like Paradoxo), so all Nav3 entries read the same
// event snapshot and the per-second clock only ticks once.
@HiltViewModel
internal class CampusEventViewModel @Inject constructor(
    observeCampusEvent: ObserveCampusEventUseCase,
    private val refreshCampusEvent: RefreshCampusEventUseCase,
    private val welcomeStore: CampusEventWelcomeStore,
    private val analytics: Analytics,
) : MviViewModel<CampusEventUiState, CampusEventIntent, CampusEventEffect>(CampusEventUiState()) {

    init {
        viewModelScope.launch {
            observeCampusEvent().collect { event ->
                setState {
                    // Un-featuring (null) keeps the payload the screen was
                    // opened with rather than blanking an open hub — same
                    // rule as the iOS reducer.
                    val next = event ?: this.event
                    val dayStillValid = next != null && selectedDay != null &&
                        next.days().any { it.date == selectedDay }
                    copy(
                        event = next,
                        selectedDay = if (dayStillValid) {
                            selectedDay
                        } else {
                            next?.currentDayDate(Clock.System.now())
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            welcomeStore.seenEventId.collect { seen ->
                setState { copy(welcomeSeenEventId = seen, welcomeLoaded = true) }
            }
        }
        // No eager refresh here: this VM is also instantiated by the shell
        // (for the welcome chrome), and the gated fetch already runs in
        // `OverviewViewModel`. The hub refreshes via pull-to-refresh; the
        // per-second clock lives in the composables (`rememberCampusEventNow`),
        // like iOS `TimelineView`, so nothing ticks while no screen shows it.
    }

    override fun onIntent(intent: CampusEventIntent) {
        when (intent) {
            is CampusEventIntent.DayTapped -> {
                analytics.selectContent(
                    contentType = ContentTypes.TILE,
                    itemId = "schedule_day",
                    properties = mapOf("date" to intent.day.toString()),
                )
                setState { copy(selectedDay = intent.day) }
            }
            is CampusEventIntent.FilterChanged -> setState { copy(filter = intent.filter) }
            CampusEventIntent.WelcomeContinueTapped -> {
                val eventId = currentState.event?.id
                setState { copy(welcomeDismissed = true) }
                if (eventId != null) {
                    viewModelScope.launch { welcomeStore.markSeen(eventId) }
                }
            }
            CampusEventIntent.RefreshPulled -> {
                viewModelScope.launch {
                    setState { copy(isRefreshing = true) }
                    refreshCampusEvent()
                    setState { copy(isRefreshing = false) }
                }
            }
        }
    }

    fun trackHubOpen(hub: String) {
        analytics.selectContent(contentType = ContentTypes.HUB, itemId = hub)
    }

    fun trackActivityOpen(activityId: String) {
        analytics.selectContent(contentType = ContentTypes.ACTIVITY, itemId = activityId)
    }
}

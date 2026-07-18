package dev.forcetower.unes.ui.feature.connected

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import co.touchlab.kermit.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.Screens
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.sync.domain.usecase.BackfillMirrorUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.PingActivityUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.RefreshSessionUseCase
import dev.forcetower.unes.widgets.WidgetSnapshotPublisher
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

// Mirrors iOS `ConnectedView`'s sync orchestration: every entry into the
// authenticated shell — first launch, logout→login, background→foreground —
// fires `RefreshSession` to refresh profile + first-page messages and pull
// active-semester payloads (latest semester between terms) and `PingActivity`
// to bump `users.last_active_at` so the worker keeps the student on the hourly
// cadence tier. The once-per-session `BackfillMirror` runs on first appearance
// only; its own `backfillMirrorComplete` flag in SyncState makes subsequent
// calls cheap, but we still gate locally so re-foregrounding the app doesn't
// restart the poll-and-paginate work that's already done.
@HiltViewModel
internal class ConnectedViewModel @Inject constructor(
    private val refreshSession: RefreshSessionUseCase,
    private val backfillMirror: BackfillMirrorUseCase,
    private val pingActivity: PingActivityUseCase,
    private val widgetSnapshotPublisher: WidgetSnapshotPublisher,
    private val analytics: Analytics,
    deepLinkHandler: DeepLinkHandler,
    logger: Logger,
) : ViewModel() {
    // Deeplink targets buffered since the notification tap (or VIEW intent).
    // Surfaces here because the screen already holds this VM — the handler
    // itself stays an activity-agnostic singleton.
    val deepLinks = deepLinkHandler.targets

    private val log = logger.withTag("ConnectedViewModel")
    private val refreshMutex = Mutex()
    private val pingMutex = Mutex()
    private var backfillStarted = false
    private var lastReportedRoute: ConnectedRoute? = null

    // Called from the screen on every `Lifecycle.Event.ON_START` — covers
    // first composition AND background → foreground transitions in one path.
    fun onAppeared(reason: String = "onStart") {
        log.i { "Firing on appear" }
        viewModelScope.launch { runRefresh(reason) }
        viewModelScope.launch { runPing(reason) }
        if (!backfillStarted) {
            backfillStarted = true
            viewModelScope.launch { runBackfill() }
        }
        // Idempotent — `start()` short-circuits if the publisher's flows are
        // already subscribed. Mirrors iOS `ConnectedView`'s `.task` mounting
        // of `WidgetSnapshotPublisher`.
        widgetSnapshotPublisher.start()
    }

    private suspend fun runRefresh(reason: String) {
        if (!refreshMutex.tryLock()) {
            log.i { "skip session refresh — already in flight reason=$reason" }
            return
        }
        try {
            log.i { "refreshing session reason=$reason" }
            when (val outcome = refreshSession()) {
                is Outcome.Ok -> log.i { "session refresh ok reason=$reason" }
                is Outcome.Err -> log.w { "session refresh failed reason=$reason err=${outcome.error}" }
            }
        } finally {
            refreshMutex.unlock()
        }
    }

    private suspend fun runBackfill() {
        when (val outcome = backfillMirror()) {
            is Outcome.Ok -> log.i { "mirror backfill ok" }
            is Outcome.Err -> log.w { "mirror backfill failed err=${outcome.error}" }
        }
    }

    private suspend fun runPing(reason: String) {
        if (!pingMutex.tryLock()) {
            log.i { "skip ping — already in flight reason=$reason" }
            return
        }
        try {
            when (val outcome = pingActivity()) {
                is Outcome.Ok -> log.i { "ping ok reason=$reason" }
                is Outcome.Err -> log.w { "ping failed reason=$reason err=${outcome.error}" }
            }
        } finally {
            pingMutex.unlock()
        }
    }

    // Called by the shell whenever the top route of the active tab's stack
    // changes — initial mount, tab switch, push, and pop all funnel through
    // here. Dedupes consecutive repeats so an unrelated recomposition can't
    // double-fire the same screen_view.
    fun onRouteShown(route: NavKey) {
        val connectedRoute = route as? ConnectedRoute ?: return
        if (connectedRoute == lastReportedRoute) return
        lastReportedRoute = connectedRoute
        val (screen, properties) = connectedRoute.toScreenEvent()
        analytics.screen(screen, properties)
    }
}

private fun ConnectedRoute.toScreenEvent(): Pair<String, Map<String, Any>> = when (this) {
    ConnectedRoute.Overview -> Screens.OVERVIEW to emptyMap()
    ConnectedRoute.Schedule -> Screens.SCHEDULE to emptyMap()
    ConnectedRoute.Classes -> Screens.DISCIPLINES to emptyMap()
    is ConnectedRoute.DisciplineDetail -> Screens.DISCIPLINE_DETAIL to mapOf("offer_id" to offerId)
    ConnectedRoute.MessagesList -> Screens.MESSAGES to emptyMap()
    is ConnectedRoute.MessageDetail -> Screens.MESSAGE_DETAIL to mapOf("message_id" to id)
    ConnectedRoute.Me -> Screens.ME to emptyMap()
    ConnectedRoute.Settings -> Screens.SETTINGS to emptyMap()
    ConnectedRoute.Passkeys -> Screens.PASSKEYS to emptyMap()
    ConnectedRoute.Calendar -> Screens.CALENDAR to emptyMap()
    is ConnectedRoute.FinalCountdown ->
        Screens.FINAL_COUNTDOWN to (offerId?.let { mapOf("offer_id" to it) } ?: emptyMap())
    ConnectedRoute.Licenses -> Screens.LICENSES to emptyMap()
    ConnectedRoute.Paradoxo -> Screens.PARADOXO to emptyMap()
    ConnectedRoute.CampusEvent -> Screens.CAMPUS_EVENT to emptyMap()
    is ConnectedRoute.CampusEventActivity -> Screens.CAMPUS_EVENT_ACTIVITY to mapOf("activity_id" to id)
    ConnectedRoute.CampusEventSpeakers -> Screens.CAMPUS_EVENT_SPEAKERS to emptyMap()
    ConnectedRoute.CampusEventWorkshops -> Screens.CAMPUS_EVENT_WORKSHOPS to emptyMap()
    ConnectedRoute.CampusEventVenues -> Screens.CAMPUS_EVENT_VENUES to emptyMap()
    ConnectedRoute.CampusEventOrganizations -> Screens.CAMPUS_EVENT_ORGANIZATIONS to emptyMap()
    is ConnectedRoute.ParadoxoDiscipline -> Screens.PARADOXO_DISCIPLINE to mapOf("entity_id" to id)
    is ConnectedRoute.ParadoxoTeacher -> Screens.PARADOXO_TEACHER to mapOf("entity_id" to id)
    is ConnectedRoute.ParadoxoExplore -> Screens.PARADOXO_EXPLORE to mapOf("kind" to kind)
    ConnectedRoute.Materials -> Screens.MATERIALS to emptyMap()
    is ConnectedRoute.MaterialsDiscipline -> Screens.MATERIALS_DISCIPLINE to mapOf("discipline_id" to disciplineId)
    is ConnectedRoute.MaterialsDetail -> Screens.MATERIALS_DETAIL to mapOf("material_id" to materialId)
    ConnectedRoute.MaterialsSaved -> Screens.MATERIALS_SAVED to emptyMap()
    ConnectedRoute.Enrollment -> Screens.ENROLLMENT to emptyMap()
    ConnectedRoute.EnrollmentOffers -> Screens.ENROLLMENT_OFFERS to emptyMap()
    is ConnectedRoute.EnrollmentDiscipline -> Screens.ENROLLMENT_DISCIPLINE to mapOf("offer_id" to id)
    ConnectedRoute.EnrollmentTimetable -> Screens.ENROLLMENT_TIMETABLE to emptyMap()
    ConnectedRoute.EnrollmentReview -> Screens.ENROLLMENT_REVIEW to emptyMap()
    ConnectedRoute.EnrollmentSuccess -> Screens.ENROLLMENT_SUCCESS to emptyMap()
}

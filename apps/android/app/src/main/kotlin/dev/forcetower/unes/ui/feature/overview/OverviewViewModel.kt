package dev.forcetower.unes.ui.feature.overview

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import androidx.annotation.StringRes
import dev.forcetower.melon.core.common.ForegroundSignal
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.me.domain.model.ReauthError
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.campusevent.domain.model.CampusEvent
import dev.forcetower.melon.feature.campusevent.domain.usecase.ObserveCampusEventUseCase
import dev.forcetower.melon.feature.campusevent.domain.usecase.RefreshCampusEventUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ReauthenticateUpstreamUseCase
import dev.forcetower.melon.feature.me.domain.usecase.RefreshCredentialStatusUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNextTestTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNowClassUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveOverviewHeaderUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveTodayTimelineUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveTomorrowPreviewUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveUnreadMessagesTileUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import dev.forcetower.unes.R
import dev.forcetower.unes.firebase.FeatureFlags
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import dev.forcetower.melon.feature.overview.domain.model.OverviewClassState as KmpOverviewClassState
import dev.forcetower.melon.feature.overview.domain.model.OverviewMessagesTile as KmpOverviewMessagesTile
import dev.forcetower.melon.feature.overview.domain.model.OverviewNextTestTile as KmpOverviewNextTestTile
import dev.forcetower.melon.feature.overview.domain.model.OverviewNowClass as KmpOverviewNowClass
import dev.forcetower.melon.feature.overview.domain.model.OverviewTodayItem as KmpOverviewTodayItem
import dev.forcetower.melon.feature.overview.domain.model.OverviewTomorrowPreview as KmpOverviewTomorrowPreview

internal enum class GreetingKind { Morning, Afternoon, Evening }

internal data class OverviewUiState(
    val userName: String? = null,
    val courseName: String? = null,
    val nowRaw: KmpOverviewNowClass? = null,
    val todayRaw: List<KmpOverviewTodayItem> = emptyList(),
    val tomorrowRaw: KmpOverviewTomorrowPreview? = null,
    val semesterCode: String? = null,
    val semesterEndIso: String? = null,
    val messagesTileRaw: KmpOverviewMessagesTile? = null,
    val nextTestTileRaw: KmpOverviewNextTestTile? = null,
    val campusEventRaw: CampusEvent? = null,
    val campusEventEnabled: Boolean = false,
    /** Set once the refresh token is spent — nothing syncs until a new login. */
    val sessionInvalid: Boolean = false,
    /** Set when the server reports the stored portal password stopped working. */
    val credentialsInvalid: Boolean = false,
    val upstreamUsername: String? = null,
    val reauthLoading: Boolean = false,
    @StringRes val reauthErrorRes: Int? = null,
    val clock: Instant = Clock.System.now(),
) : UiState {
    // Both gates must open, exactly like iOS: the Remote Config flag AND a
    // non-null featured event from the server.
    val campusEvent: CampusEvent?
        get() = campusEventRaw.takeIf { campusEventEnabled }

    val firstName: String?
        get() = userName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }

    // "MA" — first letter of the first and last name parts, mirroring the
    // design's two-letter monogram. Single-word names fall back to one letter.
    val avatarInitials: String?
        get() {
            val parts = userName?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() }
            if (parts.isNullOrEmpty()) return null
            val first = parts.first().first()
            val last = parts.drop(1).lastOrNull()?.first()
            return listOfNotNull(first, last).joinToString("").uppercase()
        }

    val greetingKind: GreetingKind
        get() {
            val hour = clock.toLocalDateTime(TimeZone.currentSystemDefault()).hour
            return when {
                hour < 12 -> GreetingKind.Morning
                hour < 18 -> GreetingKind.Afternoon
                else -> GreetingKind.Evening
            }
        }

    // "Ter, 11 mar" — rendered uppercase by the header per the design spec.
    val dateEyebrow: String
        get() = formatDayEyebrow(localDate(clock))

    // Weekday name for the "Seu dia" summary ("Terça · 4 aulas").
    val weekdayLabel: String
        get() = formatWeekday(localDate(clock))

    // Tomorrow's eyebrow inside the day-done hero ("Qua, 12 mar").
    val tomorrowEyebrow: String
        get() = formatDayEyebrow(localDate(clock).plusDays(1))

    // Days until the active semester ends — the "Reta final" countdown. Null
    // until the profile lands or once the end date has passed.
    val semesterDaysLeft: Int?
        get() {
            val endIso = semesterEndIso ?: return null
            val end = runCatching { LocalDate.parse(endIso.take(10)) }.getOrNull() ?: return null
            val days = ChronoUnit.DAYS.between(localDate(clock), end)
            return if (days >= 0) days.toInt() else null
        }
}

internal sealed interface OverviewIntent : UiIntent {
    data object ReloginTapped : OverviewIntent
    data object ReauthTapped : OverviewIntent
    data class ReauthSubmitted(val password: String) : OverviewIntent
    data object ReauthDismissed : OverviewIntent
}

internal sealed interface OverviewEffect : UiEffect {
    data object ShowRelogin : OverviewEffect
    data object ShowReauth : OverviewEffect
    data object ReauthSucceeded : OverviewEffect
}

@HiltViewModel
internal class OverviewViewModel @Inject constructor(
    observeHeader: ObserveOverviewHeaderUseCase,
    observeMeProfile: ObserveMeProfileUseCase,
    observeNowClass: ObserveNowClassUseCase,
    observeToday: ObserveTodayTimelineUseCase,
    observeTomorrow: ObserveTomorrowPreviewUseCase,
    observeMessagesTile: ObserveUnreadMessagesTileUseCase,
    observeNextTestTile: ObserveNextTestTileUseCase,
    observeCampusEvent: ObserveCampusEventUseCase,
    refreshCampusEvent: RefreshCampusEventUseCase,
    featureFlags: FeatureFlags,
    foregroundSignal: ForegroundSignal,
    sessionStore: SessionStore,
    private val refreshCredentialStatus: RefreshCredentialStatusUseCase,
    private val reauthenticateUpstream: ReauthenticateUpstreamUseCase,
    private val analytics: Analytics,
) : MviViewModel<OverviewUiState, OverviewIntent, OverviewEffect>(OverviewUiState()) {

    init {
        viewModelScope.launch {
            observeHeader().collect { header -> setState { copy(userName = header?.userName) } }
        }
        viewModelScope.launch {
            observeMeProfile().collect { profile ->
                setState {
                    copy(
                        courseName = profile.identity.courseName,
                        semesterCode = profile.semester?.code,
                        semesterEndIso = profile.semester?.endDate,
                    )
                }
            }
        }
        viewModelScope.launch {
            observeNowClass().collect { value -> setState { copy(nowRaw = value) } }
        }
        viewModelScope.launch {
            observeToday().collect { value -> setState { copy(todayRaw = value) } }
        }
        viewModelScope.launch {
            observeTomorrow().collect { value -> setState { copy(tomorrowRaw = value) } }
        }
        viewModelScope.launch {
            observeMessagesTile().collect { value -> setState { copy(messagesTileRaw = value) } }
        }
        viewModelScope.launch {
            observeNextTestTile().collect { value -> setState { copy(nextTestTileRaw = value) } }
        }
        viewModelScope.launch {
            observeCampusEvent().collect { value -> setState { copy(campusEventRaw = value) } }
        }
        // The refresh only fires while the flag is on, mirroring iOS
        // `HomeFeature.refreshCampusEvent`; failures keep the stale snapshot.
        viewModelScope.launch {
            featureFlags.gates
                .map { it.campusEvent }
                .distinctUntilChanged()
                .collect { enabled ->
                    setState { copy(campusEventEnabled = enabled) }
                    if (enabled) refreshCampusEvent()
                }
        }
        // Re-fetch on background → foreground so mid-event edits land without
        // a pull — the campus analogue of the ConnectedViewModel foreground
        // sync; the result flows in through the observe stream above.
        viewModelScope.launch {
            foregroundSignal.pulses.collect {
                if (currentState.campusEventEnabled) refreshCampusEvent()
            }
        }
        viewModelScope.launch {
            sessionStore.sessionInvalid.collect { invalid -> setState { copy(sessionInvalid = invalid) } }
        }
        viewModelScope.launch {
            sessionStore.credentialsInvalid.collect { invalid -> setState { copy(credentialsInvalid = invalid) } }
        }
        viewModelScope.launch {
            sessionStore.upstreamUsername.collect { name -> setState { copy(upstreamUsername = name) } }
        }
        // Polled on the same foreground pulse as the campus-event refresh, so
        // the banner clears on its own when the password is fixed elsewhere.
        viewModelScope.launch { refreshCredentialStatus() }
        viewModelScope.launch {
            foregroundSignal.pulses.collect { refreshCredentialStatus() }
        }
        // Clock ticker — refreshes greeting/eyebrow/countdown labels without
        // forcing KMP flows to re-emit. Mirrors iOS `runClockTicker`.
        viewModelScope.launch {
            while (isActive) {
                setState { copy(clock = Clock.System.now()) }
                delay(CLOCK_TICK_MS.milliseconds)
            }
        }
    }

    override fun onIntent(intent: OverviewIntent) {
        when (intent) {
            OverviewIntent.ReloginTapped -> {
                analytics.selectContent(contentType = ContentTypes.CTA, itemId = "session_expired")
                emitEffect(OverviewEffect.ShowRelogin)
            }
            OverviewIntent.ReauthTapped -> {
                analytics.selectContent(contentType = ContentTypes.CTA, itemId = "credentials_invalid")
                setState { copy(reauthErrorRes = null) }
                emitEffect(OverviewEffect.ShowReauth)
            }
            OverviewIntent.ReauthDismissed -> setState { copy(reauthErrorRes = null, reauthLoading = false) }
            is OverviewIntent.ReauthSubmitted -> submitReauth(intent.password)
        }
    }

    private fun submitReauth(password: String) {
        if (currentState.reauthLoading) return
        setState { copy(reauthLoading = true, reauthErrorRes = null) }
        viewModelScope.launch {
            when (val outcome = reauthenticateUpstream(password)) {
                is Outcome.Ok -> {
                    // The use case already cleared the flag, which drops the
                    // banner; the server brought the sync forward.
                    setState { copy(reauthLoading = false) }
                    emitEffect(OverviewEffect.ReauthSucceeded)
                }
                is Outcome.Err -> setState {
                    copy(reauthLoading = false, reauthErrorRes = outcome.error.toMessageRes())
                }
            }
        }
    }

    fun trackNowClassTap(offerId: String?) {
        analytics.selectContent(
            contentType = ContentTypes.TILE,
            itemId = "now_class",
            properties = offerId?.let { mapOf("offer_id" to it) } ?: emptyMap(),
        )
    }

    fun trackDisciplineTap(offerId: String?) {
        if (offerId == null) return
        analytics.selectContent(contentType = ContentTypes.DISCIPLINE, itemId = offerId)
    }

    fun trackScheduleTileTap() {
        analytics.selectContent(contentType = ContentTypes.TILE, itemId = "schedule")
    }

    fun trackMessagesTileTap() {
        analytics.selectContent(contentType = ContentTypes.TILE, itemId = "unread_messages")
    }

    fun trackCampusEventTap() {
        analytics.selectContent(contentType = ContentTypes.HUB, itemId = "campus_event")
    }

    private companion object {
        const val CLOCK_TICK_MS = 30_000L
    }
}

private fun localDate(now: Instant): LocalDate =
    java.time.Instant.ofEpochMilli(now.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

// "ter, 11 mar" in pt-BR — device-locale formatted; the header renders it
// uppercase ("TER, 11 MAR") per the design. Abbreviation dots are dropped to
// match the design's compact eyebrow.
private fun formatDayEyebrow(date: LocalDate): String =
    DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
        .format(date)
        .replace(".", "")

// "Terça-feira" — capitalized full weekday for the "Seu dia" summary.
private fun formatWeekday(date: LocalDate): String =
    DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
        .format(date)
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

private fun ReauthError.toMessageRes(): Int = when (this) {
    ReauthError.InvalidPassword -> R.string.reauth_error_invalid
    ReauthError.Unavailable -> R.string.reauth_error_unavailable
    ReauthError.NoConnection -> R.string.reauth_error_network
    is ReauthError.Server -> R.string.reauth_error_unavailable
}

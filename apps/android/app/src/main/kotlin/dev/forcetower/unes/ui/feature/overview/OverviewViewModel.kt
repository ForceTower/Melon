package dev.forcetower.unes.ui.feature.overview

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveAttendanceTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveDisciplinesUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveGradeTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveLastSyncUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNextTestTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNowClassUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveOverviewHeaderUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveTodayTimelineUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveUnreadMessagesTileUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import dev.forcetower.melon.feature.overview.domain.model.OverviewAttendanceTile as KmpOverviewAttendanceTile
import dev.forcetower.melon.feature.overview.domain.model.OverviewDiscipline as KmpOverviewDiscipline
import dev.forcetower.melon.feature.overview.domain.model.OverviewGradeTile as KmpOverviewGradeTile
import dev.forcetower.melon.feature.overview.domain.model.OverviewMessagesTile as KmpOverviewMessagesTile
import dev.forcetower.melon.feature.overview.domain.model.OverviewNextTestTile as KmpOverviewNextTestTile
import dev.forcetower.melon.feature.overview.domain.model.OverviewNowClass as KmpOverviewNowClass
import dev.forcetower.melon.feature.overview.domain.model.OverviewTodayItem as KmpOverviewTodayItem

internal enum class GreetingKind { Morning, Afternoon, Evening }

internal sealed interface LastUpdatedKind {
    data object Syncing : LastUpdatedKind
    data object JustNow : LastUpdatedKind
    data class Minutes(val n: Int) : LastUpdatedKind
    data class Hours(val n: Int) : LastUpdatedKind
    data class Days(val n: Int) : LastUpdatedKind
}

internal data class OverviewUiState(
    val userName: String? = null,
    val nowRaw: KmpOverviewNowClass? = null,
    val todayRaw: List<KmpOverviewTodayItem> = emptyList(),
    val disciplinesRaw: List<KmpOverviewDiscipline> = emptyList(),
    val semesterLabel: String = "",
    val messagesTileRaw: KmpOverviewMessagesTile? = null,
    val nextTestTileRaw: KmpOverviewNextTestTile? = null,
    val attendanceTileRaw: KmpOverviewAttendanceTile? = null,
    val gradeTileRaw: KmpOverviewGradeTile? = null,
    val lastSyncIso: String? = null,
    val clock: Instant = Clock.System.now(),
) : UiState {
    val firstName: String?
        get() = userName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }

    val avatarInitial: String?
        get() = userName?.trim()?.firstOrNull()?.uppercase()

    val greetingKind: GreetingKind
        get() {
            val hour = clock.toLocalDateTime(TimeZone.currentSystemDefault()).hour
            return when {
                hour < 12 -> GreetingKind.Morning
                hour < 18 -> GreetingKind.Afternoon
                else -> GreetingKind.Evening
            }
        }

    val dateEyebrow: String
        get() = formatEyebrow(clock)

    val lastUpdatedKind: LastUpdatedKind
        get() = computeLastUpdated(lastSyncIso, clock)
}

internal sealed interface OverviewIntent : UiIntent
internal sealed interface OverviewEffect : UiEffect

@HiltViewModel
internal class OverviewViewModel @Inject constructor(
    observeHeader: ObserveOverviewHeaderUseCase,
    observeNowClass: ObserveNowClassUseCase,
    observeToday: ObserveTodayTimelineUseCase,
    observeDisciplines: ObserveDisciplinesUseCase,
    observeMessagesTile: ObserveUnreadMessagesTileUseCase,
    observeNextTestTile: ObserveNextTestTileUseCase,
    observeAttendanceTile: ObserveAttendanceTileUseCase,
    observeGradeTile: ObserveGradeTileUseCase,
    observeLastSync: ObserveLastSyncUseCase,
) : MviViewModel<OverviewUiState, OverviewIntent, OverviewEffect>(OverviewUiState()) {

    init {
        viewModelScope.launch {
            observeHeader().collect { header -> setState { copy(userName = header?.userName) } }
        }
        viewModelScope.launch {
            observeNowClass().collect { value -> setState { copy(nowRaw = value) } }
        }
        viewModelScope.launch {
            observeToday().collect { value -> setState { copy(todayRaw = value) } }
        }
        viewModelScope.launch {
            observeDisciplines().collect { value ->
                setState {
                    copy(
                        disciplinesRaw = value,
                        semesterLabel = value.firstOrNull()?.semesterCode ?: semesterLabel,
                    )
                }
            }
        }
        viewModelScope.launch {
            observeMessagesTile().collect { value -> setState { copy(messagesTileRaw = value) } }
        }
        viewModelScope.launch {
            observeNextTestTile().collect { value -> setState { copy(nextTestTileRaw = value) } }
        }
        viewModelScope.launch {
            observeAttendanceTile().collect { value -> setState { copy(attendanceTileRaw = value) } }
        }
        viewModelScope.launch {
            observeGradeTile().collect { value -> setState { copy(gradeTileRaw = value) } }
        }
        viewModelScope.launch {
            observeLastSync().collect { value -> setState { copy(lastSyncIso = value) } }
        }
        // Clock ticker — refreshes greeting/eyebrow/last-updated labels without
        // forcing KMP flows to re-emit. Mirrors iOS `runClockTicker`.
        viewModelScope.launch {
            while (isActive) {
                setState { copy(clock = Clock.System.now()) }
                delay(CLOCK_TICK_MS)
            }
        }
    }

    override fun onIntent(intent: OverviewIntent) = Unit

    private companion object {
        const val CLOCK_TICK_MS = 30_000L
    }
}

private val PtBr: Locale = Locale.forLanguageTag("pt-BR")

// "sábado · 26 abr" — pt-BR locale, weekday-feira suffix and trailing dot
// stripped, lowercased to match iOS `eyebrowFormatter` post-processing.
private val EyebrowFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE · d MMM", PtBr)

private fun formatEyebrow(now: Instant): String {
    val zoned = java.time.Instant.ofEpochMilli(now.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
    return EyebrowFormatter.format(zoned)
        .replace("-feira", "")
        .replace(".", "")
        .lowercase(PtBr)
}

private fun computeLastUpdated(iso: String?, now: Instant): LastUpdatedKind {
    if (iso.isNullOrBlank()) return LastUpdatedKind.Syncing
    val parsed = runCatching { Instant.parse(iso) }.getOrNull() ?: return LastUpdatedKind.Syncing
    val seconds = max(0L, now.epochSeconds - parsed.epochSeconds)
    val minutes = (seconds / 60).toInt()
    if (minutes < 1) return LastUpdatedKind.JustNow
    if (minutes < 60) return LastUpdatedKind.Minutes(minutes)
    val hours = minutes / 60
    if (hours < 24) return LastUpdatedKind.Hours(hours)
    return LastUpdatedKind.Days(hours / 24)
}

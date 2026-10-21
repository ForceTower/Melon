package dev.forcetower.unes.ui.feature.commute

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.AppClock
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.transit.domain.model.CommuteLeg
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitStopBoard
import dev.forcetower.melon.feature.transit.domain.model.TransitVehiclePhase
import dev.forcetower.melon.feature.transit.domain.usecase.GetStopArrivalsUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.ObserveCommutePlanUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.RefreshCommutePlanUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import dev.forcetower.unes.remote.FeatureFlags
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// The single line under the Hoje hero class: when the student's bus passes
// their stop. Polls only while Hoje is on screen. Mirrors iOS
// `CommuteHeroFeature`.
internal data class CommuteHeroUiState(
    // The `enable_commute` gate.
    val enabled: Boolean = false,
    // The student's own switch in Configurações for those who don't take the bus.
    val onHome: Boolean = true,
    val plan: CommutePlan = CommutePlan(),
    val schedule: CommuteDaySchedule = CommuteDaySchedule(),
    val direction: TransitDirection? = null,
    val board: TransitStopBoard? = null,
    // When the board arrived on this device, so a dead connection can't keep
    // a countdown running.
    val boardReceivedAt: Instant? = null,
    val loadFailed: Boolean = false,
) : UiState {
    val isShown: Boolean get() = enabled && onHome

    val leg: CommuteLeg? get() = direction?.let(plan::leg)

    fun isOutdated(now: Instant): Boolean = CommuteFormat.isOutdated(boardReceivedAt, now)

    // The next arrival worth a glance, skipping buses already gone.
    fun headline(now: Instant): TransitArrival? {
        val outdated = isOutdated(now)
        return board?.arrivals?.firstOrNull { arrival ->
            arrival.latest > now || (arrival.phase == TransitVehiclePhase.AtStop && !outdated)
        }
    }
}

internal sealed interface CommuteHeroIntent : UiIntent {
    data class ScheduleChanged(
        val schedule: CommuteDaySchedule,
    ) : CommuteHeroIntent
    data object Start : CommuteHeroIntent
    data object Stop : CommuteHeroIntent
}

internal sealed interface CommuteHeroEffect : UiEffect

@HiltViewModel
internal class CommuteHeroViewModel @Inject constructor(
    observeCommutePlan: ObserveCommutePlanUseCase,
    private val refreshCommutePlan: RefreshCommutePlanUseCase,
    private val getStopArrivals: GetStopArrivalsUseCase,
    featureFlags: FeatureFlags,
    preferences: CommutePreferenceStore,
    private val clock: AppClock,
    private val analytics: Analytics,
) : MviViewModel<CommuteHeroUiState, CommuteHeroIntent, CommuteHeroEffect>(CommuteHeroUiState()) {
    private var pollJob: Job? = null
    private var polling = false

    init {
        viewModelScope.launch {
            featureFlags.gates.map { it.commute }.distinctUntilChanged().collect { enabled ->
                setState { copy(enabled = enabled) }
                restartPolling()
            }
        }
        viewModelScope.launch {
            preferences.onHome.collect { onHome ->
                setState { copy(onHome = onHome) }
                restartPolling()
            }
        }
        viewModelScope.launch {
            observeCommutePlan().collect { plan -> apply(plan) }
        }
    }

    override fun onIntent(intent: CommuteHeroIntent) {
        when (intent) {
            is CommuteHeroIntent.ScheduleChanged -> {
                if (intent.schedule == currentState.schedule) return
                setState { copy(schedule = intent.schedule) }
                restartPolling()
            }
            CommuteHeroIntent.Start -> {
                polling = true
                // Another device may have changed the plan; the answer lands
                // through the observation, and a failure keeps the mirror.
                if (currentState.isShown) viewModelScope.launch { refreshCommutePlan() }
                restartPolling()
            }
            CommuteHeroIntent.Stop -> {
                polling = false
                restartPolling()
            }
        }
    }

    fun trackOpenBoard() {
        analytics.selectContent(ContentTypes.TILE, "commute")
    }

    fun trackOpenSetup() {
        analytics.selectContent(ContentTypes.CTA, "commute_setup")
    }

    // The only way a plan enters the hero, so a board never outlives the
    // stop and lines it was asked for.
    private fun apply(plan: CommutePlan) {
        if (plan == currentState.plan) return
        setState {
            copy(
                plan = plan,
                board = null,
                boardReceivedAt = null,
                loadFailed = false,
                direction = if (plan.isEmpty) null else direction,
            )
        }
        restartPolling()
    }

    // One loop for as long as Hoje is visible; each tick re-resolves the leg,
    // so the line flips from going in to going home on its own.
    private fun restartPolling() {
        pollJob?.cancel()
        pollJob = null
        val state = currentState
        if (!polling || !state.isShown || state.plan.isEmpty) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                val plan = currentState.plan
                val now = CommuteFormat.local(clock.now(), ZoneId.of(clock.timeZone.id)).toLocalTime()
                val direction = currentState.schedule.direction(plan, now)
                if (direction != currentState.direction) {
                    setState { copy(direction = direction, board = null, boardReceivedAt = null, loadFailed = false) }
                }
                val leg = direction?.let(plan::leg)
                if (direction != null && leg != null) {
                    when (val outcome = getStopArrivals(leg.stopId, direction, leg.lineNumbers)) {
                        is Outcome.Ok -> setState {
                            if (direction != this.direction || leg.stopId != this.leg?.stopId) {
                                this
                            } else {
                                copy(board = outcome.value, boardReceivedAt = clock.now(), loadFailed = false)
                            }
                        }
                        is Outcome.Err -> setState { copy(loadFailed = true) }
                    }
                }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 30_000L
    }
}

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
import dev.forcetower.melon.feature.transit.domain.model.TransitNetwork
import dev.forcetower.melon.feature.transit.domain.model.TransitStopBoard
import dev.forcetower.melon.feature.transit.domain.usecase.GetStopArrivalsUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.GetTransitNetworkUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.ObserveCommutePlanUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.RefreshCommutePlanUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// "Meu trajeto": the saved stop's next buses ("Meu ponto" on the way in,
// "Saindo da UEFS" on the way out), polled while on screen. Mirrors iOS
// `CommuteBoardFeature`.
internal data class CommuteBoardUiState(
    val plan: CommutePlan = CommutePlan(),
    // The mirror answered once — the screen can tell "no plan yet" from
    // "not loaded yet" and open on the intro without flashing the board.
    val planLoaded: Boolean = false,
    // Null until the time of day picks the leg for a route that named none.
    val direction: TransitDirection? = null,
    val board: TransitStopBoard? = null,
    // When the board arrived on this device, so a dead connection can't keep
    // a countdown running.
    val boardReceivedAt: Instant? = null,
    val network: TransitNetwork? = null,
    val loadFailed: Boolean = false,
    // The arrival "Como calculamos" explains.
    val explained: TransitArrival? = null,
    val explainerOpen: Boolean = false,
) : UiState {
    val leg: CommuteLeg? get() = direction?.let(plan::leg)

    fun isOutdated(now: Instant): Boolean = CommuteFormat.isOutdated(boardReceivedAt, now)
}

internal sealed interface CommuteBoardIntent : UiIntent {
    // Entering the route: `direction` null lets the time of day pick the leg.
    data class Open(
        val direction: TransitDirection?,
    ) : CommuteBoardIntent

    // The screen is (no longer) on display — polling follows it.
    data object Start : CommuteBoardIntent
    data object Stop : CommuteBoardIntent
    data class DirectionPicked(
        val direction: TransitDirection,
    ) : CommuteBoardIntent
    data class ExplainTapped(
        val arrival: TransitArrival?,
    ) : CommuteBoardIntent
    data object ExplainerDismissed : CommuteBoardIntent

    // The route left the stack (the flow closed over it): the next `Open` is
    // a fresh entry, not a pop back.
    data object Closed : CommuteBoardIntent
}

internal sealed interface CommuteBoardEffect : UiEffect

@HiltViewModel
internal class CommuteBoardViewModel @Inject constructor(
    observeCommutePlan: ObserveCommutePlanUseCase,
    private val refreshCommutePlan: RefreshCommutePlanUseCase,
    private val getTransitNetwork: GetTransitNetworkUseCase,
    private val getStopArrivals: GetStopArrivalsUseCase,
    private val clock: AppClock,
    private val analytics: Analytics,
) : MviViewModel<CommuteBoardUiState, CommuteBoardIntent, CommuteBoardEffect>(CommuteBoardUiState()) {
    private var pollJob: Job? = null
    private var polling = false
    private var autoDirection = false

    // What the route last opened with: the same route re-entering (a pop back
    // from the follow screen) keeps whatever direction the student picked
    // since, a different one is a fresh entry.
    private var openedWith: TransitDirection? = null
    private var opened = false

    init {
        viewModelScope.launch {
            observeCommutePlan().collect { plan -> onPlan(plan) }
        }
    }

    override fun onIntent(intent: CommuteBoardIntent) {
        when (intent) {
            is CommuteBoardIntent.Open -> open(intent.direction)
            CommuteBoardIntent.Start -> {
                polling = true
                restartPolling()
            }
            CommuteBoardIntent.Stop -> {
                polling = false
                restartPolling()
            }
            is CommuteBoardIntent.DirectionPicked -> {
                if (intent.direction == currentState.direction) return
                autoDirection = false
                setState {
                    copy(direction = intent.direction, board = null, boardReceivedAt = null, loadFailed = false)
                }
                restartPolling()
            }
            is CommuteBoardIntent.ExplainTapped -> setState {
                val arrival = intent.arrival
                    ?: board?.arrivals?.firstOrNull { it.basis != null }
                    ?: board?.arrivals?.firstOrNull()
                copy(explained = arrival, explainerOpen = true)
            }
            CommuteBoardIntent.ExplainerDismissed -> setState { copy(explainerOpen = false) }
            CommuteBoardIntent.Closed -> {
                opened = false
                openedWith = null
            }
        }
    }

    fun trackFollowTap() {
        analytics.selectContent(ContentTypes.TILE, "commute_follow")
    }

    fun trackSetupTap() {
        analytics.selectContent(ContentTypes.CTA, "commute_setup")
    }

    // Coming back from the follow screen lands on the same leg, so the board
    // the student just saw stays put until the next poll replaces it.
    private fun open(direction: TransitDirection?) {
        val reentry = opened && direction == openedWith
        opened = true
        openedWith = direction
        if (!reentry) autoDirection = direction == null
        setState {
            val target = if (reentry) this.direction else direction ?: resolveDirection(plan).takeIf { planLoaded }
            val same = target == this.direction
            copy(
                direction = target,
                board = if (same) board else null,
                boardReceivedAt = if (same) boardReceivedAt else null,
                loadFailed = false,
                explainerOpen = false,
            )
        }
        // Another device may have changed the plan; the answer lands through
        // the observation, and a failure leaves the mirrored plan in place.
        viewModelScope.launch { refreshCommutePlan() }
        viewModelScope.launch {
            val outcome = getTransitNetwork()
            if (outcome is Outcome.Ok) setState { copy(network = outcome.value) }
        }
        restartPolling()
    }

    private fun onPlan(plan: CommutePlan) {
        val previousLeg = currentState.leg
        setState {
            // A leg edited away, here or on another device, hands the board
            // to the plan's own direction.
            val gone = direction != null && !plan.isEmpty && plan.leg(direction) == null
            val next = if (autoDirection || gone) resolveDirection(plan) else direction
            copy(plan = plan, planLoaded = true, direction = next)
        }
        if (currentState.leg != previousLeg) {
            setState { copy(board = null, boardReceivedAt = null) }
            restartPolling()
        }
    }

    // A plan with both legs follows the clock (noon splits the day here, the
    // hero knows the first class); one leg is simply that leg.
    private fun resolveDirection(plan: CommutePlan): TransitDirection? {
        val now = CommuteFormat.local(clock.now(), ZoneId.of(clock.timeZone.id)).toLocalTime()
        return CommuteDaySchedule().direction(plan, now) ?: TransitDirection.ToCampus
    }

    // One loop for as long as the board is on display, tied to the stop and
    // direction it was started for: a board for another leg is dropped.
    private fun restartPolling() {
        pollJob?.cancel()
        pollJob = null
        if (!polling) return
        val direction = currentState.direction ?: return
        val leg = currentState.leg ?: return
        pollJob = viewModelScope.launch {
            while (isActive) {
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
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 20_000L
    }
}

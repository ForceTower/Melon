package dev.forcetower.unes.ui.feature.commute

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.common.AppClock
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.transit.domain.model.GeoPoint
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitFollow
import dev.forcetower.melon.feature.transit.domain.model.TransitRoute
import dev.forcetower.melon.feature.transit.domain.model.TransitVehicle
import dev.forcetower.melon.feature.transit.domain.usecase.FollowTransitArrivalUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.GetTransitNetworkUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// "Seguindo o 087": only this line's route and buses, the followed
// departure's band to the student's stop, refreshed while on screen.
// Following a bus never means the student is on it. Mirrors iOS
// `CommuteFollowFeature`.
internal data class CommuteFollowUiState(
    val lineNumber: String = "",
    val direction: TransitDirection = TransitDirection.ToCampus,
    val stopId: String = "",
    // The departure being followed; stays the same as it goes from history
    // to a bus waiting or on its way.
    val arrivalId: String = "",
    val vehicleId: String? = null,
    // What the board showed, so the screen opens with a number.
    val arrival: TransitArrival? = null,
    val follow: TransitFollow? = null,
    // When the data on screen arrived on this device: the board's snapshot
    // until the first answer here replaces it.
    val followReceivedAt: Instant? = null,
    val route: TransitRoute? = null,
    val loadFailed: Boolean = false,
    val userLocation: GeoPoint? = null,
    val locating: Boolean = false,
    val locationDenied: Boolean = false,
    val explainerOpen: Boolean = false,
) : UiState {
    val showsMyLocation: Boolean get() = userLocation != null

    val followedVehicle: TransitVehicle?
        get() = follow?.vehicles?.firstOrNull { it.id == vehicleId }

    // Buses running this route, plus the followed one even while it is
    // still finishing the opposite trip.
    val routeVehicles: List<TransitVehicle>
        get() = follow?.vehicles?.filter { it.progressMeters != null || it.id == vehicleId }.orEmpty()

    fun isOutdated(now: Instant): Boolean = CommuteFormat.isOutdated(followReceivedAt, now)
}

internal sealed interface CommuteFollowIntent : UiIntent {
    // Handed over by the board before the push, so the first frame has the
    // estimate the student tapped. `receivedAt` is when that board landed.
    data class Seed(
        val arrival: TransitArrival,
        val direction: TransitDirection,
        val stopId: String,
        val receivedAt: Instant,
    ) : CommuteFollowIntent

    // Entering the route; keeps the seed when it is the same departure and
    // starts clean otherwise (a restored stack after process death).
    data class Open(
        val lineNumber: String,
        val direction: TransitDirection,
        val stopId: String,
        val arrivalId: String,
        val vehicleId: String?,
    ) : CommuteFollowIntent
    data object Start : CommuteFollowIntent
    data object Stop : CommuteFollowIntent

    // The student allowed location and asked to see themselves on the map.
    data object LocateMe : CommuteFollowIntent
    data object HideMyLocation : CommuteFollowIntent
    data object LocationDenied : CommuteFollowIntent
    data object ExplainTapped : CommuteFollowIntent
    data object ExplainerDismissed : CommuteFollowIntent
}

internal sealed interface CommuteFollowEffect : UiEffect

@HiltViewModel
internal class CommuteFollowViewModel @Inject constructor(
    private val followTransitArrival: FollowTransitArrivalUseCase,
    private val getTransitNetwork: GetTransitNetworkUseCase,
    private val locationClient: DeviceLocationClient,
    private val clock: AppClock,
) : MviViewModel<CommuteFollowUiState, CommuteFollowIntent, CommuteFollowEffect>(CommuteFollowUiState()) {
    private var pollJob: Job? = null
    private var polling = false

    // The screen owns the permission prompt; these let it ask before raising it.
    val locationPermissions: Array<String> get() = locationClient.permissions

    fun isLocationGranted(): Boolean = locationClient.isGranted()

    override fun onIntent(intent: CommuteFollowIntent) {
        when (intent) {
            is CommuteFollowIntent.Seed -> setState {
                CommuteFollowUiState(
                    lineNumber = intent.arrival.lineNumber,
                    direction = intent.direction,
                    stopId = intent.stopId,
                    arrivalId = intent.arrival.id,
                    vehicleId = intent.arrival.vehicleId,
                    arrival = intent.arrival,
                    followReceivedAt = intent.receivedAt,
                )
            }
            is CommuteFollowIntent.Open -> open(intent)
            CommuteFollowIntent.Start -> {
                polling = true
                restartPolling()
            }
            CommuteFollowIntent.Stop -> {
                polling = false
                restartPolling()
            }
            CommuteFollowIntent.LocateMe -> locate()
            CommuteFollowIntent.HideMyLocation -> setState { copy(userLocation = null) }
            CommuteFollowIntent.LocationDenied -> setState { copy(locationDenied = true, locating = false) }
            CommuteFollowIntent.ExplainTapped -> setState { copy(explainerOpen = true) }
            CommuteFollowIntent.ExplainerDismissed -> setState { copy(explainerOpen = false) }
        }
    }

    private fun open(intent: CommuteFollowIntent.Open) {
        if (currentState.arrivalId != intent.arrivalId || currentState.stopId != intent.stopId) {
            setState {
                CommuteFollowUiState(
                    lineNumber = intent.lineNumber,
                    direction = intent.direction,
                    stopId = intent.stopId,
                    arrivalId = intent.arrivalId,
                    vehicleId = intent.vehicleId,
                )
            }
        }
        setState { copy(explainerOpen = false, locationDenied = false) }
        viewModelScope.launch {
            val outcome = getTransitNetwork()
            if (outcome is Outcome.Ok) {
                setState { copy(route = outcome.value.route(lineNumber, direction)) }
            }
        }
        restartPolling()
    }

    private fun locate() {
        if (currentState.locating) return
        setState { copy(locating = true, locationDenied = false) }
        viewModelScope.launch {
            val fix = locationClient.currentLocation()
            setState {
                copy(
                    locating = false,
                    userLocation = fix,
                    locationDenied =
                        fix == null && !locationClient.isGranted(),
                )
            }
        }
    }

    private fun restartPolling() {
        pollJob?.cancel()
        pollJob = null
        if (!polling || currentState.arrivalId.isEmpty()) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                val state = currentState
                val outcome = followTransitArrival(
                    state.lineNumber,
                    state.direction,
                    state.stopId,
                    state.arrivalId,
                    state.vehicleId,
                )
                when (outcome) {
                    is Outcome.Ok -> onFollow(outcome.value)
                    is Outcome.Err -> setState { copy(loadFailed = true) }
                }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    // The server resolves the departure by id: a null arrival means it has
    // passed the stop or no longer runs. The newly associated bus becomes the
    // fallback the next requests send.
    private fun onFollow(follow: TransitFollow) {
        setState {
            copy(
                loadFailed = false,
                follow = follow,
                followReceivedAt = clock.now(),
                arrival = follow.arrival,
                vehicleId = follow.arrival?.vehicleId ?: vehicleId,
            )
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 10_000L
    }
}

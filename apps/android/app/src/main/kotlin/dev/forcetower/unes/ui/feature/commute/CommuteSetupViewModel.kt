package dev.forcetower.unes.ui.feature.commute

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.transit.domain.model.CommuteLeg
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.GeoPoint
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitNetwork
import dev.forcetower.melon.feature.transit.domain.model.TransitStop
import dev.forcetower.melon.feature.transit.domain.usecase.GetTransitNetworkUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.ObserveCommutePlanUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.RemoveCommutePlanUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.SaveCommutePlanUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import java.text.Normalizer
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// The three setup steps — direction, stop, lines — share one session, the
// Android analogue of iOS `CommuteSetupDraft` threaded through
// `CommuteDirectionFeature` / `CommuteStopPickerFeature` / `CommuteLinesFeature`.
// Activity-scoped like the enrollment flow: the steps are sibling pushes that
// read and write the same draft without serialized payloads.
internal enum class CommuteLocationStatus { Idle, Locating, Denied, Unavailable }

internal data class CommuteSetupUiState(
    // The saved plan being edited; empty on a first setup.
    val existing: CommutePlan = CommutePlan(),
    // The directions chosen on the first screen, in catalog order.
    val directions: List<TransitDirection> = listOf(TransitDirection.ToCampus),
    // The legs saved so far on this pass.
    val draft: CommutePlan = CommutePlan(),
    val network: TransitNetwork? = null,
    val networkFailed: Boolean = false,
    // Stop step.
    val query: String = "",
    val selectedStopId: String? = null,
    val userLocation: GeoPoint? = null,
    val location: CommuteLocationStatus = CommuteLocationStatus.Idle,
    // Lines step, per direction.
    val linesDirection: TransitDirection? = null,
    val linesStop: TransitStop? = null,
    val selectedLines: List<String> = emptyList(),
    val walkMinutes: Int = 5,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    // Removal, offered on the direction step while editing.
    val removeConfirmOpen: Boolean = false,
    val removing: Boolean = false,
    val removeFailed: Boolean = false,
) : UiState {
    val isEditing: Boolean get() = !existing.isEmpty

    val totalSteps: Int
        get() = 1 +
            (if (TransitDirection.ToCampus in directions) 2 else 0) +
            (if (TransitDirection.FromCampus in directions) 1 else 0)

    // The step number of the lines screen for `direction`.
    fun linesStep(direction: TransitDirection): Int = if (direction == TransitDirection.ToCampus) 3 else totalSteps

    // The direction still waiting for its lines after `direction`, if any.
    fun directionAfter(direction: TransitDirection): TransitDirection? =
        directions.getOrNull(directions.indexOf(direction) + 1)

    // Boarding stops, narrowed by the search, sorted by distance after an
    // explicit location request and otherwise led by the saved stop.
    val stops: List<TransitStop>
        get() {
            val network = network ?: return emptyList()
            var stops = network.boardingStops(TransitDirection.ToCampus)
            val needle = normalize(query)
            if (needle.isNotEmpty()) {
                stops = stops.filter { normalize("${it.name} ${it.area.orEmpty()}").contains(needle) }
            }
            val location = userLocation
            if (location != null) {
                return stops.sortedBy { it.location.distanceTo(location) }
            }
            val selected = stops.firstOrNull { it.id == selectedStopId } ?: return stops
            return listOf(selected) + stops.filter { it.id != selected.id }
        }

    val selectedStop: TransitStop?
        get() = network?.boardingStops(TransitDirection.ToCampus)?.firstOrNull { it.id == selectedStopId }

    // Lines whose route in the current lines direction stops at the stop.
    val servingLines: List<String>
        get() {
            val network = network ?: return emptyList()
            val direction = linesDirection ?: return emptyList()
            val stop = linesStop ?: return emptyList()
            return network.linesServing(stop.id, direction)
        }

    val isLastLinesStep: Boolean
        get() = linesDirection?.let { directionAfter(it) } == null

    private companion object {
        // Accent- and case-insensitive, so "quiteria" finds "Quitéria".
        fun normalize(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .lowercase()
                .trim()
    }
}

internal sealed interface CommuteSetupIntent : UiIntent {
    // Entering the direction step. Starts the session from the mirrored
    // plan unless one is already running (a pop back to the first step).
    data object Enter : CommuteSetupIntent

    // Explicit entry from the board's settings or the Hoje invitation —
    // always a fresh session over `plan`.
    data class Begin(
        val plan: CommutePlan,
    ) : CommuteSetupIntent
    data object LoadNetwork : CommuteSetupIntent
    data class DirectionToggled(
        val direction: TransitDirection,
    ) : CommuteSetupIntent
    data object ContinueFromDirection : CommuteSetupIntent
    data object RemoveTapped : CommuteSetupIntent
    data object RemoveDismissed : CommuteSetupIntent
    data object RemoveConfirmed : CommuteSetupIntent
    data object RemoveFailureDismissed : CommuteSetupIntent

    // Stop step.
    data object OpenStop : CommuteSetupIntent
    data class QueryChanged(
        val query: String,
    ) : CommuteSetupIntent
    data class StopSelected(
        val stopId: String?,
    ) : CommuteSetupIntent
    data object LocateMe : CommuteSetupIntent
    data object LocationDenied : CommuteSetupIntent
    data object ContinueFromStop : CommuteSetupIntent

    // Lines step.
    data class OpenLines(
        val direction: TransitDirection,
    ) : CommuteSetupIntent
    data class LineToggled(
        val lineNumber: String,
    ) : CommuteSetupIntent
    data class WalkChanged(
        val minutes: Int,
    ) : CommuteSetupIntent
    data object SaveTapped : CommuteSetupIntent
    data object SaveFailureDismissed : CommuteSetupIntent
}

// Where the flow goes next; the shell turns these into pushes, like iOS
// `CommutePath.route`.
internal sealed interface CommuteSetupEffect : UiEffect {
    data object GoToStop : CommuteSetupEffect
    data class GoToLines(
        val direction: TransitDirection,
    ) : CommuteSetupEffect

    // The plan was saved: the setup steps give way to its board.
    data class Finished(
        val plan: CommutePlan,
        val direction: TransitDirection,
    ) : CommuteSetupEffect
    data object Removed : CommuteSetupEffect

    // A later step came back without its session (the process was recreated
    // under it): the steps give way to the first, which starts over.
    data object Expired : CommuteSetupEffect
}

@HiltViewModel
internal class CommuteSetupViewModel @Inject constructor(
    private val observeCommutePlan: ObserveCommutePlanUseCase,
    private val getTransitNetwork: GetTransitNetworkUseCase,
    private val saveCommutePlan: SaveCommutePlanUseCase,
    private val removeCommutePlan: RemoveCommutePlanUseCase,
    private val locationClient: DeviceLocationClient,
    private val analytics: Analytics,
) : MviViewModel<CommuteSetupUiState, CommuteSetupIntent, CommuteSetupEffect>(CommuteSetupUiState()) {
    private var begun = false

    // The stop step owns the permission prompt; these let it ask before raising it.
    val locationPermissions: Array<String> get() = locationClient.permissions

    fun isLocationGranted(): Boolean = locationClient.isGranted()

    override fun onIntent(intent: CommuteSetupIntent) {
        when (intent) {
            CommuteSetupIntent.Enter -> if (!begun) {
                viewModelScope.launch { begin(observeCommutePlan().first()) }
            }
            is CommuteSetupIntent.Begin -> begin(intent.plan)
            CommuteSetupIntent.LoadNetwork -> loadNetwork()
            is CommuteSetupIntent.DirectionToggled -> toggleDirection(intent.direction)
            CommuteSetupIntent.ContinueFromDirection -> {
                emitEffect(
                    if (TransitDirection.ToCampus in currentState.directions) {
                        CommuteSetupEffect.GoToStop
                    } else {
                        CommuteSetupEffect.GoToLines(TransitDirection.FromCampus)
                    },
                )
            }
            CommuteSetupIntent.RemoveTapped -> setState { copy(removeConfirmOpen = true) }
            CommuteSetupIntent.RemoveDismissed -> setState { copy(removeConfirmOpen = false) }
            CommuteSetupIntent.RemoveConfirmed -> remove()
            CommuteSetupIntent.RemoveFailureDismissed -> setState { copy(removeFailed = false) }
            CommuteSetupIntent.OpenStop -> if (!begun) emitEffect(CommuteSetupEffect.Expired)
            is CommuteSetupIntent.QueryChanged -> setState { copy(query = intent.query) }
            is CommuteSetupIntent.StopSelected -> setState { copy(selectedStopId = intent.stopId) }
            CommuteSetupIntent.LocateMe -> locate()
            CommuteSetupIntent.LocationDenied -> setState { copy(location = CommuteLocationStatus.Denied) }
            CommuteSetupIntent.ContinueFromStop -> {
                if (currentState.selectedStop !=
                    null
                ) {
                    emitEffect(CommuteSetupEffect.GoToLines(TransitDirection.ToCampus))
                }
            }
            is CommuteSetupIntent.OpenLines -> {
                if (begun) openLines(intent.direction) else emitEffect(CommuteSetupEffect.Expired)
            }
            is CommuteSetupIntent.LineToggled -> toggleLine(intent.lineNumber)
            is CommuteSetupIntent.WalkChanged -> setState { copy(walkMinutes = intent.minutes.coerceIn(WALK_RANGE)) }
            CommuteSetupIntent.SaveTapped -> save()
            CommuteSetupIntent.SaveFailureDismissed -> setState { copy(saveFailed = false) }
        }
    }

    private fun begin(plan: CommutePlan) {
        begun = true
        val saved = plan.directions
        setState {
            CommuteSetupUiState(
                existing = plan,
                directions = saved.ifEmpty { listOf(TransitDirection.ToCampus) },
                selectedStopId = plan.toCampus?.stopId,
                network = network,
            )
        }
        loadNetwork()
    }

    private fun loadNetwork() {
        if (currentState.network != null) return
        setState { copy(networkFailed = false) }
        viewModelScope.launch {
            when (val outcome = getTransitNetwork()) {
                is Outcome.Ok -> {
                    setState { copy(network = outcome.value) }
                    // The lines step may already be waiting on the catalog.
                    currentState.linesDirection?.let(::openLines)
                }
                is Outcome.Err -> setState { copy(networkFailed = true) }
            }
        }
    }

    // At least one direction stays on.
    private fun toggleDirection(direction: TransitDirection) {
        setState {
            val next = if (direction in directions) {
                if (directions.size > 1) directions - direction else directions
            } else {
                TransitDirection.entries.filter { it == direction || it in directions }
            }
            copy(directions = next)
        }
    }

    private fun locate() {
        if (currentState.location == CommuteLocationStatus.Locating) return
        setState { copy(location = CommuteLocationStatus.Locating) }
        viewModelScope.launch {
            val fix = locationClient.currentLocation()
            setState {
                when {
                    fix != null -> copy(userLocation = fix, location = CommuteLocationStatus.Idle, query = "")
                    !locationClient.isGranted() -> copy(location = CommuteLocationStatus.Denied)
                    else -> copy(location = CommuteLocationStatus.Unavailable)
                }
            }
        }
    }

    // Leaving campus has a single boarding stop, so that direction comes
    // straight here. The lines prefill from this pass's draft, then the saved
    // leg when it is for the same stop, then every line that serves it.
    private fun openLines(direction: TransitDirection) {
        setState {
            val stop = if (direction == TransitDirection.ToCampus) selectedStop else network?.campusDepartureStop
            val serving = if (stop != null) network?.linesServing(stop.id, direction).orEmpty() else emptyList()
            val remembered = listOfNotNull(draft.leg(direction), existing.leg(direction))
                .firstOrNull { it.stopId == stop?.id }
            val previous = remembered?.lineNumbers?.filter(serving::contains).orEmpty()
            copy(
                linesDirection = direction,
                linesStop = stop,
                selectedLines = previous.ifEmpty { serving },
                walkMinutes = remembered?.walkMinutes
                    ?: existing.leg(direction)?.walkMinutes
                    ?: if (direction == TransitDirection.ToCampus) DEFAULT_WALK_TO_CAMPUS else DEFAULT_WALK_FROM_CAMPUS,
                saveFailed = false,
            )
        }
    }

    private fun toggleLine(lineNumber: String) {
        setState {
            if (lineNumber !in servingLines) return@setState this
            val next = if (lineNumber in selectedLines) {
                selectedLines - lineNumber
            } else {
                servingLines.filter { it == lineNumber || it in selectedLines }
            }
            copy(selectedLines = next)
        }
    }

    private fun save() {
        val state = currentState
        val direction = state.linesDirection ?: return
        val stop = state.linesStop ?: return
        if (state.selectedLines.isEmpty() || state.saving) return
        val leg = CommuteLeg(
            stopId = stop.id,
            stopName = stop.name,
            stopArea = stop.area,
            lineNumbers = state.selectedLines,
            walkMinutes = state.walkMinutes,
        )
        val draft = state.draft.withLeg(direction, leg)
        setState { copy(draft = draft) }
        val next = state.directionAfter(direction)
        if (next != null) {
            emitEffect(CommuteSetupEffect.GoToLines(next))
            return
        }
        setState { copy(saving = true) }
        // A pass through a direction unchecked since leaves its leg in the
        // draft; only the chosen directions are sent.
        val plan = state.directions.fold(CommutePlan()) { plan, chosen -> plan.withLeg(chosen, draft.leg(chosen)) }
        viewModelScope.launch {
            when (val outcome = saveCommutePlan(plan)) {
                is Outcome.Ok -> {
                    setState { copy(saving = false) }
                    begun = false
                    analytics.selectContent(
                        ContentTypes.SETTING,
                        "commute_saved",
                        mapOf("directions" to outcome.value.directions.joinToString(",") { it.wire }),
                    )
                    emitEffect(CommuteSetupEffect.Finished(outcome.value, state.directions.first()))
                }
                is Outcome.Err -> setState { copy(saving = false, saveFailed = true) }
            }
        }
    }

    private fun remove() {
        setState { copy(removeConfirmOpen = false, removing = true) }
        viewModelScope.launch {
            when (removeCommutePlan()) {
                is Outcome.Ok -> {
                    setState { copy(removing = false) }
                    begun = false
                    analytics.selectContent(ContentTypes.SETTING, "commute_removed")
                    emitEffect(CommuteSetupEffect.Removed)
                }
                is Outcome.Err -> setState { copy(removing = false, removeFailed = true) }
            }
        }
    }

    companion object {
        val WALK_RANGE = 0..30
        private const val DEFAULT_WALK_TO_CAMPUS = 5
        private const val DEFAULT_WALK_FROM_CAMPUS = 3
    }
}

package dev.forcetower.melon.feature.transit.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.transit.data.TransitRepository
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitError
import dev.forcetower.melon.feature.transit.domain.model.TransitFollow
import dev.forcetower.melon.feature.transit.domain.model.TransitNetwork
import dev.forcetower.melon.feature.transit.domain.model.TransitStopBoard
import dev.zacsweers.metro.Inject

// The lines, routes and stops — cached for a few hours, stale on failure.
@Inject
class GetTransitNetworkUseCase internal constructor(
    private val repository: TransitRepository,
) {
    suspend operator fun invoke(): Outcome<TransitNetwork, TransitError> = repository.network()
}

// One stop's board for the saved lines in one direction. Polled while the
// board or the Hoje line is on screen.
@Inject
class GetStopArrivalsUseCase internal constructor(
    private val repository: TransitRepository,
) {
    suspend operator fun invoke(
        stopId: String,
        direction: TransitDirection,
        lineNumbers: List<String>,
    ): Outcome<TransitStopBoard, TransitError> = repository.arrivals(stopId, direction, lineNumbers)
}

// One departure's buses and band, polled while "Seguindo" is on screen.
@Inject
class FollowTransitArrivalUseCase internal constructor(
    private val repository: TransitRepository,
) {
    suspend operator fun invoke(
        lineNumber: String,
        direction: TransitDirection,
        stopId: String,
        arrivalId: String,
        vehicleId: String?,
    ): Outcome<TransitFollow, TransitError> = repository.follow(lineNumber, direction, stopId, arrivalId, vehicleId)
}

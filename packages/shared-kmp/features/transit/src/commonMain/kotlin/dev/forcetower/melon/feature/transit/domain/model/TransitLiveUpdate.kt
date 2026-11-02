package dev.forcetower.melon.feature.transit.domain.model

import kotlin.time.Instant

// A followed departure outside the app: the backend pushes one of these
// whenever what the ongoing notification shows would change
// (`contracts/v1/transit.json` → `liveActivity.content`, the iOS
// `CommuteActivityAttributes.ContentState`).
data class TransitLiveUpdate(
    val status: Status,
    // Null once it ended.
    val window: Window?,
    val originDeparture: Instant?,
    val campusArrival: Instant?,
    val stopsAway: Int?,
    val atStop: Boolean,
    // The followed bus along the route; null while it is not on it.
    val progressMeters: Double?,
    val signalLost: Boolean,
) {
    enum class Status(
        val wire: String,
    ) {
        Live("live"),
        Waiting("waiting"),
        Historical("historical"),

        // The followed bus went by the stop.
        Passed("passed"),

        // The departure stopped running, or following it timed out.
        Ended("ended"),
        ;

        companion object {
            fun fromWire(value: String): Status? = entries.firstOrNull { it.wire == value }
        }
    }

    data class Window(
        val earliest: Instant,
        val latest: Instant,
        val expected: Instant,
    )

    // The followed departure as the estimate rules read it; null once it ended.
    fun arrival(
        id: String,
        lineNumber: String,
    ): TransitArrival? {
        val window = window ?: return null
        val state = TransitArrivalState.fromWire(status.wire) ?: return null
        return TransitArrival(
            id = id,
            lineNumber = lineNumber,
            state = state,
            phase = if (atStop) TransitVehiclePhase.AtStop else null,
            earliest = window.earliest,
            latest = window.latest,
            expected = window.expected,
            originDeparture = originDeparture,
            campusArrival = campusArrival,
            stopsAway = stopsAway,
            signalLost = signalLost,
        )
    }

    companion object {
        // What the app shows the moment the student starts following, before
        // the first push.
        fun of(
            arrival: TransitArrival,
            progressMeters: Double?,
        ): TransitLiveUpdate =
            TransitLiveUpdate(
                status = Status.fromWire(arrival.state.wire) ?: Status.Historical,
                window = Window(arrival.earliest, arrival.latest, arrival.expected),
                originDeparture = arrival.originDeparture,
                campusArrival = arrival.campusArrival,
                stopsAway = arrival.stopsAway,
                atStop = arrival.phase == TransitVehiclePhase.AtStop,
                progressMeters = progressMeters,
                signalLost = arrival.signalLost,
            )
    }
}

// `POST api/transit/live-activities` for this device's ongoing notification.
data class TransitLiveUpdateRegistration(
    val activityId: String,
    val deviceToken: String,
    val lineNumber: String,
    val direction: TransitDirection,
    val stopId: String,
    val arrivalId: String,
    val vehicleId: String?,
)

package dev.forcetower.unes.commute

import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitArrivalState
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitLiveUpdate
import dev.forcetower.melon.feature.transit.domain.model.TransitRoute
import dev.forcetower.unes.ui.feature.commute.CommuteEstimate
import dev.forcetower.unes.ui.feature.commute.CommuteFollowSeed
import dev.forcetower.unes.ui.feature.commute.CommuteFormat
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.serialization.Serializable

// The followed departure behind the ongoing notification — one at a time, as
// on iOS. Kept on disk so a push, a stale alarm or a stop tapped on the
// notification can redraw or end it after the process died.
@Serializable
internal data class CommuteLiveRecord(
    val activityId: String,
    val lineNumber: String,
    val lineName: String,
    val direction: String,
    val stopId: String,
    val arrivalId: String,
    val vehicleId: String?,
    // Where the route line's stops sit along the route, origin first: up to
    // the student's stop, or on to the end when they board where it starts.
    val stopDistances: List<Double>,
    val mineIndex: Int,
    val update: Update,
    // Server time of the update on screen, Unix seconds; an older push is dropped.
    val updatedAt: Long,
    // The FCM token the backend pushes this follow to; null until a
    // registration goes through.
    val registeredToken: String? = null,
    // Unix seconds. Past it the follow ends even if the backend's end never
    // arrived, as ActivityKit ends a Live Activity on its own. A record written
    // before it existed counts from its last update.
    val expiresAt: Long = updatedAt + MAX_LIFETIME.inWholeSeconds,
) {
    val expiry: Instant get() = Instant.fromEpochSeconds(expiresAt)

    fun expired(now: Instant): Boolean = now >= expiry

    @Serializable
    data class Update(
        val status: String,
        val earliest: Long? = null,
        val latest: Long? = null,
        val expected: Long? = null,
        val originDeparture: Long? = null,
        val campusArrival: Long? = null,
        val stopsAway: Int? = null,
        val atStop: Boolean = false,
        val progressMeters: Double? = null,
        val signalLost: Boolean = false,
    ) {
        fun toDomain(): TransitLiveUpdate =
            TransitLiveUpdate(
                status = TransitLiveUpdate.Status.fromWire(status) ?: TransitLiveUpdate.Status.Ended,
                window = if (earliest != null && latest != null && expected != null) {
                    TransitLiveUpdate.Window(
                        Instant.fromEpochSeconds(earliest),
                        Instant.fromEpochSeconds(latest),
                        Instant.fromEpochSeconds(expected),
                    )
                } else {
                    null
                },
                originDeparture = originDeparture?.let { Instant.fromEpochSeconds(it) },
                campusArrival = campusArrival?.let { Instant.fromEpochSeconds(it) },
                stopsAway = stopsAway,
                atStop = atStop,
                progressMeters = progressMeters,
                signalLost = signalLost,
            )

        companion object {
            fun of(update: TransitLiveUpdate): Update =
                Update(
                    status = update.status.wire,
                    earliest = update.window?.earliest?.epochSeconds,
                    latest = update.window?.latest?.epochSeconds,
                    expected = update.window?.expected?.epochSeconds,
                    originDeparture = update.originDeparture?.epochSeconds,
                    campusArrival = update.campusArrival?.epochSeconds,
                    stopsAway = update.stopsAway,
                    atStop = update.atStop,
                    progressMeters = update.progressMeters,
                    signalLost = update.signalLost,
                )
        }
    }

    // Leaving the campus from where the line starts: the follow screen says
    // "até a saída" there, and so does the notification.
    val untilDeparture: Boolean
        get() = mineIndex == 0 && direction == TransitDirection.FromCampus.wire

    // What a tap on the notification opens the follow screen with; null once
    // the bus passed and there is nothing left to follow.
    fun followSeed(): CommuteFollowSeed? {
        val arrival = update.toDomain().arrival(arrivalId, lineNumber) ?: return null
        return CommuteFollowSeed(
            arrival = arrival.copy(vehicleId = vehicleId),
            direction = TransitDirection.fromWire(direction) ?: return null,
            stopId = stopId,
            receivedAt = Instant.fromEpochSeconds(updatedAt),
        )
    }

    companion object {
        private val MAX_LIFETIME = 8.hours

        fun of(
            activityId: String,
            arrival: TransitArrival,
            route: TransitRoute,
            lineName: String,
            stopId: String,
            progressMeters: Double?,
            now: Instant,
        ): CommuteLiveRecord? {
            val mine = route.indexOf(stopId) ?: return null
            val end = when {
                mine > 0 -> mine
                route.direction == TransitDirection.ToCampus ->
                    route.campusStopId?.let(route::indexOf) ?: route.stops.lastIndex
                else -> route.stops.lastIndex
            }
            return CommuteLiveRecord(
                activityId = activityId,
                lineNumber = arrival.lineNumber,
                lineName = lineName,
                direction = route.direction.wire,
                stopId = stopId,
                arrivalId = arrival.id,
                vehicleId = arrival.vehicleId,
                stopDistances = route.stops.take(end + 1).map { it.distanceMeters },
                mineIndex = mine,
                update = Update.of(TransitLiveUpdate.of(arrival, progressMeters)),
                updatedAt = now.epochSeconds,
            )
        }
    }
}

// What the notification says, decided apart from the strings that say it.
internal sealed interface CommuteLiveView {
    data class Following(
        val estimate: CommuteEstimate,
        // The status-bar chip: the estimate when it fits, its digits otherwise.
        val chip: CommuteEstimate,
        val untilDeparture: Boolean,
        val degraded: Degraded?,
        val stopsAway: Int?,
        val campusArrival: Instant?,
        val usuallyLeaves: Instant?,
    ) : CommuteLiveView

    data object Passed : CommuteLiveView

    data object NoEstimate : CommuteLiveView

    enum class Degraded {
        // The bus stopped reporting its position.
        SignalLost,

        // No push reached the device for a while.
        Offline,
    }
}

internal fun commuteLiveView(
    record: CommuteLiveRecord,
    now: Instant,
    stale: Boolean,
): CommuteLiveView {
    val update = record.update.toDomain()
    val arrival = update.arrival(record.arrivalId, record.lineNumber)
        ?: return if (update.status == TransitLiveUpdate.Status.Passed) {
            CommuteLiveView.Passed
        } else {
            CommuteLiveView.NoEstimate
        }
    val estimate = CommuteFormat.estimate(arrival, now, frozen = stale)
    // A clock band doesn't fit the chip; the most likely time does.
    val chip = if (estimate is CommuteEstimate.ClockRange) {
        CommuteEstimate.Clock(arrival.expected, isApproximate = true)
    } else {
        estimate
    }
    return CommuteLiveView.Following(
        estimate = estimate,
        chip = chip,
        untilDeparture = record.untilDeparture,
        degraded = when {
            stale -> CommuteLiveView.Degraded.Offline
            arrival.signalLost -> CommuteLiveView.Degraded.SignalLost
            else -> null
        },
        // The count includes the student's stop, so 1 means it is the next one.
        stopsAway = arrival.stopsAway?.takeIf { it > 0 && arrival.state == TransitArrivalState.Live },
        campusArrival = arrival.campusArrival,
        usuallyLeaves = arrival.originDeparture?.takeIf {
            arrival.campusArrival == null && arrival.state == TransitArrivalState.Waiting
        },
    )
}

// The route line as the platform's progress track wants it: lengths and
// positions in metres from the first stop.
internal data class CommuteLiveTrack(
    // One per stop-to-stop span, so the gaps between them mark the stops.
    val segments: List<Int>,
    // Null while the followed bus isn't on the route.
    val progress: Int?,
    // The platform draws no point at either end, so there the stop gets an icon.
    val minePoint: Int?,
    val mineAtStart: Boolean,
    val mineAtEnd: Boolean,
)

internal fun commuteLiveTrack(record: CommuteLiveRecord): CommuteLiveTrack {
    val origin = record.stopDistances.first()
    val segments = record.stopDistances.zipWithNext { from, to -> (to - from).roundToInt().coerceAtLeast(1) }
        .ifEmpty { listOf(1) }
    val length = segments.sum()
    val mine = segments.take(record.mineIndex).sum()
    return CommuteLiveTrack(
        segments = segments,
        progress = record.update.progressMeters?.let { (it - origin).roundToInt().coerceIn(0, length) },
        minePoint = mine.takeIf { it in 1 until length },
        mineAtStart = mine <= 0,
        mineAtEnd = mine >= length,
    )
}

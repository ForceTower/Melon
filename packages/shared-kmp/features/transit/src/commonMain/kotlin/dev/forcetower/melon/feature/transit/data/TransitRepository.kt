package dev.forcetower.melon.feature.transit.data

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.AppClock
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.transit.data.network.TransitService
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitError
import dev.forcetower.melon.feature.transit.domain.model.TransitFollow
import dev.forcetower.melon.feature.transit.domain.model.TransitNetwork
import dev.forcetower.melon.feature.transit.domain.model.TransitStopBoard
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// The bus network, arrivals and follow reads. Stops and geometry change only
// when the city edits a route, so the network is reused for a few hours and a
// stale copy still beats no map when the request fails; arrivals and follow
// are meant to be polled while their screen is visible. The Android analogue
// of iOS `TransitRepository+Live`.
@SingleIn(AppScope::class)
@Inject
class TransitRepository internal constructor(
    private val service: TransitService,
    private val clock: AppClock,
    logger: Logger,
) {
    private val log = logger.withTag("TransitRepository")
    private val networkLock = Mutex()
    private var cachedNetwork: Pair<TransitNetwork, Instant>? = null
    private val outages = Mutex()
    private val failing = mutableSetOf<String>()

    suspend fun network(): Outcome<TransitNetwork, TransitError> =
        networkLock.withLock {
            val cached = cachedNetwork
            if (cached != null && clock.now() - cached.second < NETWORK_TTL) return Outcome.Ok(cached.first)
            when (val outcome = service.network()) {
                is Outcome.Ok -> {
                    cachedNetwork = outcome.value to clock.now()
                    log.i { "network ok lines=${outcome.value.lines.size} routes=${outcome.value.routes.size}" }
                    outcome
                }
                is Outcome.Err -> {
                    // Once per screen entry; being offline isn't a bug.
                    log.i { "network failed err=${outcome.error} stale=${cached != null}" }
                    cached?.let { Outcome.Ok(it.first) } ?: outcome
                }
            }
        }

    suspend fun arrivals(
        stopId: String,
        direction: TransitDirection,
        lineNumbers: List<String>,
    ): Outcome<TransitStopBoard, TransitError> {
        val outcome = service.arrivals(stopId, direction, lineNumbers)
        noteOutcome("arrivals", "$stopId/${direction.wire}", failed = outcome is Outcome.Err)
        return outcome
    }

    // Follows one departure by its arrival id, which survives it going from
    // history to a bus on its way; `vehicleId` is the fallback.
    suspend fun follow(
        lineNumber: String,
        direction: TransitDirection,
        stopId: String,
        arrivalId: String,
        vehicleId: String?,
    ): Outcome<TransitFollow, TransitError> {
        val outcome = service.follow(lineNumber, direction, stopId, arrivalId, vehicleId)
        noteOutcome("follow", arrivalId, failed = outcome is Outcome.Err)
        return outcome
    }

    // The polled reads log once per outage: the first failure and the recovery
    // at Info (one remote record each, no non-fatal), the repeats in between
    // at Debug, which stays on the device. A screen polling every few seconds
    // offline must not file a report per tick.
    private suspend fun noteOutcome(
        read: String,
        key: String,
        failed: Boolean,
    ) {
        val wasFailing = outages.withLock { if (failed) !failing.add(key) else failing.remove(key) }
        when {
            failed && !wasFailing -> log.i { "$read failing key=$key" }
            failed -> log.d { "$read still failing key=$key" }
            wasFailing -> log.i { "$read recovered key=$key" }
        }
    }

    private companion object {
        val NETWORK_TTL = 6.hours
    }
}

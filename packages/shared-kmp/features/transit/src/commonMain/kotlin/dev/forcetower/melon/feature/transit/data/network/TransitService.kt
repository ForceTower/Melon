package dev.forcetower.melon.feature.transit.data.network

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.network.ApiEnvelope
import dev.forcetower.melon.feature.transit.domain.model.CommuteLeg
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.GeoPoint
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitArrivalState
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitError
import dev.forcetower.melon.feature.transit.domain.model.TransitEstimateBasis
import dev.forcetower.melon.feature.transit.domain.model.TransitFeed
import dev.forcetower.melon.feature.transit.domain.model.TransitFeedStatus
import dev.forcetower.melon.feature.transit.domain.model.TransitFollow
import dev.forcetower.melon.feature.transit.domain.model.TransitLine
import dev.forcetower.melon.feature.transit.domain.model.TransitNetwork
import dev.forcetower.melon.feature.transit.domain.model.TransitNextService
import dev.forcetower.melon.feature.transit.domain.model.TransitRoute
import dev.forcetower.melon.feature.transit.domain.model.TransitStop
import dev.forcetower.melon.feature.transit.domain.model.TransitStopBoard
import dev.forcetower.melon.feature.transit.domain.model.TransitTodaySlot
import dev.forcetower.melon.feature.transit.domain.model.TransitTodaySlotStatus
import dev.forcetower.melon.feature.transit.domain.model.TransitVehicle
import dev.forcetower.melon.feature.transit.domain.model.TransitVehiclePhase
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// `api/transit/*` — the signed-in wire contract behind "Meu trajeto"
// (`contracts/v1/transit.json`). Mirrors iOS `TransitDTO.swift`: instants
// are ISO strings, unknown vehicle phases degrade to "no phase" and unknown
// feed statuses to `Unavailable`, so a newer server never blanks the board.
@Inject
internal class TransitService(
    private val client: HttpClient,
) {
    suspend fun network(): Outcome<TransitNetwork, TransitError> =
        payload<TransitNetworkDTO, TransitNetwork>({ client.get("api/transit/network") }) { it.toDomain() }

    suspend fun arrivals(
        stopId: String,
        direction: TransitDirection,
        lineNumbers: List<String>,
    ): Outcome<TransitStopBoard, TransitError> =
        payload<TransitStopArrivalsDTO, TransitStopBoard>({
            client.get("api/transit/stops/$stopId/arrivals") {
                parameter("direction", direction.wire)
                parameter("lines", lineNumbers.joinToString(","))
            }
        }) { it.toDomain() }

    suspend fun follow(
        lineNumber: String,
        direction: TransitDirection,
        stopId: String,
        arrivalId: String,
        vehicleId: String?,
    ): Outcome<TransitFollow, TransitError> =
        payload<TransitFollowDTO, TransitFollow>({
            client.get("api/transit/follow") {
                parameter("line", lineNumber)
                parameter("direction", direction.wire)
                parameter("stopId", stopId)
                parameter("arrivalId", arrivalId)
                if (vehicleId != null) parameter("vehicleId", vehicleId)
            }
        }) { it.toDomain() }

    suspend fun plan(): Outcome<CommutePlan, TransitError> =
        payload<TransitCommutePlanDTO, CommutePlan>({ client.get("api/transit/plan") }) { it.toDomain() }

    // `PUT` replaces the plan whole: both keys are always sent, null removes
    // a direction. The shared Json drops nulls, so the body is built by hand.
    suspend fun savePlan(plan: CommutePlan): Outcome<CommutePlan, TransitError> =
        payload<TransitCommutePlanDTO, CommutePlan>({
            client.put("api/transit/plan") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("toCampus", plan.toCampus.toJson())
                        put("fromCampus", plan.fromCampus.toJson())
                    },
                )
            }
        }) { it.toDomain() }

    suspend fun removePlan(): Outcome<CommutePlan, TransitError> =
        payload<TransitCommutePlanDTO, CommutePlan>({ client.delete("api/transit/plan") }) { it.toDomain() }

    private suspend inline fun <reified D, T> payload(
        request: () -> HttpResponse,
        map: (D) -> T?,
    ): Outcome<T, TransitError> {
        val dto = try {
            val response = request()
            if (!response.status.isSuccess()) return Outcome.Err(TransitError.Connection)
            response.body<ApiEnvelope<D>>().takeIf { it.ok }?.data
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        } ?: return Outcome.Err(TransitError.Connection)
        return map(dto)?.let { Outcome.Ok(it) } ?: Outcome.Err(TransitError.Connection)
    }
}

private fun CommuteLeg?.toJson() =
    if (this == null) {
        JsonNull
    } else {
        buildJsonObject {
            put("stopId", stopId)
            put(
                "lineNumbers",
                buildJsonArray {
                    lineNumbers.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                },
            )
            put("walkMinutes", walkMinutes)
        }
    }

// ───────── wire shapes ─────────

@Serializable
internal data class TransitNetworkDTO(
    val lines: List<TransitLineDTO>,
    val routes: List<TransitRouteDTO>,
)

@Serializable
internal data class TransitLineDTO(
    val number: String,
    val name: String,
)

@Serializable
internal data class TransitStopDTO(
    val id: String,
    val name: String,
    val area: String? = null,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
)

@Serializable
internal data class TransitRouteDTO(
    val lineNumber: String,
    val direction: String,
    val itineraryId: String,
    val stops: List<TransitStopDTO>,
    val path: List<List<Double>>,
    val campusStopId: String? = null,
    val typicalMinutes: RangeDTO? = null,
) {
    @Serializable
    internal data class RangeDTO(
        val low: Int,
        val high: Int,
    )
}

@Serializable
internal data class TransitBasisDTO(
    val sampleCount: Int,
    val dayCount: Int,
    val hitRate: Double,
    val histogram: HistogramDTO,
) {
    @Serializable
    internal data class HistogramDTO(
        val startsAt: String,
        val binMinutes: Int,
        val counts: List<Int>,
    )
}

@Serializable
internal data class TransitArrivalDTO(
    val id: String,
    val lineNumber: String,
    val state: String,
    val phase: String? = null,
    val vehicleId: String? = null,
    val window: WindowDTO,
    val expected: String,
    val originDeparture: String? = null,
    val originArrival: String? = null,
    val campusArrival: String? = null,
    val stopsAway: Int? = null,
    val lastFixAt: String? = null,
    val signalLost: Boolean = false,
    val lastSeenStopName: String? = null,
    val scheduledDeparture: String? = null,
    val reliability: ReliabilityDTO? = null,
    val missing: Boolean = false,
    val basis: TransitBasisDTO? = null,
) {
    @Serializable
    internal data class WindowDTO(
        val earliest: String,
        val latest: String,
    )

    @Serializable
    internal data class ReliabilityDTO(
        val ranDays: Int,
        val observedDays: Int,
    )
}

@Serializable
internal data class TransitTodaySlotDTO(
    val lineNumber: String,
    val usual: String,
    val scheduledDeparture: String? = null,
    val status: String,
    val flaky: Boolean = false,
)

@Serializable
internal data class TransitFeedDTO(
    val status: String,
    val lastUpdateAt: String? = null,
)

@Serializable
internal data class TransitStopArrivalsDTO(
    val generatedAt: String,
    val stopId: String,
    val direction: String,
    val feed: TransitFeedDTO,
    val arrivals: List<TransitArrivalDTO>,
    val today: List<TransitTodaySlotDTO> = emptyList(),
    val serviceToday: Boolean = true,
    val nextService: NextServiceDTO? = null,
) {
    @Serializable
    internal data class NextServiceDTO(
        val lineNumber: String,
        val usual: String,
    )
}

@Serializable
internal data class TransitVehicleDTO(
    val id: String,
    val lineNumber: String,
    val latitude: Double,
    val longitude: Double,
    val heading: Double? = null,
    val fixAt: String,
    val progressMeters: Double? = null,
)

@Serializable
internal data class TransitFollowDTO(
    val generatedAt: String,
    val feed: TransitFeedDTO,
    val vehicles: List<TransitVehicleDTO>,
    val arrival: TransitArrivalDTO? = null,
)

@Serializable
internal data class TransitCommuteLegDTO(
    val stopId: String,
    val stopName: String,
    val stopArea: String? = null,
    val lineNumbers: List<String>,
    val walkMinutes: Int,
)

@Serializable
internal data class TransitCommutePlanDTO(
    val toCampus: TransitCommuteLegDTO? = null,
    val fromCampus: TransitCommuteLegDTO? = null,
    val updatedAt: String? = null,
)

// ───────── wire → domain ─────────

private fun instant(raw: String): Instant? = runCatching { Instant.parse(raw) }.getOrNull()

internal fun TransitNetworkDTO.toDomain(): TransitNetwork? {
    val routes = routes.map { route ->
        val direction = TransitDirection.fromWire(route.direction) ?: return null
        TransitRoute(
            lineNumber = route.lineNumber,
            direction = direction,
            itineraryId = route.itineraryId,
            stops = route.stops.map { stop ->
                TransitStop(
                    id = stop.id,
                    name = stop.name,
                    area = stop.area,
                    location = GeoPoint(stop.latitude, stop.longitude),
                    distanceMeters = stop.distanceMeters,
                )
            },
            path = route.path.mapNotNull { pair -> pair.takeIf { it.size == 2 }?.let { GeoPoint(it[0], it[1]) } },
            campusStopId = route.campusStopId,
            typicalMinutes = route.typicalMinutes?.takeIf { it.low <= it.high }?.let { it.low..it.high },
        )
    }
    return TransitNetwork(lines = lines.map { TransitLine(it.number, it.name) }, routes = routes)
}

internal fun TransitArrivalDTO.toDomain(): TransitArrival? {
    val state = TransitArrivalState.fromWire(state) ?: return null
    val basis = basis?.let { basis ->
        TransitEstimateBasis(
            sampleCount = basis.sampleCount,
            dayCount = basis.dayCount,
            hitRate = basis.hitRate,
            histogramStart = instant(basis.histogram.startsAt) ?: return null,
            binMinutes = basis.histogram.binMinutes,
            counts = basis.histogram.counts,
        )
    }
    return TransitArrival(
        id = id,
        lineNumber = lineNumber,
        state = state,
        phase = TransitVehiclePhase.fromWire(phase),
        vehicleId = vehicleId,
        earliest = instant(window.earliest) ?: return null,
        latest = instant(window.latest) ?: return null,
        expected = instant(expected) ?: return null,
        originDeparture = originDeparture?.let(::instant),
        originArrival = originArrival?.let(::instant),
        campusArrival = campusArrival?.let(::instant),
        stopsAway = stopsAway,
        lastFixAt = lastFixAt?.let(::instant),
        signalLost = signalLost,
        lastSeenStopName = lastSeenStopName,
        scheduledDeparture = scheduledDeparture,
        ranDays = reliability?.ranDays,
        observedDays = reliability?.observedDays,
        missing = missing,
        basis = basis,
    )
}

internal fun TransitFeedDTO.toDomain() =
    TransitFeed(
        status = TransitFeedStatus.fromWire(status),
        lastUpdateAt = lastUpdateAt?.let(::instant),
    )

internal fun TransitStopArrivalsDTO.toDomain(): TransitStopBoard? =
    TransitStopBoard(
        generatedAt = instant(generatedAt) ?: return null,
        stopId = stopId,
        direction = TransitDirection.fromWire(direction) ?: return null,
        feed = feed.toDomain(),
        arrivals = arrivals.mapNotNull { it.toDomain() },
        today = today.mapNotNull { slot ->
            TransitTodaySlot(
                lineNumber = slot.lineNumber,
                usual = instant(slot.usual) ?: return@mapNotNull null,
                scheduledDeparture = slot.scheduledDeparture,
                status = TransitTodaySlotStatus.fromWire(slot.status),
                isFlaky = slot.flaky,
            )
        },
        serviceToday = serviceToday,
        nextService = nextService?.let { next ->
            instant(next.usual)?.let { TransitNextService(lineNumber = next.lineNumber, usual = it) }
        },
    )

internal fun TransitFollowDTO.toDomain(): TransitFollow? =
    TransitFollow(
        generatedAt = instant(generatedAt) ?: return null,
        feed = feed.toDomain(),
        vehicles = vehicles.mapNotNull { vehicle ->
            TransitVehicle(
                id = vehicle.id,
                lineNumber = vehicle.lineNumber,
                location = GeoPoint(vehicle.latitude, vehicle.longitude),
                heading = vehicle.heading,
                fixAt = instant(vehicle.fixAt) ?: return@mapNotNull null,
                progressMeters = vehicle.progressMeters,
            )
        },
        arrival = arrival?.toDomain(),
    )

internal fun TransitCommutePlanDTO.toDomain() =
    CommutePlan(toCampus = toCampus?.toDomain(), fromCampus = fromCampus?.toDomain())

internal fun TransitCommuteLegDTO.toDomain() =
    CommuteLeg(
        stopId = stopId,
        stopName = stopName,
        stopArea = stopArea,
        lineNumbers = lineNumbers,
        walkMinutes = walkMinutes,
    )

internal fun CommutePlan.toDto() = TransitCommutePlanDTO(toCampus = toCampus?.toDto(), fromCampus = fromCampus?.toDto())

private fun CommuteLeg.toDto() =
    TransitCommuteLegDTO(
        stopId = stopId,
        stopName = stopName,
        stopArea = stopArea,
        lineNumbers = lineNumbers,
        walkMinutes = walkMinutes,
    )

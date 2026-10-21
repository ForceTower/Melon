package dev.forcetower.melon.feature.transit.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Instant

// The UEFS bus lines as the backend's collector sees them (`api/transit/*`).
// Mirrors iOS `Transit.swift`; `wire` values are the strings the API speaks.

// `ToCampus` is "Chegar na UEFS"; `FromCampus` is "Sair da UEFS".
enum class TransitDirection(
    val wire: String,
) {
    ToCampus("toCampus"),
    FromCampus("fromCampus"),
    ;

    companion object {
        fun fromWire(value: String?): TransitDirection? = entries.firstOrNull { it.wire == value }
    }
}

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    // Haversine distance in metres.
    fun distanceTo(other: GeoPoint): Double {
        val radius = 6_371_000.0
        val dLat = (other.latitude - latitude).toRadians()
        val dLon = (other.longitude - longitude).toRadians()
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(latitude.toRadians()) * cos(other.latitude.toRadians()) * sin(dLon / 2) * sin(dLon / 2)
        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

private fun Double.toRadians(): Double = this * kotlin.math.PI / 180

data class TransitLine(
    // Display number, zero padded ("087").
    val number: String,
    val name: String,
)

data class TransitStop(
    val id: String,
    // Street part ("Av. Maria Quitéria, 3203").
    val name: String,
    // Neighbourhood ("Queimadinha").
    val area: String?,
    val location: GeoPoint,
    // Along its route from the origin.
    val distanceMeters: Double,
)

data class TransitRoute(
    val lineNumber: String,
    val direction: TransitDirection,
    val itineraryId: String,
    // Travel order.
    val stops: List<TransitStop>,
    val path: List<GeoPoint>,
    val campusStopId: String?,
    val typicalMinutes: IntRange?,
) {
    fun indexOf(stopId: String): Int? = stops.indexOfFirst { it.id == stopId }.takeIf { it >= 0 }
}

data class TransitNetwork(
    val lines: List<TransitLine>,
    val routes: List<TransitRoute>,
) {
    fun line(number: String): TransitLine? = lines.firstOrNull { it.number == number }

    fun route(
        line: String,
        direction: TransitDirection,
    ): TransitRoute? = routes.firstOrNull { it.lineNumber == line && it.direction == direction }

    // Lines whose route in `direction` stops at `stopId`, in catalog order.
    fun linesServing(
        stopId: String,
        direction: TransitDirection,
    ): List<String> = lines.map { it.number }.filter { number -> route(number, direction)?.indexOf(stopId) != null }

    // Where a student can board towards `direction`: every stop before the
    // campus on the way in, deduplicated across lines and kept in route order.
    fun boardingStops(direction: TransitDirection): List<TransitStop> {
        val seen = mutableSetOf<String>()
        val stops = mutableListOf<TransitStop>()
        routes.filter { it.direction == direction }.forEach { route ->
            val end = route.campusStopId?.let(route::indexOf) ?: route.stops.size
            val candidates = if (direction == TransitDirection.ToCampus) {
                route.stops.take(end)
            } else {
                route.stops.take(1)
            }
            candidates.forEach { stop -> if (seen.add(stop.id)) stops += stop }
        }
        return stops
    }

    // The stop every line leaves the campus from.
    val campusDepartureStop: TransitStop?
        get() = boardingStops(TransitDirection.FromCampus).firstOrNull()
}

// What the estimate rests on. `Live`: a bus is on its way. `Waiting`: the bus
// that will make the trip hasn't left its origin. `Historical`: no bus yet,
// only how that departure usually behaves.
enum class TransitArrivalState(
    val wire: String,
) {
    Live("live"),
    Waiting("waiting"),
    Historical("historical"),
    ;

    companion object {
        fun fromWire(value: String): TransitArrivalState? = entries.firstOrNull { it.wire == value }
    }
}

enum class TransitVehiclePhase(
    val wire: String,
) {
    EnRoute("enRoute"),
    AtStop("atStop"),
    AtOrigin("atOrigin"),

    // Still finishing the opposite trip towards this route's origin.
    Inbound("inbound"),
    ;

    companion object {
        fun fromWire(value: String?): TransitVehiclePhase? = entries.firstOrNull { it.wire == value }
    }
}

data class TransitEstimateBasis(
    val sampleCount: Int,
    val dayCount: Int,
    // Share of past trips that reached the stop inside the window.
    val hitRate: Double,
    val histogramStart: Instant,
    val binMinutes: Int,
    val counts: List<Int>,
)

data class TransitArrival(
    val id: String,
    val lineNumber: String,
    val state: TransitArrivalState,
    val phase: TransitVehiclePhase? = null,
    val vehicleId: String? = null,
    val earliest: Instant,
    val latest: Instant,
    val expected: Instant,
    val originDeparture: Instant? = null,
    val originArrival: Instant? = null,
    val campusArrival: Instant? = null,
    val stopsAway: Int? = null,
    val lastFixAt: Instant? = null,
    val signalLost: Boolean = false,
    val lastSeenStopName: String? = null,
    // Official origin departure, "HH:mm".
    val scheduledDeparture: String? = null,
    val ranDays: Int? = null,
    val observedDays: Int? = null,
    val missing: Boolean = false,
    val basis: TransitEstimateBasis? = null,
) {
    // Ran on fewer than four of every five comparable days.
    val isFlaky: Boolean
        get() {
            val ran = ranDays ?: return false
            val observed = observedDays ?: return false
            if (observed < 3) return false
            return ran.toDouble() / observed < 0.8
        }
}

enum class TransitTodaySlotStatus(
    val wire: String,
) {
    Passed("passed"),
    Next("next"),
    Upcoming("upcoming"),
    ;

    companion object {
        fun fromWire(value: String): TransitTodaySlotStatus = entries.firstOrNull { it.wire == value } ?: Upcoming
    }
}

data class TransitTodaySlot(
    val lineNumber: String,
    val usual: Instant,
    val scheduledDeparture: String?,
    val status: TransitTodaySlotStatus,
    val isFlaky: Boolean,
) {
    val id: String get() = "$lineNumber-${usual.epochSeconds}"
}

enum class TransitFeedStatus(
    val wire: String,
) {
    Ok("ok"),

    // The collector fell behind.
    Stale("stale"),

    // SIU stopped answering.
    Unavailable("unavailable"),
    ;

    companion object {
        fun fromWire(value: String): TransitFeedStatus = entries.firstOrNull { it.wire == value } ?: Unavailable
    }
}

data class TransitFeed(
    val status: TransitFeedStatus,
    val lastUpdateAt: Instant?,
)

// First usual passage on the next service day.
data class TransitNextService(
    val lineNumber: String,
    val usual: Instant,
)

// Everything "Meu ponto" shows for one stop, direction and set of lines.
data class TransitStopBoard(
    val generatedAt: Instant,
    val stopId: String,
    val direction: TransitDirection,
    val feed: TransitFeed,
    val arrivals: List<TransitArrival>,
    val today: List<TransitTodaySlot>,
    val serviceToday: Boolean,
    val nextService: TransitNextService?,
)

data class TransitVehicle(
    val id: String,
    val lineNumber: String,
    val location: GeoPoint,
    // Degrees clockwise from north.
    val heading: Double?,
    val fixAt: Instant,
    val progressMeters: Double?,
)

data class TransitFollow(
    val generatedAt: Instant,
    val feed: TransitFeed,
    val vehicles: List<TransitVehicle>,
    // Null once the followed bus passed the stop.
    val arrival: TransitArrival?,
)

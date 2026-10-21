package dev.forcetower.unes.ui.feature.commute

import dev.forcetower.melon.feature.transit.domain.model.CommuteLeg
import dev.forcetower.melon.feature.transit.domain.model.CommutePlan
import dev.forcetower.melon.feature.transit.domain.model.GeoPoint
import dev.forcetower.melon.feature.transit.domain.model.TransitArrival
import dev.forcetower.melon.feature.transit.domain.model.TransitArrivalState
import dev.forcetower.melon.feature.transit.domain.model.TransitDirection
import dev.forcetower.melon.feature.transit.domain.model.TransitEstimateBasis
import dev.forcetower.melon.feature.transit.domain.model.TransitFeed
import dev.forcetower.melon.feature.transit.domain.model.TransitFeedStatus
import dev.forcetower.melon.feature.transit.domain.model.TransitFollow
import dev.forcetower.melon.feature.transit.domain.model.TransitLine
import dev.forcetower.melon.feature.transit.domain.model.TransitNetwork
import dev.forcetower.melon.feature.transit.domain.model.TransitRoute
import dev.forcetower.melon.feature.transit.domain.model.TransitStop
import dev.forcetower.melon.feature.transit.domain.model.TransitStopBoard
import dev.forcetower.melon.feature.transit.domain.model.TransitTodaySlot
import dev.forcetower.melon.feature.transit.domain.model.TransitTodaySlotStatus
import dev.forcetower.melon.feature.transit.domain.model.TransitVehicle
import dev.forcetower.melon.feature.transit.domain.model.TransitVehiclePhase
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// The design's scenario — boarding the 087 at Av. Maria Quitéria, 3203 on a
// weekday morning — laid out relative to `now` for previews (iOS
// `TransitFixtures`). Every stop, id and time is invented.
internal object CommuteFixtures {
    const val MY_STOP_ID = "3203"
    const val CAMPUS_DEPARTURE_STOP_ID = "2452"

    val network: TransitNetwork = run {
        val terminal = stop("4975", "Terminal Central", null, -12.2545, -38.9718)
        val getulio = stop("3174", "Av. Getúlio Vargas", "Mercado de Artes", -12.2553, -38.9655)
        val oi = stop("2011", "Ponto da Oi", "Centro", -12.2571, -38.9641)
        val camara = stop("1963", "Ponto da Câmara", "Centro", -12.2512, -38.9629)
        val mine = stop(MY_STOP_ID, "Av. Maria Quitéria, 3203", "Queimadinha", -12.2385, -38.9597)
        val fragaMaia = stop("5451", "Av. Francisco Fraga Maia", "Parque Ipê", -12.2262, -38.9581)
        val lucas = stop("3723", "Comunidade Lucas da Feira", "Parque Ipê", -12.2158, -38.9612)
        val novoHorizonte = stop("2241", "Av. Transnordestina", "Novo Horizonte", -12.2061, -38.9662)
        val uefs = stop("5670", "PC UEFS", "Novo Horizonte", -12.1986, -38.9657)
        val falcao = stop("1989", "Av. José Falcão da Silva", "Queimadinha", -12.2386, -38.9637)
        val transnordestina = stop("2031", "Av. Transnordestina", "Campo Limpo", -12.2239, -38.9650)
        val departure =
            stop(CAMPUS_DEPARTURE_STOP_ID, "Av. Transnordestina", "Novo Horizonte (Saída)", -12.2019, -38.9741)
        val frente = stop("5788", "Frente da UEFS", "Novo Horizonte", -12.2000, -38.9656)

        val inbound087 = listOf(terminal, getulio, oi, camara, mine, fragaMaia, lucas, novoHorizonte, uefs)
        val inbound088 = listOf(terminal, getulio, oi, camara, mine, transnordestina, novoHorizonte, frente)
        val inbound003 = listOf(terminal, oi, falcao, transnordestina, uefs)

        TransitNetwork(
            lines = listOf(
                TransitLine("003", "UEFS Direta via Terminal Norte"),
                TransitLine("087", "Expresso Universitário"),
                TransitLine("088", "UEFS via Sobradinho"),
            ),
            routes = listOf(
                route("003", TransitDirection.ToCampus, "6", inbound003, uefs.id, 24..28),
                route(
                    "003",
                    TransitDirection.FromCampus,
                    "5",
                    listOf(departure) + inbound003.reversed().drop(1),
                    departure.id,
                    24..29,
                ),
                route("087", TransitDirection.ToCampus, "1826", inbound087, uefs.id, 35..40),
                route(
                    "087",
                    TransitDirection.FromCampus,
                    "1825",
                    listOf(departure) + inbound087.reversed().drop(1),
                    departure.id,
                    33..39,
                ),
                route("088", TransitDirection.ToCampus, "26", inbound088, frente.id, 38..45),
                route(
                    "088",
                    TransitDirection.FromCampus,
                    "25",
                    listOf(departure) + inbound088.reversed().drop(1),
                    departure.id,
                    36..44,
                ),
            ),
        )
    }

    val plan = CommutePlan(
        toCampus = CommuteLeg(
            stopId = MY_STOP_ID,
            stopName = "Av. Maria Quitéria, 3203",
            stopArea = "Queimadinha",
            lineNumbers = listOf("087", "088"),
            walkMinutes = 5,
        ),
        fromCampus = CommuteLeg(
            stopId = CAMPUS_DEPARTURE_STOP_ID,
            stopName = "Av. Transnordestina",
            stopArea = "Novo Horizonte (Saída)",
            lineNumbers = listOf("087", "088", "003"),
            walkMinutes = 4,
        ),
    )

    fun board(
        now: Instant,
        direction: TransitDirection = TransitDirection.ToCampus,
    ): TransitStopBoard =
        TransitStopBoard(
            generatedAt = now,
            stopId = if (direction == TransitDirection.ToCampus) MY_STOP_ID else CAMPUS_DEPARTURE_STOP_ID,
            direction = direction,
            feed = TransitFeed(TransitFeedStatus.Ok, now - 6.seconds),
            arrivals = if (direction == TransitDirection.ToCampus) inboundArrivals(now) else departureArrivals(now),
            today = if (direction == TransitDirection.ToCampus) todayStrip(now) else emptyList(),
            serviceToday = true,
            nextService = null,
        )

    fun follow(now: Instant): TransitFollow {
        val route = network.route("087", TransitDirection.ToCampus)!!
        val from = route.stops[2]
        val to = route.stops[3]
        return TransitFollow(
            generatedAt = now,
            feed = TransitFeed(TransitFeedStatus.Ok, now - 6.seconds),
            vehicles = listOf(
                TransitVehicle(
                    id = "20311",
                    lineNumber = "087",
                    location = GeoPoint(
                        from.location.latitude + (to.location.latitude - from.location.latitude) * 0.4,
                        from.location.longitude + (to.location.longitude - from.location.longitude) * 0.4,
                    ),
                    heading = 20.0,
                    fixAt = now - 6.seconds,
                    progressMeters = from.distanceMeters + (to.distanceMeters - from.distanceMeters) * 0.4,
                ),
                TransitVehicle(
                    id = "20298",
                    lineNumber = "087",
                    location = route.stops[0].location,
                    heading = null,
                    fixAt = now - 12.seconds,
                    progressMeters = 0.0,
                ),
            ),
            arrival = inboundArrivals(now)[0],
        )
    }

    fun basis(now: Instant) =
        TransitEstimateBasis(
            sampleCount = 34,
            dayCount = 7,
            hitRate = 0.9,
            histogramStart = now + 4.minutes,
            binMinutes = 1,
            counts = listOf(1, 2, 4, 7, 11, 9, 5, 3, 2, 1, 1),
        )

    private fun inboundArrivals(now: Instant) =
        listOf(
            TransitArrival(
                id = "087-20311",
                lineNumber = "087",
                state = TransitArrivalState.Live,
                phase = TransitVehiclePhase.EnRoute,
                vehicleId = "20311",
                earliest = now + 8.minutes,
                latest = now + 10.minutes,
                expected = now + 9.minutes,
                campusArrival = now + 22.minutes,
                stopsAway = 4,
                lastFixAt = now - 8.seconds,
                scheduledDeparture = "06:30",
                ranDays = 10,
                observedDays = 10,
                basis = basis(now),
            ),
            TransitArrival(
                id = "088-20298",
                lineNumber = "088",
                state = TransitArrivalState.Waiting,
                phase = TransitVehiclePhase.AtOrigin,
                vehicleId = "20298",
                earliest = now + 12.minutes,
                latest = now + 18.minutes,
                expected = now + 15.minutes,
                originDeparture = now + 5.minutes,
                campusArrival = now + 38.minutes,
                lastFixAt = now - 14.seconds,
                scheduledDeparture = "06:50",
                ranDays = 9,
                observedDays = 10,
            ),
            TransitArrival(
                id = "087-0715",
                lineNumber = "087",
                state = TransitArrivalState.Historical,
                earliest = now + 27.minutes,
                latest = now + 33.minutes,
                expected = now + 30.minutes,
                scheduledDeparture = "07:00",
                ranDays = 10,
                observedDays = 10,
            ),
            TransitArrival(
                id = "088-0730",
                lineNumber = "088",
                state = TransitArrivalState.Historical,
                earliest = now + 41.minutes,
                latest = now + 49.minutes,
                expected = now + 45.minutes,
                scheduledDeparture = "07:20",
                ranDays = 6,
                observedDays = 10,
            ),
        )

    private fun departureArrivals(now: Instant) =
        listOf(
            TransitArrival(
                id = "088-20290",
                lineNumber = "088",
                state = TransitArrivalState.Live,
                phase = TransitVehiclePhase.AtStop,
                vehicleId = "20290",
                earliest = now + 11.minutes,
                latest = now + 13.minutes,
                expected = now + 12.minutes,
                lastFixAt = now - 11.seconds,
                scheduledDeparture = "14:30",
                ranDays = 10,
                observedDays = 10,
            ),
            TransitArrival(
                id = "087-20311",
                lineNumber = "087",
                state = TransitArrivalState.Waiting,
                phase = TransitVehiclePhase.Inbound,
                vehicleId = "20311",
                earliest = now + 17.minutes,
                latest = now + 21.minutes,
                expected = now + 19.minutes,
                originArrival = now + 6.minutes,
                lastFixAt = now - 17.seconds,
                scheduledDeparture = "14:35",
                ranDays = 10,
                observedDays = 10,
            ),
            TransitArrival(
                id = "003-1450",
                lineNumber = "003",
                state = TransitArrivalState.Historical,
                earliest = now + 31.minutes,
                latest = now + 37.minutes,
                expected = now + 34.minutes,
                scheduledDeparture = "14:50",
                ranDays = 5,
                observedDays = 10,
            ),
        )

    private fun todayStrip(now: Instant): List<TransitTodaySlot> {
        val slots = listOf(
            Triple("087", -38, "06:00"),
            Triple("088", -10, "06:30"),
            Triple("087", 8, "06:45"),
            Triple("088", 15, "07:00"),
            Triple("087", 30, "07:15"),
            Triple("088", 45, "07:30"),
            Triple("087", 75, "08:00"),
        )
        return slots.map { (line, offset, scheduled) ->
            TransitTodaySlot(
                lineNumber = line,
                usual = now + offset.minutes,
                scheduledDeparture = scheduled,
                status = when {
                    offset < 0 -> TransitTodaySlotStatus.Passed
                    offset == 8 -> TransitTodaySlotStatus.Next
                    else -> TransitTodaySlotStatus.Upcoming
                },
                isFlaky = offset == 45,
            )
        }
    }

    private fun stop(
        id: String,
        name: String,
        area: String?,
        lat: Double,
        lon: Double,
    ) = TransitStop(id = id, name = name, area = area, location = GeoPoint(lat, lon), distanceMeters = 0.0)

    private fun route(
        line: String,
        direction: TransitDirection,
        itinerary: String,
        stops: List<TransitStop>,
        campusStopId: String,
        typical: IntRange,
    ): TransitRoute {
        var distance = 0.0
        val placed = stops.mapIndexed { index, stop ->
            if (index > 0) distance += stops[index - 1].location.distanceTo(stop.location)
            stop.copy(distanceMeters = distance)
        }
        return TransitRoute(
            lineNumber = line,
            direction = direction,
            itineraryId = itinerary,
            stops = placed,
            path = placed.map { it.location },
            campusStopId = campusStopId,
            typicalMinutes = typical,
        )
    }
}

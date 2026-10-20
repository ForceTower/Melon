import Foundation

/// The design's scenario — boarding the 087 at Av. Maria Quitéria, 3203 on a
/// weekday morning — laid out relative to `now` for previews and the mock
/// repository.
enum TransitFixtures {
    enum BoardMode: Equatable, Sendable {
        case normal
        /// The live bus stopped reporting two minutes ago.
        case signalLost
        /// A usual departure is due but no bus carries it.
        case missing
        case feedUnavailable
        case noService
    }

    static let myStopId = "3203"
    static let campusDepartureStopId = "2452"

    static let network: TransitNetwork = {
        let terminal = stop("4975", "Terminal Central", nil, -12.2545, -38.9718)
        let getulio = stop("3174", "Av. Getúlio Vargas", "Mercado de Artes", -12.2553, -38.9655)
        let oi = stop("2011", "Ponto da Oi", "Centro", -12.2571, -38.9641)
        let camara = stop("1963", "Ponto da Câmara", "Centro", -12.2512, -38.9629)
        let mine = stop(myStopId, "Av. Maria Quitéria, 3203", "Queimadinha", -12.2385, -38.9597)
        let fragaMaia = stop("5451", "Av. Francisco Fraga Maia", "Parque Ipê", -12.2262, -38.9581)
        let lucas = stop("3723", "Comunidade Lucas da Feira", "Parque Ipê", -12.2158, -38.9612)
        let novoHorizonte = stop("2241", "Av. Transnordestina", "Novo Horizonte", -12.2061, -38.9662)
        let uefs = stop("5670", "PC UEFS", "Novo Horizonte", -12.1986, -38.9657)
        let falcao = stop("1989", "Av. José Falcão da Silva", "Queimadinha", -12.2386, -38.9637)
        let transnordestina = stop("2031", "Av. Transnordestina", "Campo Limpo", -12.2239, -38.9650)
        let departure = stop(campusDepartureStopId, "Av. Transnordestina", "Novo Horizonte (Saída)", -12.2019, -38.9741)
        let frente = stop("5788", "Frente da UEFS", "Novo Horizonte", -12.2000, -38.9656)

        let inbound087 = [terminal, getulio, oi, camara, mine, fragaMaia, lucas, novoHorizonte, uefs]
        let inbound088 = [terminal, getulio, oi, camara, mine, transnordestina, novoHorizonte, frente]
        let inbound003 = [terminal, oi, falcao, transnordestina, uefs]

        return TransitNetwork(
            lines: [
                TransitLine(number: "003", name: "UEFS Direta via Terminal Norte"),
                TransitLine(number: "087", name: "Expresso Universitário"),
                TransitLine(number: "088", name: "UEFS via Sobradinho"),
            ],
            routes: [
                route("003", .toCampus, "6", inbound003, campus: uefs.id, typical: 24...28),
                route("003", .fromCampus, "5", [departure] + inbound003.reversed().dropFirst(), campus: departure.id, typical: 24...29),
                route("087", .toCampus, "1826", inbound087, campus: uefs.id, typical: 35...40),
                route("087", .fromCampus, "1825", [departure] + inbound087.reversed().dropFirst(), campus: departure.id, typical: 33...39),
                route("088", .toCampus, "26", inbound088, campus: frente.id, typical: 38...45),
                route("088", .fromCampus, "25", [departure] + inbound088.reversed().dropFirst(), campus: departure.id, typical: 36...44),
            ]
        )
    }()

    static let plan = CommutePlan(
        toCampus: CommuteLeg(
            stopId: myStopId,
            stopName: "Av. Maria Quitéria, 3203",
            stopArea: "Queimadinha",
            lineNumbers: ["087", "088"],
            walkMinutes: 5
        ),
        fromCampus: CommuteLeg(
            stopId: campusDepartureStopId,
            stopName: "Av. Transnordestina",
            stopArea: "Novo Horizonte (Saída)",
            lineNumbers: ["087", "088", "003"],
            walkMinutes: 4
        )
    )

    static func board(now: Date, direction: TransitDirection = .toCampus, mode: BoardMode = .normal) -> TransitStopBoard {
        let minute = { (value: Double) in now.addingTimeInterval(value * 60) }
        let fresh = TransitFeed(status: .ok, lastUpdateAt: now.addingTimeInterval(-6))
        let stopId = direction == .toCampus ? myStopId : campusDepartureStopId

        if mode == .noService {
            let monday = Calendar.current.nextDate(
                after: now,
                matching: DateComponents(hour: 5, minute: 40, weekday: 2),
                matchingPolicy: .nextTime
            ) ?? minute(24 * 60)
            return TransitStopBoard(
                generatedAt: now,
                stopId: stopId,
                direction: direction,
                feed: fresh,
                arrivals: [],
                today: [],
                serviceToday: false,
                nextService: TransitNextService(lineNumber: "087", usual: monday)
            )
        }

        var arrivals = direction == .toCampus ? inboundArrivals(now: now) : departureArrivals(now: now)
        var feed = fresh
        switch mode {
        case .signalLost:
            arrivals[0].signalLost = true
            arrivals[0].lastFixAt = minute(-2)
            arrivals[0].lastSeenStopName = "Ponto da Oi"
        case .missing:
            arrivals[2] = TransitArrival(
                id: "087-0705",
                lineNumber: "087",
                state: .historical,
                earliest: minute(13),
                latest: minute(17),
                expected: minute(15),
                signalLost: false,
                scheduledDeparture: "06:45",
                ranDays: 9,
                observedDays: 10,
                missing: true
            )
        case .feedUnavailable:
            feed = TransitFeed(status: .unavailable, lastUpdateAt: minute(-9))
            arrivals[0].signalLost = true
            arrivals[0].lastFixAt = minute(-9)
            arrivals[0].lastSeenStopName = "Ponto da Oi"
        case .normal, .noService:
            break
        }

        return TransitStopBoard(
            generatedAt: now,
            stopId: stopId,
            direction: direction,
            feed: feed,
            arrivals: arrivals,
            today: direction == .toCampus ? todayStrip(now: now) : [],
            serviceToday: true,
            nextService: nil
        )
    }

    static func follow(now: Date) -> TransitFollow {
        let route = network.route(line: "087", direction: .toCampus)!
        return TransitFollow(
            generatedAt: now,
            feed: TransitFeed(status: .ok, lastUpdateAt: now.addingTimeInterval(-6)),
            vehicles: [
                TransitVehicle(
                    id: "20311",
                    lineNumber: "087",
                    location: midpoint(route.stops[2].location, route.stops[3].location, 0.4),
                    heading: 20,
                    fixAt: now.addingTimeInterval(-6),
                    progressMeters: route.stops[2].distanceMeters
                        + (route.stops[3].distanceMeters - route.stops[2].distanceMeters) * 0.4
                ),
                TransitVehicle(
                    id: "20298",
                    lineNumber: "087",
                    location: route.stops[0].location,
                    heading: nil,
                    fixAt: now.addingTimeInterval(-12),
                    progressMeters: 0
                ),
            ],
            arrival: inboundArrivals(now: now)[0]
        )
    }

    static func basis(now: Date) -> TransitEstimateBasis {
        TransitEstimateBasis(
            sampleCount: 34,
            dayCount: 7,
            hitRate: 0.9,
            histogramStart: now.addingTimeInterval(4 * 60),
            binMinutes: 1,
            counts: [1, 2, 4, 7, 11, 9, 5, 3, 2, 1, 1]
        )
    }

    private static func inboundArrivals(now: Date) -> [TransitArrival] {
        let minute = { (value: Double) in now.addingTimeInterval(value * 60) }
        return [
            TransitArrival(
                id: "087-20311",
                lineNumber: "087",
                state: .live,
                phase: .enRoute,
                vehicleId: "20311",
                earliest: minute(8),
                latest: minute(10),
                expected: minute(9),
                campusArrival: minute(22),
                stopsAway: 4,
                lastFixAt: now.addingTimeInterval(-8),
                signalLost: false,
                scheduledDeparture: "06:30",
                ranDays: 10,
                observedDays: 10,
                missing: false,
                basis: basis(now: now)
            ),
            TransitArrival(
                id: "088-20298",
                lineNumber: "088",
                state: .waiting,
                phase: .atOrigin,
                vehicleId: "20298",
                earliest: minute(12),
                latest: minute(18),
                expected: minute(15),
                originDeparture: minute(5),
                campusArrival: minute(38),
                lastFixAt: now.addingTimeInterval(-14),
                signalLost: false,
                scheduledDeparture: "06:50",
                ranDays: 9,
                observedDays: 10,
                missing: false
            ),
            TransitArrival(
                id: "087-0715",
                lineNumber: "087",
                state: .historical,
                earliest: minute(27),
                latest: minute(33),
                expected: minute(30),
                signalLost: false,
                scheduledDeparture: "07:00",
                ranDays: 10,
                observedDays: 10,
                missing: false
            ),
            TransitArrival(
                id: "088-0730",
                lineNumber: "088",
                state: .historical,
                earliest: minute(41),
                latest: minute(49),
                expected: minute(45),
                signalLost: false,
                scheduledDeparture: "07:20",
                ranDays: 6,
                observedDays: 10,
                missing: false
            ),
        ]
    }

    private static func departureArrivals(now: Date) -> [TransitArrival] {
        let minute = { (value: Double) in now.addingTimeInterval(value * 60) }
        return [
            TransitArrival(
                id: "088-20290",
                lineNumber: "088",
                state: .live,
                phase: .atStop,
                vehicleId: "20290",
                earliest: minute(11),
                latest: minute(13),
                expected: minute(12),
                lastFixAt: now.addingTimeInterval(-11),
                signalLost: false,
                scheduledDeparture: "14:30",
                ranDays: 10,
                observedDays: 10,
                missing: false
            ),
            TransitArrival(
                id: "087-20311",
                lineNumber: "087",
                state: .waiting,
                phase: .inbound,
                vehicleId: "20311",
                earliest: minute(17),
                latest: minute(21),
                expected: minute(19),
                originArrival: minute(6),
                lastFixAt: now.addingTimeInterval(-17),
                signalLost: false,
                scheduledDeparture: "14:35",
                ranDays: 10,
                observedDays: 10,
                missing: false
            ),
            TransitArrival(
                id: "003-1450",
                lineNumber: "003",
                state: .historical,
                earliest: minute(31),
                latest: minute(37),
                expected: minute(34),
                signalLost: false,
                scheduledDeparture: "14:50",
                ranDays: 5,
                observedDays: 10,
                missing: false
            ),
            TransitArrival(
                id: "088-1510",
                lineNumber: "088",
                state: .historical,
                earliest: minute(46),
                latest: minute(52),
                expected: minute(49),
                signalLost: false,
                scheduledDeparture: "15:05",
                ranDays: 10,
                observedDays: 10,
                missing: false
            ),
        ]
    }

    private static func todayStrip(now: Date) -> [TransitTodaySlot] {
        let minute = { (value: Double) in now.addingTimeInterval(value * 60) }
        let slots: [(String, Double, String, Bool)] = [
            ("087", -38, "06:00", false),
            ("088", -10, "06:30", false),
            ("087", 8, "06:45", false),
            ("088", 15, "07:00", false),
            ("087", 30, "07:15", false),
            ("088", 45, "07:30", true),
            ("087", 75, "08:00", false),
            ("088", 110, "08:30", false),
        ]
        return slots.map { line, offset, scheduled, flaky in
            TransitTodaySlot(
                lineNumber: line,
                usual: minute(offset),
                scheduledDeparture: scheduled,
                status: offset < 0 ? .passed : offset == 8 ? .next : .upcoming,
                isFlaky: flaky
            )
        }
    }

    private static func stop(_ id: String, _ name: String, _ area: String?, _ lat: Double, _ lon: Double) -> TransitStop {
        TransitStop(id: id, name: name, area: area, location: GeoPoint(latitude: lat, longitude: lon), distanceMeters: 0)
    }

    private static func route(
        _ line: String,
        _ direction: TransitDirection,
        _ itinerary: String,
        _ stops: [TransitStop],
        campus: String,
        typical: ClosedRange<Int>
    ) -> TransitRoute {
        var distance = 0.0
        var placed: [TransitStop] = []
        for (index, stop) in stops.enumerated() {
            if index > 0 { distance += stops[index - 1].location.distance(to: stop.location) }
            var stop = stop
            stop.distanceMeters = distance
            placed.append(stop)
        }
        return TransitRoute(
            lineNumber: line,
            direction: direction,
            itineraryId: itinerary,
            stops: placed,
            path: placed.map(\.location),
            campusStopId: campus,
            typicalMinutes: typical
        )
    }

    private static func midpoint(_ a: GeoPoint, _ b: GeoPoint, _ fraction: Double) -> GeoPoint {
        GeoPoint(
            latitude: a.latitude + (b.latitude - a.latitude) * fraction,
            longitude: a.longitude + (b.longitude - a.longitude) * fraction
        )
    }
}

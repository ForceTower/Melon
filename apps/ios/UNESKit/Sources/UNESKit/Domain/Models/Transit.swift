import Foundation

/// `toCampus` is "Chegar na UEFS"; `fromCampus` is "Sair da UEFS".
enum TransitDirection: String, Codable, CaseIterable, Equatable, Sendable {
    case toCampus
    case fromCampus
}

struct GeoPoint: Codable, Equatable, Hashable, Sendable {
    var latitude: Double
    var longitude: Double

    /// Haversine distance in metres.
    func distance(to other: GeoPoint) -> Double {
        let radius = 6_371_000.0
        let dLat = (other.latitude - latitude) * .pi / 180
        let dLon = (other.longitude - longitude) * .pi / 180
        let a = sin(dLat / 2) * sin(dLat / 2)
            + cos(latitude * .pi / 180) * cos(other.latitude * .pi / 180) * sin(dLon / 2) * sin(dLon / 2)
        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

struct TransitLine: Equatable, Sendable, Identifiable {
    /// Display number, zero padded ("087").
    var number: String
    var name: String

    var id: String { number }
}

struct TransitStop: Equatable, Sendable, Identifiable {
    var id: String
    /// Street part ("Av. Maria Quitéria, 3203").
    var name: String
    /// Neighbourhood ("Queimadinha").
    var area: String?
    var location: GeoPoint
    /// Along its route from the origin.
    var distanceMeters: Double
}

struct TransitRoute: Equatable, Sendable, Identifiable {
    var lineNumber: String
    var direction: TransitDirection
    var itineraryId: String
    /// Travel order.
    var stops: [TransitStop]
    var path: [GeoPoint]
    var campusStopId: String?
    var typicalMinutes: ClosedRange<Int>?

    var id: String { "\(lineNumber)-\(direction.rawValue)" }

    func index(of stopId: String) -> Int? {
        stops.firstIndex { $0.id == stopId }
    }
}

struct TransitNetwork: Equatable, Sendable {
    var lines: [TransitLine]
    var routes: [TransitRoute]

    func line(_ number: String) -> TransitLine? {
        lines.first { $0.number == number }
    }

    func route(line: String, direction: TransitDirection) -> TransitRoute? {
        routes.first { $0.lineNumber == line && $0.direction == direction }
    }

    /// Lines whose route in `direction` stops at `stopId`, in catalog order.
    func lines(serving stopId: String, direction: TransitDirection) -> [String] {
        lines.map(\.number).filter { number in
            route(line: number, direction: direction)?.index(of: stopId) != nil
        }
    }

    /// Where a student can board towards `direction`: every stop before the
    /// campus on the way in, deduplicated across lines and kept in route order.
    func boardingStops(direction: TransitDirection) -> [TransitStop] {
        var seen = Set<String>()
        var stops: [TransitStop] = []
        for route in routes where route.direction == direction {
            let end = route.campusStopId.flatMap(route.index(of:)) ?? route.stops.endIndex
            let candidates = direction == .toCampus ? Array(route.stops[..<end]) : Array(route.stops.prefix(1))
            for stop in candidates where seen.insert(stop.id).inserted {
                stops.append(stop)
            }
        }
        return stops
    }

    /// The stop every line leaves the campus from.
    var campusDepartureStop: TransitStop? {
        boardingStops(direction: .fromCampus).first
    }
}

/// What the estimate rests on. `live`: a bus is on its way. `waiting`: the bus
/// that will make the trip hasn't left its origin. `historical`: no bus yet,
/// only how that departure usually behaves.
enum TransitArrivalState: String, Equatable, Sendable {
    case live
    case waiting
    case historical
}

enum TransitVehiclePhase: String, Equatable, Sendable {
    case enRoute
    case atStop
    case atOrigin
    /// Still finishing the opposite trip towards this route's origin.
    case inbound
}

struct TransitEstimateBasis: Equatable, Sendable {
    var sampleCount: Int
    var dayCount: Int
    /// Share of past trips that reached the stop inside the window.
    var hitRate: Double
    var histogramStart: Date
    var binMinutes: Int
    var counts: [Int]
}

struct TransitArrival: Equatable, Sendable, Identifiable {
    var id: String
    var lineNumber: String
    var state: TransitArrivalState
    var phase: TransitVehiclePhase?
    var vehicleId: String?
    var earliest: Date
    var latest: Date
    var expected: Date
    var originDeparture: Date?
    var originArrival: Date?
    var campusArrival: Date?
    var stopsAway: Int?
    var lastFixAt: Date?
    var signalLost: Bool
    var lastSeenStopName: String?
    /// Official origin departure, "HH:mm".
    var scheduledDeparture: String?
    var ranDays: Int?
    var observedDays: Int?
    var missing: Bool
    var basis: TransitEstimateBasis?

    /// Ran on fewer than four of every five comparable days.
    var isFlaky: Bool {
        guard let ranDays, let observedDays, observedDays >= 3 else { return false }
        return Double(ranDays) / Double(observedDays) < 0.8
    }
}

enum TransitTodaySlotStatus: String, Equatable, Sendable {
    case passed
    case next
    case upcoming
}

struct TransitTodaySlot: Equatable, Sendable, Identifiable {
    var lineNumber: String
    var usual: Date
    var scheduledDeparture: String?
    var status: TransitTodaySlotStatus
    var isFlaky: Bool

    var id: String { "\(lineNumber)-\(usual.timeIntervalSince1970)" }
}

enum TransitFeedStatus: String, Equatable, Sendable {
    case ok
    /// The collector fell behind.
    case stale
    /// SIU stopped answering.
    case unavailable
}

struct TransitFeed: Equatable, Sendable {
    var status: TransitFeedStatus
    var lastUpdateAt: Date?
}

/// Everything "Meu ponto" shows for one stop, direction and set of lines.
struct TransitStopBoard: Equatable, Sendable {
    var generatedAt: Date
    var stopId: String
    var direction: TransitDirection
    var feed: TransitFeed
    var arrivals: [TransitArrival]
    var today: [TransitTodaySlot]
    var serviceToday: Bool
    var nextService: TransitNextService?
}

/// First usual passage on the next service day.
struct TransitNextService: Equatable, Sendable {
    var lineNumber: String
    var usual: Date
}

struct TransitVehicle: Equatable, Sendable, Identifiable {
    var id: String
    var lineNumber: String
    var location: GeoPoint
    /// Degrees clockwise from north.
    var heading: Double?
    var fixAt: Date
    var progressMeters: Double?
}

struct TransitFollow: Equatable, Sendable {
    var generatedAt: Date
    var feed: TransitFeed
    var vehicles: [TransitVehicle]
    /// Nil once the followed bus passed the stop.
    var arrival: TransitArrival?
}

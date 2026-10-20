import Foundation

// Wire contracts for `api/transit/*` — mirrors `packages/shared-types/src/transit.ts`
// in unes-backrooms. Instants are ISO strings with fractional seconds.

struct TransitNetworkDTO: Decodable {
    var lines: [TransitLineDTO]
    var routes: [TransitRouteDTO]
}

struct TransitLineDTO: Decodable {
    var number: String
    var name: String
}

struct TransitStopDTO: Decodable {
    var id: String
    var name: String
    var area: String?
    var latitude: Double
    var longitude: Double
    var distanceMeters: Double
}

struct TransitRouteDTO: Decodable {
    struct Range: Decodable {
        var low: Int
        var high: Int
    }

    var lineNumber: String
    var direction: String
    var itineraryId: String
    var stops: [TransitStopDTO]
    var path: [[Double]]
    var campusStopId: String?
    var typicalMinutes: Range?
}

struct TransitBasisDTO: Decodable {
    struct Histogram: Decodable {
        var startsAt: String
        var binMinutes: Int
        var counts: [Int]
    }

    var sampleCount: Int
    var dayCount: Int
    var hitRate: Double
    var histogram: Histogram
}

struct TransitArrivalDTO: Decodable {
    struct Window: Decodable {
        var earliest: String
        var latest: String
    }

    struct Reliability: Decodable {
        var ranDays: Int
        var observedDays: Int
    }

    var id: String
    var lineNumber: String
    var state: String
    var phase: String?
    var vehicleId: String?
    var window: Window
    var expected: String
    var originDeparture: String?
    var originArrival: String?
    var campusArrival: String?
    var stopsAway: Int?
    var lastFixAt: String?
    var signalLost: Bool
    var lastSeenStopName: String?
    var scheduledDeparture: String?
    var reliability: Reliability?
    var missing: Bool
    var basis: TransitBasisDTO?
}

struct TransitTodaySlotDTO: Decodable {
    var lineNumber: String
    var usual: String
    var scheduledDeparture: String?
    var status: String
    var flaky: Bool
}

struct TransitFeedDTO: Decodable {
    var status: String
    var lastUpdateAt: String?
}

struct TransitStopArrivalsDTO: Decodable {
    struct NextService: Decodable {
        var lineNumber: String
        var usual: String
    }

    var generatedAt: String
    var stopId: String
    var direction: String
    var feed: TransitFeedDTO
    var arrivals: [TransitArrivalDTO]
    var today: [TransitTodaySlotDTO]
    var serviceToday: Bool
    var nextService: NextService?
}

struct TransitVehicleDTO: Decodable {
    var id: String
    var lineNumber: String
    var latitude: Double
    var longitude: Double
    var heading: Double?
    var fixAt: String
    var progressMeters: Double?
}

struct TransitFollowDTO: Decodable {
    var generatedAt: String
    var feed: TransitFeedDTO
    var vehicles: [TransitVehicleDTO]
    var arrival: TransitArrivalDTO?
}

struct TransitCommuteLegDTO: Decodable {
    var stopId: String
    var stopName: String
    var stopArea: String?
    var lineNumbers: [String]
    var walkMinutes: Int
}

struct TransitCommutePlanDTO: Decodable {
    var toCampus: TransitCommuteLegDTO?
    var fromCampus: TransitCommuteLegDTO?
    var updatedAt: String?
}

/// `PUT api/transit/plan`. Both keys are always sent: null removes a direction.
struct TransitCommutePlanInputDTO: Encodable, Sendable {
    struct Leg: Encodable, Sendable {
        var stopId: String
        var lineNumbers: [String]
        var walkMinutes: Int
    }

    var toCampus: Leg?
    var fromCampus: Leg?

    init(_ plan: CommutePlan) {
        let leg = { (leg: CommuteLeg?) in
            leg.map { Leg(stopId: $0.stopId, lineNumbers: $0.lineNumbers, walkMinutes: $0.walkMinutes) }
        }
        toCampus = leg(plan.toCampus)
        fromCampus = leg(plan.fromCampus)
    }

    private enum CodingKeys: String, CodingKey {
        case toCampus, fromCampus
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(toCampus, forKey: .toCampus)
        try container.encode(fromCampus, forKey: .fromCampus)
    }
}

enum TransitDTOError: Error, Equatable {
    case invalid(String)
}

// MARK: - Mapping

private let fractionalISO8601 = Date.ISO8601FormatStyle(includingFractionalSeconds: true)

private func instant(_ raw: String) throws -> Date {
    if let date = (try? Date(raw, strategy: fractionalISO8601)) ?? (try? Date(raw, strategy: .iso8601)) {
        return date
    }
    throw TransitDTOError.invalid("instant \(raw)")
}

private func optionalInstant(_ raw: String?) throws -> Date? {
    try raw.map(instant)
}

private func parseDirection(_ raw: String) throws -> TransitDirection {
    guard let value = TransitDirection(rawValue: raw) else { throw TransitDTOError.invalid("direction \(raw)") }
    return value
}

extension TransitNetworkDTO {
    func domain() throws -> TransitNetwork {
        TransitNetwork(
            lines: lines.map { TransitLine(number: $0.number, name: $0.name) },
            routes: try routes.map { route in
                TransitRoute(
                    lineNumber: route.lineNumber,
                    direction: try parseDirection(route.direction),
                    itineraryId: route.itineraryId,
                    stops: route.stops.map { stop in
                        TransitStop(
                            id: stop.id,
                            name: stop.name,
                            area: stop.area,
                            location: GeoPoint(latitude: stop.latitude, longitude: stop.longitude),
                            distanceMeters: stop.distanceMeters
                        )
                    },
                    path: route.path.compactMap { pair in
                        pair.count == 2 ? GeoPoint(latitude: pair[0], longitude: pair[1]) : nil
                    },
                    campusStopId: route.campusStopId,
                    typicalMinutes: route.typicalMinutes.flatMap { $0.low <= $0.high ? $0.low...$0.high : nil }
                )
            }
        )
    }
}

extension TransitArrivalDTO {
    func domain() throws -> TransitArrival {
        guard let state = TransitArrivalState(rawValue: state) else { throw TransitDTOError.invalid("state \(state)") }
        return TransitArrival(
            id: id,
            lineNumber: lineNumber,
            state: state,
            // An unknown phase degrades to "no phase" rather than failing the board.
            phase: phase.flatMap(TransitVehiclePhase.init(rawValue:)),
            vehicleId: vehicleId,
            earliest: try instant(window.earliest),
            latest: try instant(window.latest),
            expected: try instant(expected),
            originDeparture: try optionalInstant(originDeparture),
            originArrival: try optionalInstant(originArrival),
            campusArrival: try optionalInstant(campusArrival),
            stopsAway: stopsAway,
            lastFixAt: try optionalInstant(lastFixAt),
            signalLost: signalLost,
            lastSeenStopName: lastSeenStopName,
            scheduledDeparture: scheduledDeparture,
            ranDays: reliability?.ranDays,
            observedDays: reliability?.observedDays,
            missing: missing,
            basis: try basis.map { basis in
                TransitEstimateBasis(
                    sampleCount: basis.sampleCount,
                    dayCount: basis.dayCount,
                    hitRate: basis.hitRate,
                    histogramStart: try instant(basis.histogram.startsAt),
                    binMinutes: basis.histogram.binMinutes,
                    counts: basis.histogram.counts
                )
            }
        )
    }
}

extension TransitFeedDTO {
    func domain() throws -> TransitFeed {
        TransitFeed(
            status: TransitFeedStatus(rawValue: status) ?? .unavailable,
            lastUpdateAt: try optionalInstant(lastUpdateAt)
        )
    }
}

extension TransitStopArrivalsDTO {
    func domain() throws -> TransitStopBoard {
        TransitStopBoard(
            generatedAt: try instant(generatedAt),
            stopId: stopId,
            direction: try parseDirection(direction),
            feed: try feed.domain(),
            arrivals: try arrivals.map { try $0.domain() },
            today: try today.map { slot in
                TransitTodaySlot(
                    lineNumber: slot.lineNumber,
                    usual: try instant(slot.usual),
                    scheduledDeparture: slot.scheduledDeparture,
                    status: TransitTodaySlotStatus(rawValue: slot.status) ?? .upcoming,
                    isFlaky: slot.flaky
                )
            },
            serviceToday: serviceToday,
            nextService: try nextService.map { TransitNextService(lineNumber: $0.lineNumber, usual: try instant($0.usual)) }
        )
    }
}

extension TransitCommutePlanDTO {
    var domain: CommutePlan {
        let leg = { (dto: TransitCommuteLegDTO?) in
            dto.map {
                CommuteLeg(
                    stopId: $0.stopId,
                    stopName: $0.stopName,
                    stopArea: $0.stopArea,
                    lineNumbers: $0.lineNumbers,
                    walkMinutes: $0.walkMinutes
                )
            }
        }
        return CommutePlan(toCampus: leg(toCampus), fromCampus: leg(fromCampus))
    }
}

extension TransitFollowDTO {
    func domain() throws -> TransitFollow {
        TransitFollow(
            generatedAt: try instant(generatedAt),
            feed: try feed.domain(),
            vehicles: try vehicles.map { vehicle in
                TransitVehicle(
                    id: vehicle.id,
                    lineNumber: vehicle.lineNumber,
                    location: GeoPoint(latitude: vehicle.latitude, longitude: vehicle.longitude),
                    heading: vehicle.heading,
                    fixAt: try instant(vehicle.fixAt),
                    progressMeters: vehicle.progressMeters
                )
            },
            arrival: try arrival?.domain()
        )
    }
}

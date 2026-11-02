#if os(iOS)
import ActivityKit
#endif
import Foundation

/// "Seguindo o 087" outside the app. The fixed part is what the app knew when
/// the student started following; `ContentState` is what the backend pushes
/// from then on (`contracts/v1/transit.json` → `liveActivity.content`).
struct CommuteActivityAttributes: Codable, Hashable, Sendable {
    struct Stop: Codable, Hashable, Sendable {
        /// "Terminal", "UEFS" or the street, as the route line labels it.
        var name: String
        var distanceMeters: Double
    }

    var lineNumber: String
    var lineName: String
    var direction: TransitDirection
    var stopId: String
    var arrivalId: String
    /// The route line, origin first: up to the student's stop, or on to the
    /// end when they board where the line starts.
    var stops: [Stop]
    var mineIndex: Int

    /// The estimate is when the bus leaves, not when it arrives.
    var boardsAtOrigin: Bool { mineIndex == 0 }

    struct ContentState: Codable, Hashable, Sendable {
        enum Status: String, Codable, Sendable {
            case live
            case waiting
            case historical
            /// The followed bus went by the stop.
            case passed
            /// The departure stopped running, or following it timed out.
            case ended
        }

        /// Unix seconds: ActivityKit decodes pushes with its own decoder, so
        /// instants travel as plain numbers.
        struct Window: Codable, Hashable, Sendable {
            var earliest: TimeInterval
            var latest: TimeInterval
            var expected: TimeInterval
        }

        var status: Status
        /// Nil once the activity ended.
        var window: Window?
        var originDeparture: TimeInterval?
        var campusArrival: TimeInterval?
        var stopsAway: Int?
        var atStop: Bool
        /// The followed bus along the route; nil while it is not on it.
        var progressMeters: Double?
        var signalLost: Bool
    }
}

extension CommuteActivityAttributes {
    /// Where the followed bus sits on the route line, as a fractional stop index.
    func busPosition(_ state: ContentState) -> Double? {
        state.progressMeters.flatMap { TransitRoute.fractionalIndex($0, distances: stops.map(\.distanceMeters)) }
    }
}

extension CommuteActivityAttributes.ContentState {
    init(arrival: TransitArrival, progressMeters: Double?) {
        self.init(
            status: Status(rawValue: arrival.state.rawValue) ?? .historical,
            window: Window(
                earliest: arrival.earliest.timeIntervalSince1970,
                latest: arrival.latest.timeIntervalSince1970,
                expected: arrival.expected.timeIntervalSince1970
            ),
            originDeparture: arrival.originDeparture?.timeIntervalSince1970,
            campusArrival: arrival.campusArrival?.timeIntervalSince1970,
            stopsAway: arrival.stopsAway,
            atStop: arrival.phase == .atStop,
            progressMeters: progressMeters,
            signalLost: arrival.signalLost
        )
    }

    /// The followed departure as the app's estimate rules read it; nil once
    /// the activity ended.
    func arrival(for attributes: CommuteActivityAttributes) -> TransitArrival? {
        guard let window, let state = TransitArrivalState(rawValue: status.rawValue) else { return nil }
        let date = { (seconds: TimeInterval) in Date(timeIntervalSince1970: seconds) }
        return TransitArrival(
            id: attributes.arrivalId,
            lineNumber: attributes.lineNumber,
            state: state,
            phase: atStop ? .atStop : nil,
            earliest: date(window.earliest),
            latest: date(window.latest),
            expected: date(window.expected),
            originDeparture: originDeparture.map(date),
            campusArrival: campusArrival.map(date),
            stopsAway: stopsAway,
            signalLost: signalLost,
            missing: false
        )
    }
}

#if os(iOS)
extension CommuteActivityAttributes: ActivityAttributes {}
#endif

import Foundation

/// One saved direction of "Meu trajeto": where the student boards and which
/// lines they actually take there.
struct CommuteLeg: Codable, Hashable, Sendable {
    var stopId: String
    var stopName: String
    var stopArea: String?
    var lineNumbers: [String]
    /// From home (or class) to the stop.
    var walkMinutes: Int
}

/// The student's saved commute. The server owns it (`api/transit/plan`); the
/// device mirrors whatever the server last answered.
struct CommutePlan: Codable, Hashable, Sendable {
    var toCampus: CommuteLeg?
    var fromCampus: CommuteLeg?

    var isEmpty: Bool { toCampus == nil && fromCampus == nil }

    var directions: [TransitDirection] {
        TransitDirection.allCases.filter { leg(for: $0) != nil }
    }

    func leg(for direction: TransitDirection) -> CommuteLeg? {
        switch direction {
        case .toCampus: toCampus
        case .fromCampus: fromCampus
        }
    }

    mutating func setLeg(_ leg: CommuteLeg?, for direction: TransitDirection) {
        switch direction {
        case .toCampus: toCampus = leg
        case .fromCampus: fromCampus = leg
        }
    }
}

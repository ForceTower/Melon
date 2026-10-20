import ComposableArchitecture
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

extension SharedKey where Self == FileStorageKey<CommutePlan>.Default {
    /// Device-local and wiped on logout: a boarding stop says where someone lives.
    static var commutePlan: Self {
        Self[.fileStorage(.applicationSupportDirectory.appending(component: "commute-plan.json")), default: CommutePlan()]
    }
}

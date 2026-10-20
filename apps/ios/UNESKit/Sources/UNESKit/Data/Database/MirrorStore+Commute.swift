import Foundation
import GRDB

// MARK: - Meu trajeto (one row per saved direction)

/// A saved leg as the server last answered it. `lineNumbers` is stored as a
/// JSON array by GRDB's Codable support.
struct CommuteLegRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "commuteLegs"
    var direction: String
    var stopId: String
    var stopName: String
    var stopArea: String?
    var lineNumbers: [String]
    var walkMinutes: Int

    init(_ leg: CommuteLeg, direction: TransitDirection) {
        self.direction = direction.rawValue
        stopId = leg.stopId
        stopName = leg.stopName
        stopArea = leg.stopArea
        lineNumbers = leg.lineNumbers
        walkMinutes = leg.walkMinutes
    }

    var leg: CommuteLeg {
        CommuteLeg(
            stopId: stopId,
            stopName: stopName,
            stopArea: stopArea,
            lineNumbers: lineNumbers,
            walkMinutes: walkMinutes
        )
    }
}

extension MirrorStore {
    /// Replaces the mirrored plan with the server's; an unchanged plan writes
    /// nothing, so observers don't wake for no-op refreshes.
    func applyCommutePlan(_ plan: CommutePlan) async throws {
        try await writer.write { db in
            guard try Self.commutePlan(db) != plan else { return }
            try CommuteLegRecord.deleteAll(db)
            for direction in plan.directions {
                guard let leg = plan.leg(for: direction) else { continue }
                try CommuteLegRecord(leg, direction: direction).insert(db)
            }
        }
    }

    func cachedCommutePlan() async throws -> CommutePlan {
        try await writer.read { db in try Self.commutePlan(db) }
    }

    /// Emits the mirrored plan on subscription and after every write that
    /// changes it — a save from any screen, a refresh, a logout wipe.
    func commutePlanUpdates() -> AsyncValueObservation<CommutePlan> {
        ValueObservation
            .tracking { db in try Self.commutePlan(db) }
            .removeDuplicates()
            .values(in: writer)
    }

    private static func commutePlan(_ db: Database) throws -> CommutePlan {
        var plan = CommutePlan()
        for record in try CommuteLegRecord.fetchAll(db) {
            guard let direction = TransitDirection(rawValue: record.direction) else { continue }
            plan.setLeg(record.leg, for: direction)
        }
        return plan
    }
}

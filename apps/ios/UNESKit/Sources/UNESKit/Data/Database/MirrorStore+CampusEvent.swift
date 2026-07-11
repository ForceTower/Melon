import Foundation
import GRDB

// MARK: - Featured campus event (one JSON snapshot row)

/// The mirrored featured event. The domain payload is stored whole as JSON:
/// it's read whole, replaced whole on publish, and never queried by column —
/// only `id`/`revision` are materialized to detect real changes.
struct CampusEventRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "campusEvent"
    var id: String
    var revision: Int
    var payload: String
    var syncedAt: String

    init(event: CampusEvent, syncedAt: Date) throws {
        id = event.id
        revision = event.revision
        payload = String(decoding: try Self.encoder.encode(event), as: UTF8.self)
        self.syncedAt = syncedAt.formatted(MirrorStore.timestampFormat)
    }

    /// nil when the stored payload no longer decodes (model evolution) —
    /// rendered as "nothing featured" until the next refresh rewrites it.
    var event: CampusEvent? {
        try? Self.decoder.decode(CampusEvent.self, from: Data(payload.utf8))
    }

    private static let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }()

    private static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }()
}

extension MirrorStore {
    /// Replaces the mirrored event with one refresh result: nil clears it
    /// (nothing featured), an unchanged (id, revision) pair writes nothing so
    /// observers don't wake for no-op refreshes.
    func applyCampusEvent(_ event: CampusEvent?, syncedAt: Date) async throws {
        try await writer.write { db in
            guard let event else {
                try CampusEventRecord.deleteAll(db)
                return
            }
            if let current = try CampusEventRecord.fetchOne(db),
               current.id == event.id, current.revision == event.revision {
                return
            }
            try CampusEventRecord.deleteAll(db)
            try CampusEventRecord(event: event, syncedAt: syncedAt).insert(db)
        }
    }

    /// The featured event as mirrored on disk; nil when nothing is featured
    /// (or nothing was fetched yet).
    func cachedCampusEvent() async throws -> CampusEvent? {
        try await writer.read { db in try CampusEventRecord.fetchOne(db)?.event }
    }

    /// Emits the mirrored event on subscription and again after every write
    /// that changes it — refreshes that land a new revision, un-featuring,
    /// logout wipes.
    func campusEventUpdates() -> AsyncValueObservation<CampusEvent?> {
        ValueObservation
            .tracking { db in try CampusEventRecord.fetchOne(db)?.event }
            .values(in: writer)
    }
}

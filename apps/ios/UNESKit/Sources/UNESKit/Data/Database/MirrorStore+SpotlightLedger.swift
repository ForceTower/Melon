import Foundation
import GRDB

// MARK: - Spotlight ledger persistence

extension MirrorStore {
    private static let ledgerVersionKey = "version"
    private static let ledgerPlatformKey = "platform"
    private static let disciplineKind = "discipline"
    private static let messageKind = "message"
    private static let evaluationKind = "evaluation"
    private static let lectureKind = "lecture"
    private static let sessionKind = "session"
    private static let personalEventKind = "personalEvent"

    /// Nil means no usable ledger — the caller must wipe the index before
    /// re-indexing, because the old items' identifier formats may differ.
    /// That covers four cases: another schema version, another OS major
    /// (the app target maps the same projections onto different entity
    /// types per OS), a missing version row (a DEBUG
    /// `eraseDatabaseOnSchemaChange` empties these tables while Spotlight
    /// keeps its entries — an empty read must not masquerade as a fresh
    /// install), and the pre-Phase-3 JSON-file ledger, whose presence marks
    /// a legacy install.
    func spotlightLedger() async throws -> SpotlightIndexLedger? {
        // File check outside the database access: the marker is deleted by
        // the caller only after the wipe succeeds, so a failed wipe keeps
        // its signal for the next launch.
        if Self.legacySpotlightLedgerFileExists { return nil }
        return try await writer.read { db -> SpotlightIndexLedger? in
            guard let stored = try SpotlightLedgerStateRecord.fetchOne(db, key: Self.ledgerVersionKey),
                  stored.value == String(SpotlightIndexLedger.schemaVersion),
                  let platform = try SpotlightLedgerStateRecord.fetchOne(db, key: Self.ledgerPlatformKey),
                  platform.value == String(SpotlightIndexLedger.currentPlatform)
            else { return nil }
            var ledger = SpotlightIndexLedger()
            for row in try SpotlightLedgerRecord.fetchAll(db) {
                switch row.kind {
                case Self.disciplineKind: ledger.disciplines[row.identifier] = row.digest
                case Self.messageKind: ledger.messages[row.identifier] = row.digest
                case Self.evaluationKind: ledger.evaluations[row.identifier] = row.digest
                case Self.lectureKind: ledger.lectures[row.identifier] = row.digest
                case Self.sessionKind: ledger.sessions[row.identifier] = row.digest
                case Self.personalEventKind: ledger.personalEvents[row.identifier] = row.digest
                default: break
                }
            }
            return ledger
        }
    }

    /// Transactional full rewrite — the row set stays exactly in step with
    /// the in-memory value, and the ledger is small enough that a delta
    /// upsert isn't worth the code.
    func saveSpotlightLedger(_ ledger: SpotlightIndexLedger) async throws {
        try await writer.write { db in
            try SpotlightLedgerRecord.deleteAll(db)
            let kinds = [
                (Self.disciplineKind, ledger.disciplines),
                (Self.messageKind, ledger.messages),
                (Self.evaluationKind, ledger.evaluations),
                (Self.lectureKind, ledger.lectures),
                (Self.sessionKind, ledger.sessions),
                (Self.personalEventKind, ledger.personalEvents),
            ]
            for (kind, digests) in kinds {
                for (identifier, digest) in digests {
                    try SpotlightLedgerRecord(identifier: identifier, kind: kind, digest: digest).insert(db)
                }
            }
            try SpotlightLedgerStateRecord(key: Self.ledgerVersionKey, value: String(ledger.version)).upsert(db)
            try SpotlightLedgerStateRecord(key: Self.ledgerPlatformKey, value: String(ledger.platform)).upsert(db)
        }
    }

    // MARK: Legacy JSON ledger (pre-Phase 3)

    private static var legacySpotlightLedgerFileURL: URL? {
        try? FileManager.default
            .url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            .appending(path: "spotlight-index-ledger.json")
    }

    private static var legacySpotlightLedgerFileExists: Bool {
        legacySpotlightLedgerFileURL.map { FileManager.default.fileExists(atPath: $0.path) } ?? false
    }

    static func deleteLegacySpotlightLedgerFile() {
        guard let url = legacySpotlightLedgerFileURL else { return }
        try? FileManager.default.removeItem(at: url)
    }
}

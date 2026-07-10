import Foundation
import GRDB

// MARK: - Spotlight ledger persistence

extension MirrorStore {
    private static let ledgerVersionKey = "version"
    private static let disciplineKind = "discipline"
    private static let messageKind = "message"
    private static let evaluationKind = "evaluation"

    /// Nil means no usable ledger — the caller must wipe the index before
    /// re-indexing, because the old items' identifier formats may differ.
    /// That covers three cases: another schema version, a missing version
    /// row (a DEBUG `eraseDatabaseOnSchemaChange` empties these tables while
    /// Spotlight keeps its entries — an empty read must not masquerade as a
    /// fresh install), and the pre-Phase-3 JSON-file ledger, whose presence
    /// marks a legacy install.
    func spotlightLedger() async throws -> SpotlightIndexLedger? {
        // File check outside the database access: the marker is deleted by
        // the caller only after the wipe succeeds, so a failed wipe keeps
        // its signal for the next launch.
        if Self.legacySpotlightLedgerFileExists { return nil }
        return try await writer.read { db -> SpotlightIndexLedger? in
            guard let stored = try SpotlightLedgerStateRecord.fetchOne(db, key: Self.ledgerVersionKey),
                  stored.value == String(SpotlightIndexLedger.schemaVersion)
            else { return nil }
            var ledger = SpotlightIndexLedger()
            for row in try SpotlightLedgerRecord.fetchAll(db) {
                switch row.kind {
                case Self.disciplineKind: ledger.disciplines[row.identifier] = row.digest
                case Self.messageKind: ledger.messages[row.identifier] = row.digest
                case Self.evaluationKind: ledger.evaluations[row.identifier] = row.digest
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
            ]
            for (kind, digests) in kinds {
                for (identifier, digest) in digests {
                    try SpotlightLedgerRecord(identifier: identifier, kind: kind, digest: digest).insert(db)
                }
            }
            try SpotlightLedgerStateRecord(key: Self.ledgerVersionKey, value: String(ledger.version)).upsert(db)
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

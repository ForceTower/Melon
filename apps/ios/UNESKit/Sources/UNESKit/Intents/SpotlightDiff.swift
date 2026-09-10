import CryptoKit
import Foundation

/// What one projection emission changes in the index — a pure function of
/// (ledger, snapshot); no file or index I/O in here.
struct SpotlightDiff: Equatable, Sendable {
    var disciplinesToIndex: [SpotlightDiscipline] = []
    var disciplineIdsToDelete: [String] = []
    var messagesToIndex: [SpotlightMessage] = []
    var messageIdsToDelete: [String] = []
    var evaluationsToIndex: [SpotlightEvaluation] = []
    var evaluationIdsToDelete: [String] = []
    var lecturesToIndex: [SpotlightLecture] = []
    var lectureIdsToDelete: [String] = []
    var sessionsToIndex: [SpotlightSession] = []
    var sessionIdsToDelete: [String] = []
    var personalEventsToIndex: [SpotlightPersonalEvent] = []
    var personalEventIdsToDelete: [String] = []
    /// The mirror was wiped while the index still has entries.
    var wipeAll = false

    var isEmpty: Bool {
        disciplinesToIndex.isEmpty && disciplineIdsToDelete.isEmpty
            && messagesToIndex.isEmpty && messageIdsToDelete.isEmpty
            && evaluationsToIndex.isEmpty && evaluationIdsToDelete.isEmpty
            && lecturesToIndex.isEmpty && lectureIdsToDelete.isEmpty
            && sessionsToIndex.isEmpty && sessionIdsToDelete.isEmpty
            && personalEventsToIndex.isEmpty && personalEventIdsToDelete.isEmpty
            && !wipeAll
    }

    static func compute(ledger: SpotlightIndexLedger, snapshot: SpotlightSnapshot?) -> SpotlightDiff {
        guard let snapshot else {
            // A signed-out fresh install (empty ledger) never issues delete-alls.
            return SpotlightDiff(wipeAll: !ledger.isEmpty)
        }
        var diff = SpotlightDiff()
        (diff.disciplinesToIndex, diff.disciplineIdsToDelete) = delta(ledger.disciplines, snapshot.disciplines)
        (diff.messagesToIndex, diff.messageIdsToDelete) = delta(ledger.messages, snapshot.messages)
        (diff.evaluationsToIndex, diff.evaluationIdsToDelete) = delta(ledger.evaluations, snapshot.evaluations)
        (diff.lecturesToIndex, diff.lectureIdsToDelete) = delta(ledger.lectures, snapshot.lectures)
        (diff.sessionsToIndex, diff.sessionIdsToDelete) = delta(ledger.sessions, snapshot.sessions)
        (diff.personalEventsToIndex, diff.personalEventIdsToDelete) = delta(ledger.personalEvents, snapshot.personalEvents)
        return diff
    }

    /// Items whose digest changed (or are new), and ledger ids no longer
    /// projected — sorted so the diff is deterministic.
    private static func delta<Item: Encodable & Identifiable>(
        _ digests: [String: String],
        _ items: [Item]
    ) -> (toIndex: [Item], toDelete: [String]) where Item.ID == String {
        var current: Set<String> = []
        var toIndex: [Item] = []
        for item in items {
            current.insert(item.id)
            if digests[item.id] != SpotlightIndexLedger.digest(of: item) {
                toIndex.append(item)
            }
        }
        return (toIndex, digests.keys.filter { !current.contains($0) }.sorted())
    }
}

/// id → stable content digest of everything ever indexed, persisted (as the
/// mirror's `spotlightLedger` table, behind `MirrorStore`) so app launches
/// are delta-only: the launch emission diffs against what previous runs
/// indexed instead of re-sending the whole mirror to the index.
struct SpotlightIndexLedger: Equatable, Codable, Sendable {
    /// Bump when the app target's projection → index-item mapping changes:
    /// digests only cover projected content, so a mapping change must force
    /// a wipe + full re-index of otherwise-unchanged items.
    /// 3: messages became classic searchable items (entity-created items
    /// never full-text match on their body) with bare-id identifiers.
    /// 4: evaluations joined the index, and the ledger moved from the JSON
    /// file into the mirror database.
    /// 5: `deleteAll` became a true delete-all — the old domain-based wipe
    /// never reached entity-created items, so pre-3 message entities
    /// survived every schema wipe and lingered as stale duplicates.
    /// 6: discipline items gained the code as a Spotlight alternate name —
    /// an attribute-set-only change the digests can't see.
    /// 7: Phase 4 — disciplines carry typed properties and the teacher as
    /// an alternate name; lectures, sessions, and personal events joined;
    /// on iOS 27 evaluations, sessions, personal events, and messages are
    /// written as schema entities.
    /// 8: on iOS 27 sessions became visible and messages entity-only (the
    /// hidden twins never reached Siri AI's index) — an attribute-level
    /// change the digests can't see.
    static let schemaVersion = 8

    /// The OS major the ledger was written under. The app target picks
    /// entity types by `#available`, so an OS upgrade changes what the
    /// same digests map to — a mismatch is treated like a schema bump.
    static var currentPlatform: Int { ProcessInfo.processInfo.operatingSystemVersion.majorVersion }

    var version: Int = SpotlightIndexLedger.schemaVersion
    var platform: Int = SpotlightIndexLedger.currentPlatform
    var disciplines: [String: String] = [:]
    var messages: [String: String] = [:]
    var evaluations: [String: String] = [:]
    var lectures: [String: String] = [:]
    var sessions: [String: String] = [:]
    var personalEvents: [String: String] = [:]

    var isEmpty: Bool {
        disciplines.isEmpty && messages.isEmpty && evaluations.isEmpty
            && lectures.isEmpty && sessions.isEmpty && personalEvents.isEmpty
    }

    /// The kinds apply separately so one failed index write only holds
    /// back its own kind's ledger update (and retry).
    mutating func applyDisciplines(_ diff: SpotlightDiff) {
        Self.apply(&disciplines, indexed: diff.disciplinesToIndex, deleted: diff.disciplineIdsToDelete)
    }

    mutating func applyMessages(_ diff: SpotlightDiff) {
        Self.apply(&messages, indexed: diff.messagesToIndex, deleted: diff.messageIdsToDelete)
    }

    mutating func applyEvaluations(_ diff: SpotlightDiff) {
        Self.apply(&evaluations, indexed: diff.evaluationsToIndex, deleted: diff.evaluationIdsToDelete)
    }

    mutating func applyLectures(_ diff: SpotlightDiff) {
        Self.apply(&lectures, indexed: diff.lecturesToIndex, deleted: diff.lectureIdsToDelete)
    }

    mutating func applySessions(_ diff: SpotlightDiff) {
        Self.apply(&sessions, indexed: diff.sessionsToIndex, deleted: diff.sessionIdsToDelete)
    }

    mutating func applyPersonalEvents(_ diff: SpotlightDiff) {
        Self.apply(&personalEvents, indexed: diff.personalEventsToIndex, deleted: diff.personalEventIdsToDelete)
    }

    private static func apply<Item: Encodable & Identifiable>(
        _ digests: inout [String: String],
        indexed: [Item],
        deleted: [String]
    ) where Item.ID == String {
        for item in indexed {
            digests[item.id] = digest(of: item)
        }
        for id in deleted {
            digests[id] = nil
        }
    }

    /// SHA-256 over the sorted-keys JSON encoding — Swift's `hashValue` is
    /// process-seeded and can't be persisted.
    static func digest(of value: some Encodable) -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        guard let data = try? encoder.encode(value) else { return "" }
        return SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined()
    }
}

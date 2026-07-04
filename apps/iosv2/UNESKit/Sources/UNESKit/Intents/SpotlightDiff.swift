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
    /// The mirror was wiped while the index still has entries.
    var wipeAll = false

    var isEmpty: Bool {
        disciplinesToIndex.isEmpty && disciplineIdsToDelete.isEmpty
            && messagesToIndex.isEmpty && messageIdsToDelete.isEmpty
            && evaluationsToIndex.isEmpty && evaluationIdsToDelete.isEmpty && !wipeAll
    }

    static func compute(ledger: SpotlightIndexLedger, snapshot: SpotlightSnapshot?) -> SpotlightDiff {
        guard let snapshot else {
            // A signed-out fresh install (empty ledger) never issues delete-alls.
            return SpotlightDiff(wipeAll: !ledger.isEmpty)
        }
        var diff = SpotlightDiff()
        var currentDisciplineIds: Set<String> = []
        for discipline in snapshot.disciplines {
            currentDisciplineIds.insert(discipline.id)
            if ledger.disciplines[discipline.id] != SpotlightIndexLedger.digest(of: discipline) {
                diff.disciplinesToIndex.append(discipline)
            }
        }
        diff.disciplineIdsToDelete = ledger.disciplines.keys
            .filter { !currentDisciplineIds.contains($0) }.sorted()

        var currentMessageIds: Set<String> = []
        for message in snapshot.messages {
            currentMessageIds.insert(message.id)
            if ledger.messages[message.id] != SpotlightIndexLedger.digest(of: message) {
                diff.messagesToIndex.append(message)
            }
        }
        diff.messageIdsToDelete = ledger.messages.keys
            .filter { !currentMessageIds.contains($0) }.sorted()

        var currentEvaluationIds: Set<String> = []
        for evaluation in snapshot.evaluations {
            currentEvaluationIds.insert(evaluation.id)
            if ledger.evaluations[evaluation.id] != SpotlightIndexLedger.digest(of: evaluation) {
                diff.evaluationsToIndex.append(evaluation)
            }
        }
        diff.evaluationIdsToDelete = ledger.evaluations.keys
            .filter { !currentEvaluationIds.contains($0) }.sorted()
        return diff
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
    static let schemaVersion = 4

    var version: Int = SpotlightIndexLedger.schemaVersion
    var disciplines: [String: String] = [:]
    var messages: [String: String] = [:]
    var evaluations: [String: String] = [:]

    var isEmpty: Bool { disciplines.isEmpty && messages.isEmpty && evaluations.isEmpty }

    /// The two kinds apply separately so one failed index write only holds
    /// back its own kind's ledger update (and retry).
    mutating func applyDisciplines(_ diff: SpotlightDiff) {
        for discipline in diff.disciplinesToIndex {
            disciplines[discipline.id] = Self.digest(of: discipline)
        }
        for id in diff.disciplineIdsToDelete {
            disciplines[id] = nil
        }
    }

    mutating func applyMessages(_ diff: SpotlightDiff) {
        for message in diff.messagesToIndex {
            messages[message.id] = Self.digest(of: message)
        }
        for id in diff.messageIdsToDelete {
            messages[id] = nil
        }
    }

    mutating func applyEvaluations(_ diff: SpotlightDiff) {
        for evaluation in diff.evaluationsToIndex {
            evaluations[evaluation.id] = Self.digest(of: evaluation)
        }
        for id in diff.evaluationIdsToDelete {
            evaluations[id] = nil
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

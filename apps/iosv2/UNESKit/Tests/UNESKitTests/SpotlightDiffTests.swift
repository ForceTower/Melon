import Foundation
import Testing

@testable import UNESKit

/// The pure diff between the persisted ledger and a projection emission,
/// and the apply step's failure semantics.
struct SpotlightDiffTests {
    private func discipline(_ id: String, title: String = "Algoritmos I") -> SpotlightDiscipline {
        SpotlightDiscipline(
            id: "discipline/sem1/\(id)", semesterId: "sem1", disciplineId: id,
            title: title, code: "ALGI", subtitle: "ALGI · qui · 08:00", keywords: ["ALGI"]
        )
    }

    private func message(_ id: String, body: String = "corpo") -> SpotlightMessage {
        SpotlightMessage(
            id: "message/\(id)", messageId: id, title: "Aviso",
            subtitle: "UNES · 10 de abr. de 2026", body: body, keywords: []
        )
    }

    /// A ledger that has fully applied `snapshot`.
    private func ledger(for snapshot: SpotlightSnapshot) -> SpotlightIndexLedger {
        var ledger = SpotlightIndexLedger()
        let diff = SpotlightDiff.compute(ledger: ledger, snapshot: snapshot)
        ledger.applyDisciplines(diff)
        ledger.applyMessages(diff)
        return ledger
    }

    @Test
    func emptyLedgerIndexesEverything() {
        let snapshot = SpotlightSnapshot(disciplines: [discipline("d1")], messages: [message("m1")])

        let diff = SpotlightDiff.compute(ledger: SpotlightIndexLedger(), snapshot: snapshot)

        #expect(diff.disciplinesToIndex == [discipline("d1")])
        #expect(diff.messagesToIndex == [message("m1")])
        #expect(diff.disciplineIdsToDelete.isEmpty)
        #expect(diff.messageIdsToDelete.isEmpty)
        #expect(!diff.wipeAll)
    }

    @Test
    func identicalSnapshotProducesNoWork() {
        let snapshot = SpotlightSnapshot(disciplines: [discipline("d1")], messages: [message("m1")])

        let diff = SpotlightDiff.compute(ledger: ledger(for: snapshot), snapshot: snapshot)

        #expect(diff.isEmpty)
    }

    @Test
    func contentChangeReindexesExactlyThatItem() {
        let before = SpotlightSnapshot(
            disciplines: [discipline("d1")],
            messages: [message("m1"), message("m2")]
        )
        let after = SpotlightSnapshot(
            disciplines: [discipline("d1")],
            messages: [message("m1"), message("m2", body: "corpo editado")]
        )

        let diff = SpotlightDiff.compute(ledger: ledger(for: before), snapshot: after)

        #expect(diff.disciplinesToIndex.isEmpty)
        #expect(diff.messagesToIndex == [message("m2", body: "corpo editado")])
        #expect(diff.messageIdsToDelete.isEmpty)
    }

    @Test
    func removedIdsAreDeleted() {
        let before = SpotlightSnapshot(
            disciplines: [discipline("d1"), discipline("d2", title: "Cálculo II")],
            messages: [message("m1")]
        )
        let after = SpotlightSnapshot(disciplines: [discipline("d1")], messages: [])

        let diff = SpotlightDiff.compute(ledger: ledger(for: before), snapshot: after)

        #expect(diff.disciplinesToIndex.isEmpty)
        #expect(diff.disciplineIdsToDelete == ["discipline/sem1/d2"])
        #expect(diff.messageIdsToDelete == ["message/m1"])
    }

    @Test
    func wipeRequiresANonEmptyLedger() {
        let populated = ledger(for: SpotlightSnapshot(disciplines: [discipline("d1")], messages: []))

        #expect(SpotlightDiff.compute(ledger: populated, snapshot: nil).wipeAll)
        // A signed-out fresh install never issues delete-alls.
        #expect(SpotlightDiff.compute(ledger: SpotlightIndexLedger(), snapshot: nil).isEmpty)
    }

    @Test
    func applyingBringsTheLedgerCurrent() {
        let before = SpotlightSnapshot(disciplines: [discipline("d1")], messages: [message("m1")])
        let after = SpotlightSnapshot(
            disciplines: [discipline("d1", title: "Algoritmos I — T02")],
            messages: [message("m2")]
        )

        var ledger = ledger(for: before)
        let diff = SpotlightDiff.compute(ledger: ledger, snapshot: after)
        ledger.applyDisciplines(diff)
        ledger.applyMessages(diff)

        #expect(SpotlightDiff.compute(ledger: ledger, snapshot: after).isEmpty)
    }

    @Test
    func ledgerFromAnotherSchemaVersionSignalsAWipe() throws {
        // Pre-version file (no `version` key) — fails decoding, signals stale.
        let preVersion = Data(#"{"disciplines":{"a":"1"},"messages":{}}"#.utf8)
        #expect(SpotlightIndexLedger.decode(preVersion) == nil)

        let stale = Data(#"{"version":2,"disciplines":{"a":"1"},"messages":{}}"#.utf8)
        #expect(SpotlightIndexLedger.decode(stale) == nil)

        var current = SpotlightIndexLedger()
        current.disciplines["a"] = "1"
        #expect(SpotlightIndexLedger.decode(try JSONEncoder().encode(current)) == current)
    }

    @Test
    func failedWriteHoldsBackOnlyItsOwnKind() async {
        let snapshot = SpotlightSnapshot(disciplines: [discipline("d1")], messages: [message("m1")])
        let diff = SpotlightDiff.compute(ledger: SpotlightIndexLedger(), snapshot: snapshot)

        let next = await SpotlightSupport.apply(
            diff, ledger: SpotlightIndexLedger(), writer: DisciplineRejectingWriter()
        )

        // Messages advanced; the failed disciplines stay pending for retry.
        let retry = SpotlightDiff.compute(ledger: next, snapshot: snapshot)
        #expect(retry.disciplinesToIndex == [discipline("d1")])
        #expect(retry.messagesToIndex.isEmpty)
    }
}

private struct DisciplineRejectingWriter: SpotlightIndexWriter {
    struct Rejected: Error {}

    func index(disciplines: [SpotlightDiscipline], messages: [SpotlightMessage]) async throws {
        guard disciplines.isEmpty else { throw Rejected() }
    }

    func delete(disciplineIds: [String], messageIds: [String]) async throws {
        guard disciplineIds.isEmpty else { throw Rejected() }
    }

    func deleteAll() async throws {}
}

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

    private func evaluation(_ id: String, dateStamp: String = "2026-05-15") -> SpotlightEvaluation {
        SpotlightEvaluation(
            id: "evaluation/sem1/d1/\(id)", semesterId: "sem1", disciplineId: "d1", gradeId: id,
            title: "Prova 1 — Algoritmos I", subtitle: "sex., 15 de mai.",
            dateStamp: dateStamp, keywords: ["ALGI"]
        )
    }

    private func snapshot(
        disciplines: [SpotlightDiscipline] = [],
        messages: [SpotlightMessage] = [],
        evaluations: [SpotlightEvaluation] = []
    ) -> SpotlightSnapshot {
        SpotlightSnapshot(disciplines: disciplines, messages: messages, evaluations: evaluations)
    }

    /// A ledger that has fully applied `snapshot`.
    private func ledger(for snapshot: SpotlightSnapshot) -> SpotlightIndexLedger {
        var ledger = SpotlightIndexLedger()
        let diff = SpotlightDiff.compute(ledger: ledger, snapshot: snapshot)
        ledger.applyDisciplines(diff)
        ledger.applyMessages(diff)
        ledger.applyEvaluations(diff)
        return ledger
    }

    @Test
    func emptyLedgerIndexesEverything() {
        let snapshot = snapshot(
            disciplines: [discipline("d1")], messages: [message("m1")], evaluations: [evaluation("g1")]
        )

        let diff = SpotlightDiff.compute(ledger: SpotlightIndexLedger(), snapshot: snapshot)

        #expect(diff.disciplinesToIndex == [discipline("d1")])
        #expect(diff.messagesToIndex == [message("m1")])
        #expect(diff.evaluationsToIndex == [evaluation("g1")])
        #expect(diff.disciplineIdsToDelete.isEmpty)
        #expect(diff.messageIdsToDelete.isEmpty)
        #expect(diff.evaluationIdsToDelete.isEmpty)
        #expect(!diff.wipeAll)
    }

    @Test
    func identicalSnapshotProducesNoWork() {
        let snapshot = snapshot(
            disciplines: [discipline("d1")], messages: [message("m1")], evaluations: [evaluation("g1")]
        )

        let diff = SpotlightDiff.compute(ledger: ledger(for: snapshot), snapshot: snapshot)

        #expect(diff.isEmpty)
    }

    @Test
    func contentChangeReindexesExactlyThatItem() {
        let before = snapshot(
            disciplines: [discipline("d1")],
            messages: [message("m1"), message("m2")],
            evaluations: [evaluation("g1")]
        )
        let after = snapshot(
            disciplines: [discipline("d1")],
            messages: [message("m1"), message("m2", body: "corpo editado")],
            evaluations: [evaluation("g1", dateStamp: "2026-05-22")]
        )

        let diff = SpotlightDiff.compute(ledger: ledger(for: before), snapshot: after)

        #expect(diff.disciplinesToIndex.isEmpty)
        #expect(diff.messagesToIndex == [message("m2", body: "corpo editado")])
        #expect(diff.evaluationsToIndex == [evaluation("g1", dateStamp: "2026-05-22")])
        #expect(diff.messageIdsToDelete.isEmpty)
        #expect(diff.evaluationIdsToDelete.isEmpty)
    }

    @Test
    func removedIdsAreDeleted() {
        let before = snapshot(
            disciplines: [discipline("d1"), discipline("d2", title: "Cálculo II")],
            messages: [message("m1")],
            evaluations: [evaluation("g1")]
        )
        let after = snapshot(disciplines: [discipline("d1")])

        let diff = SpotlightDiff.compute(ledger: ledger(for: before), snapshot: after)

        #expect(diff.disciplinesToIndex.isEmpty)
        #expect(diff.disciplineIdsToDelete == ["discipline/sem1/d2"])
        #expect(diff.messageIdsToDelete == ["message/m1"])
        // A posted value (or a passed date) drops the evaluation from the
        // projection — the diff removes it from the index.
        #expect(diff.evaluationIdsToDelete == ["evaluation/sem1/d1/g1"])
    }

    @Test
    func wipeRequiresANonEmptyLedger() {
        let populated = ledger(for: snapshot(disciplines: [discipline("d1")]))

        #expect(SpotlightDiff.compute(ledger: populated, snapshot: nil).wipeAll)
        // A signed-out fresh install never issues delete-alls.
        #expect(SpotlightDiff.compute(ledger: SpotlightIndexLedger(), snapshot: nil).isEmpty)
    }

    @Test
    func evaluationsAloneMakeTheLedgerWipeworthy() {
        let populated = ledger(for: snapshot(evaluations: [evaluation("g1")]))

        #expect(SpotlightDiff.compute(ledger: populated, snapshot: nil).wipeAll)
    }

    @Test
    func applyingBringsTheLedgerCurrent() {
        let before = snapshot(disciplines: [discipline("d1")], messages: [message("m1")])
        let after = snapshot(
            disciplines: [discipline("d1", title: "Algoritmos I — T02")],
            messages: [message("m2")],
            evaluations: [evaluation("g1")]
        )

        var ledger = ledger(for: before)
        let diff = SpotlightDiff.compute(ledger: ledger, snapshot: after)
        ledger.applyDisciplines(diff)
        ledger.applyMessages(diff)
        ledger.applyEvaluations(diff)

        #expect(SpotlightDiff.compute(ledger: ledger, snapshot: after).isEmpty)
    }

    @Test
    func failedWriteHoldsBackOnlyItsOwnKind() async {
        let snapshot = snapshot(
            disciplines: [discipline("d1")], messages: [message("m1")], evaluations: [evaluation("g1")]
        )
        let diff = SpotlightDiff.compute(ledger: SpotlightIndexLedger(), snapshot: snapshot)

        let next = await SpotlightSupport.apply(
            diff, ledger: SpotlightIndexLedger(), writer: DisciplineRejectingWriter()
        )

        // Messages and evaluations advanced; the failed disciplines stay
        // pending for retry.
        let retry = SpotlightDiff.compute(ledger: next, snapshot: snapshot)
        #expect(retry.disciplinesToIndex == [discipline("d1")])
        #expect(retry.messagesToIndex.isEmpty)
        #expect(retry.evaluationsToIndex.isEmpty)
    }
}

private struct DisciplineRejectingWriter: SpotlightIndexWriter {
    struct Rejected: Error {}

    func index(
        disciplines: [SpotlightDiscipline],
        messages: [SpotlightMessage],
        evaluations: [SpotlightEvaluation]
    ) async throws {
        guard disciplines.isEmpty else { throw Rejected() }
    }

    func delete(disciplineIds: [String], messageIds: [String], evaluationIds: [String]) async throws {
        guard disciplineIds.isEmpty else { throw Rejected() }
    }

    func deleteAll() async throws {}

    func disciplinesDidChange() async {}
}

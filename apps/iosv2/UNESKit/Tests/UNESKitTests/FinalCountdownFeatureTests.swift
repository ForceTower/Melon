import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

/// The UEFS verdict rules, including the truncation the university applies:
/// averages floor to a tenth, "needed" grades ceil to a tenth.
struct FinalCountdownMathTests {
    private func row(_ id: String, _ score: String = "", weight: Int = 1) -> FCRow {
        FCRow(id: id, label: id, scoreText: score, weight: weight)
    }

    @Test
    func directPassWhenEverythingClearsSeven() {
        let verdict = FinalCountdownMath.verdict(
            for: [row("a", "8,5"), row("b", "7,8"), row("c", "9")],
            weighted: false
        )
        #expect(verdict.kind == .passed)
        #expect(verdict.avg == 8.4)
    }

    @Test
    func truncationKeepsSixNinetyFiveOutOfDirectPass() {
        // Raw mean 6,983… floors to 6,9 — never rounds up past the cutoff.
        let verdict = FinalCountdownMath.verdict(
            for: [row("a", "6,95"), row("b", "7"), row("c", "7")],
            weighted: false
        )
        #expect(verdict.kind == .final)
        #expect(verdict.avg == 6.9)
        #expect(verdict.need == 2.2)
    }

    @Test
    func finalNeedRoundsUpFromTheFormula() {
        let verdict = FinalCountdownMath.verdict(
            for: [row("a", "5,5"), row("b", "4"), row("c", "6,2")],
            weighted: false
        )
        #expect(verdict.kind == .final)
        #expect(verdict.avg == 5.2)
        // (5 − 0,6·5,2) / 0,4 is exactly 4,7 — float noise must not lift it.
        #expect(verdict.need == 4.7)
    }

    @Test
    func failsOutrightBelowTheFloor() {
        let verdict = FinalCountdownMath.verdict(
            for: [row("a", "2"), row("b", "2,5"), row("c", "2,8")],
            weighted: false
        )
        #expect(verdict.kind == .failed)
        #expect(verdict.avg == 2.4)
    }

    @Test
    func borderlineSolvesTheSingleMissingRow() {
        let verdict = FinalCountdownMath.verdict(
            for: [row("a", "6,5"), row("b", "5,2"), row("c")],
            weighted: false
        )
        #expect(verdict.kind == .borderline)
        #expect(verdict.avg == 5.8)
        #expect(verdict.wildcardNeeded == 9.3)
    }

    @Test
    func borderlineFinalWhenEvenTenFallsShort() {
        let verdict = FinalCountdownMath.verdict(
            for: [row("a", "5"), row("b", "4"), row("c")],
            weighted: false
        )
        #expect(verdict.kind == .borderlineFinal)
        #expect(verdict.wildcardNeeded == 12)
    }

    @Test
    func passesEarlyWhenTheWorstCaseAlreadyClears() {
        let verdict = FinalCountdownMath.verdict(
            for: [row("a", "10"), row("b", "10"), row("c", "10"), row("d")],
            weighted: false
        )
        #expect(verdict.kind == .passed)
        #expect(verdict.avg == 7.5)
    }

    @Test
    func onTrackWhenSeveralRowsAreStillOpen() {
        let verdict = FinalCountdownMath.verdict(
            for: [row("a", "8"), row("b"), row("c")],
            weighted: false
        )
        #expect(verdict.kind == .ontrack)
        #expect(verdict.wildcardNeeded == nil)
        #expect(verdict.best == 9.3)
        #expect(verdict.worst == 2.6)
    }

    @Test
    func failingTrackWhenTheBestCaseStaysUnderTheFloor() {
        let verdict = FinalCountdownMath.verdict(
            for: [row("a", "0"), row("b", "0"), row("c", "0"), row("d")],
            weighted: false
        )
        #expect(verdict.kind == .failingTrack)
        #expect(verdict.best == 2.5)
    }

    @Test
    func weightedModeUsesTheWeights() {
        let verdict = FinalCountdownMath.verdict(
            for: [row("a", "8", weight: 2), row("b", "5"), row("c")],
            weighted: true
        )
        #expect(verdict.kind == .borderline)
        #expect(verdict.avg == 7.0)
        // (7·4 − 8·2 − 5·1) / 1
        #expect(verdict.wildcardNeeded == 7.0)
    }

    @Test
    func emptyUntilSomethingIsFilled() {
        let verdict = FinalCountdownMath.verdict(for: [row("a"), row("b")], weighted: false)
        #expect(verdict.kind == .empty)
        #expect(verdict.avg == nil)
    }

    @Test
    func scoreTextSanitizing() {
        #expect(FCRow.sanitizeScoreText("8.5") == "8,5")
        #expect(FCRow.sanitizeScoreText("7,5,5") == "7,55")
        #expect(FCRow.sanitizeScoreText("abc") == "")
        #expect(FCRow.sanitizeScoreText("15") == "10")
        #expect(FCRow.parseScore("9,75") == 9.75)
        #expect(FCRow.parseScore("") == nil)
    }
}

@MainActor
struct FinalCountdownFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)

    @Test
    func meEntryStartsInModoLivre() async {
        let overview = DisciplinesOverview.preview(now: Self.referenceDate)

        let store = TestStore(initialState: FinalCountdownFeature.State()) {
            FinalCountdownFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.cached = { _ in overview }
        }

        #expect(store.state.discipline == nil)
        #expect(store.state.rows == FinalCountdownFeature.State.fallbackRows)

        await store.send(.task)
        // The overview only feeds the picker — modo livre stays put.
        await store.receive(.overviewLoaded(overview)) {
            $0.choices = overview.current!.disciplines
            $0.currentSemesterCode = "20261"
        }
    }

    @Test
    func detailEntryKeepsItsSeedWhenTheOverviewLands() async {
        let overview = DisciplinesOverview.preview(now: Self.referenceDate)
        let detail = DisciplineDetail.preview(now: Self.referenceDate)

        let store = TestStore(
            initialState: FinalCountdownFeature.State(
                detail: detail,
                selectedGroup: nil,
                semesterCode: "20261"
            )
        ) {
            FinalCountdownFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.cached = { _ in overview }
        }

        #expect(store.state.rows == [
            FCRow(id: "g1", label: "AV1", scoreText: "8,3"),
            FCRow(id: "g2", label: "AV2"),
            FCRow(id: "g3", label: "AV3"),
        ])

        await store.send(.task)
        // The overview only feeds the picker — the seed survives.
        await store.receive(.overviewLoaded(overview)) {
            $0.choices = overview.current!.disciplines
            $0.currentSemesterCode = "20261"
        }
    }

    @Test
    func scoreEditingSanitizesTheField() async {
        var seeded = FinalCountdownFeature.State()
        seeded.rows = FinalCountdownFeature.State.fallbackRows

        let store = TestStore(initialState: seeded) {
            FinalCountdownFeature()
        }

        await store.send(.scoreEdited(id: "fallback-1", text: "8.5")) {
            $0.rows[id: "fallback-1"]?.scoreText = "8,5"
        }
        await store.send(.scoreEdited(id: "fallback-1", text: "15")) {
            $0.rows[id: "fallback-1"]?.scoreText = "10"
        }
        await store.send(.labelEdited(id: "fallback-1", text: "Trabalho Final")) {
            $0.rows[id: "fallback-1"]?.label = "Trabal"
        }
    }

    @Test
    func weightsClampBetweenOneAndNine() async {
        var seeded = FinalCountdownFeature.State()
        seeded.rows = FinalCountdownFeature.State.fallbackRows

        let store = TestStore(initialState: seeded) {
            FinalCountdownFeature()
        }

        // Already at the floor — stepping down changes nothing.
        await store.send(.weightStepped(id: "fallback-1", delta: -1))
        await store.send(.weightStepped(id: "fallback-1", delta: 1)) {
            $0.rows[id: "fallback-1"]?.weight = 2
        }
        await store.send(.weightStepped(id: "fallback-1", delta: 99)) {
            $0.rows[id: "fallback-1"]?.weight = 9
        }
    }

    @Test
    func rowsCanBeAddedRemovedAndCleared() async {
        var seeded = FinalCountdownFeature.State()
        seeded.rows = [
            FCRow(id: "a", label: "AV1", scoreText: "8"),
            FCRow(id: "b", label: "AV2", scoreText: "6,5"),
        ]

        let store = TestStore(initialState: seeded) {
            FinalCountdownFeature()
        } withDependencies: {
            $0.uuid = .incrementing
        }

        await store.send(.addRowTapped) {
            $0.rows.append(FCRow(id: UUID(0).uuidString, label: "AV3"))
        }
        await store.send(.rowRemoved(id: UUID(0).uuidString)) {
            $0.rows.remove(id: UUID(0).uuidString)
        }
        await store.send(.clearTapped) {
            $0.rows[id: "a"]?.scoreText = ""
            $0.rows[id: "b"]?.scoreText = ""
        }
        await store.send(.rowRemoved(id: "a")) {
            $0.rows.remove(id: "a")
        }
        // The last row can't be removed.
        await store.send(.rowRemoved(id: "b"))
    }

    @Test
    func pickingAnotherDisciplineReseedsTheRows() async {
        let overview = DisciplinesOverview.preview(now: Self.referenceDate)
        var seeded = FinalCountdownFeature.State()
        seeded.choices = overview.current!.disciplines
        seeded.currentSemesterCode = "20261"

        let store = TestStore(initialState: seeded) {
            FinalCountdownFeature()
        }

        await store.send(.changeTapped) {
            $0.isPickerPresented = true
        }
        await store.send(.disciplinePicked("d5")) {
            $0.isPickerPresented = false
            $0.discipline = FCDiscipline(
                id: "d5",
                name: "Estatística",
                teacherName: "Laís Pinheiro",
                colorIndex: 4,
                semesterCode: "20261"
            )
            $0.rows = [
                FCRow(id: "g12", label: "AV1", scoreText: "9,1"),
                FCRow(id: "g13", label: "AV2"),
            ]
        }
    }

    @Test
    func modoLivreDetachesTheDisciplineAndStartsBlank() async {
        let overview = DisciplinesOverview.preview(now: Self.referenceDate)
        var seeded = FinalCountdownFeature.State()
        seeded.choices = overview.current!.disciplines
        seeded.currentSemesterCode = "20261"
        seeded.rows = [FCRow(id: "g12", label: "AV1", scoreText: "9,1")]
        seeded.discipline = FCDiscipline(
            id: "d5",
            name: "Estatística",
            teacherName: "Laís Pinheiro",
            colorIndex: 4,
            semesterCode: "20261"
        )

        let store = TestStore(initialState: seeded) {
            FinalCountdownFeature()
        }

        await store.send(.changeTapped) {
            $0.isPickerPresented = true
        }
        await store.send(.disciplinePicked(nil)) {
            $0.isPickerPresented = false
            $0.discipline = nil
            $0.rows = FinalCountdownFeature.State.fallbackRows
        }
    }
}

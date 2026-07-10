import Foundation
import Testing

@testable import UNESKit

/// The Prova Final verdict mapping — the same rules the detail screen and
/// FinalCountdown render, so Siri can never disagree with the app. Floor
/// rule for averages, ceil rule for needed grades, both pinned.
struct FinalExamVerdictTests {
    private func grade(
        _ id: String,
        title: String,
        value: Double? = nil,
        weight: Double? = nil,
        date: String? = nil,
        daysUntil: Int? = nil
    ) -> DisciplineDetailGrade {
        DisciplineDetailGrade(
            id: id, label: title, title: title, value: value, weight: weight,
            date: date, daysUntil: daysUntil
        )
    }

    private func detail(
        grades: [DisciplineDetailGrade],
        finalGrade: Double? = nil,
        approved: Bool? = nil,
        wentToFinals: Bool = false
    ) -> DisciplineDetail {
        DisciplineDetail(
            id: "d1", semesterId: "sem1", code: "MAT202", name: "Cálculo II",
            department: nil, ementa: nil, teacherName: nil, hours: 60, missedHours: 0,
            sections: [DisciplineGradeSection(id: "c1", grades: grades)],
            finalGrade: finalGrade, approved: approved, wentToFinals: wentToFinals
        )
    }

    @Test
    func missingDetailIsAStaleEntity() {
        #expect(IntentSupport.finalExamVerdict(detail: nil) == .stale)
    }

    @Test
    func approvedSpeaksTheResultMean() {
        let detail = detail(
            grades: [grade("g1", title: "Prova 1", value: 9.0)], finalGrade: 8.6, approved: true
        )
        #expect(IntentSupport.finalExamVerdict(detail: detail) == .approved(average: 8.6))
    }

    @Test
    func upstreamFailIsLost() {
        let detail = detail(
            grades: [grade("g1", title: "Prova 1", value: 4.0)], finalGrade: 4.0, approved: false
        )
        #expect(IntentSupport.finalExamVerdict(detail: detail) == .lost(average: 4.0))
    }

    @Test
    func finalsNeedRoundsUpAfterTheMeanTruncates() {
        // Raw mean 6,95 truncates to 6,9 (never rounds to 7,0); the need
        // (5 − 0,6·6,9)/0,4 = 2,15 rounds up to 2,2 — scoring exactly the
        // displayed value still clears the cutoff.
        let detail = detail(
            grades: [grade("g1", title: "Prova 1", value: 6.95)], wentToFinals: true
        )
        #expect(IntentSupport.finalExamVerdict(detail: detail) == .finals(average: 6.95, needed: 2.2))
    }

    @Test
    func unreachableFinalsNeedIsLost() {
        // Mean 1,0 needs (5 − 0,6)/0,4 = 11 on the Finals — above 10.
        let detail = detail(
            grades: [grade("g1", title: "Prova 1", value: 1.0)], wentToFinals: true
        )
        #expect(IntentSupport.finalExamVerdict(detail: detail) == .lost(average: 1.0))
    }

    @Test
    func ongoingWithReachablePendingClosesDirect() {
        let detail = detail(grades: [
            grade("g1", title: "Prova 1", value: 8.0),
            grade("g2", title: "Prova 3", date: "2026-09-01", daysUntil: 40),
            grade("g3", title: "Prova 2", date: "2026-08-15", daysUntil: 23),
        ])
        // (7·3 − 8) / 2 = 6,5 needed on each of the two pending rows; the
        // soonest scheduled evaluation rides along for the dialog.
        #expect(IntentSupport.finalExamVerdict(detail: detail) == .directClose(
            average: 8.0,
            needed: 6.5,
            next: FinalExamVerdict.NextEvaluation(title: "Prova 2", dateStamp: "2026-08-15")
        ))
    }

    @Test
    func unscheduledPendingRowsCarryNoNextEvaluation() {
        let detail = detail(grades: [
            grade("g1", title: "Prova 1", value: 8.0),
            grade("g2", title: "Prova 2"),
        ])
        #expect(IntentSupport.finalExamVerdict(detail: detail) == .directClose(
            average: 8.0, needed: 6.0, next: nil
        ))
    }

    @Test
    func unreachableDirectCloseGoesThroughTheFinals() {
        // Low released mean (status lowGrade): needs 7·2 − 3 = 11 on the
        // one pending row — direct close is off the table.
        let detail = detail(grades: [
            grade("g1", title: "Prova 1", value: 3.0),
            grade("g2", title: "Prova 2", date: "2026-08-15", daysUntil: 23),
        ])
        #expect(IntentSupport.finalExamVerdict(detail: detail) == .finalsPath(average: 3.0))
    }

    @Test
    func nothingPendingReportsThePartialMean() {
        let detail = detail(grades: [
            grade("g1", title: "Prova 1", value: 7.0),
            grade("g2", title: "Prova 2", value: 8.0),
        ])
        #expect(IntentSupport.finalExamVerdict(detail: detail) == .partial(average: 7.5))
    }

    @Test
    func noReleasedGradesIsNoGrades() {
        #expect(IntentSupport.finalExamVerdict(detail: detail(grades: [])) == .noGrades)
        // All rows pending: nothing released yet either.
        let pendingOnly = detail(grades: [grade("g1", title: "Prova 1", date: "2026-08-15", daysUntil: 23)])
        #expect(IntentSupport.finalExamVerdict(detail: pendingOnly) == .noGrades)
    }
}

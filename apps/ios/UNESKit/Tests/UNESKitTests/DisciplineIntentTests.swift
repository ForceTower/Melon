import Foundation
import Testing

@testable import UNESKit

/// The discipline-scoped answers' pure cores: the next-class
/// classification over a filtered occurrence set, and the grades summary
/// over a detail — the spoken pieces Siri must never get wrong.
struct DisciplineIntentTests {
    private let calendar = Calendar(identifier: .gregorian)
    private let now = Date(timeIntervalSince1970: 1_757_412_000) // 2025-09-09 10:00 UTC-ish

    private func occurrence(startingIn minutes: Int, lasting: Int = 100, room: String? = "MT-14") -> ClassOccurrence {
        let start = now.addingTimeInterval(TimeInterval(minutes * 60))
        return ClassOccurrence(
            classId: "c1", code: "EXA805", title: "Algoritmos", room: room, teacherName: "Camila",
            topic: nil, colorIndex: 0, disciplineId: "d1", start: start,
            end: start.addingTimeInterval(TimeInterval(lasting * 60)), startTime: "10:00", endTime: "11:40"
        )
    }

    private func verdict(_ occurrences: [ClassOccurrence]) -> DisciplineClassVerdict {
        IntentSupport.disciplineClassVerdict(now: now, occurrences: occurrences, calendar: calendar)
    }

    @Test
    func runningClassIsInClass() {
        let current = occurrence(startingIn: -10)
        #expect(verdict([current]) == .inClass(current))
    }

    @Test
    func laterTodayIsToday() {
        let later = occurrence(startingIn: 180)
        #expect(verdict([later]) == .today(later))
    }

    @Test
    func nextDayIsTomorrow() {
        let tomorrow = occurrence(startingIn: 24 * 60)
        #expect(verdict([tomorrow]) == .tomorrow(tomorrow))
    }

    @Test
    func fartherOutIsAWeekday() {
        let later = occurrence(startingIn: 3 * 24 * 60)
        #expect(verdict([later]) == .weekday(later))
    }

    @Test
    func finishedClassesWithNothingAheadIsNone() {
        #expect(verdict([occurrence(startingIn: -300)]) == .none)
        #expect(verdict([]) == .none)
    }

    @Test
    func finishedTodayFallsThroughToTheNextOccurrence() {
        let done = occurrence(startingIn: -300)
        let next = occurrence(startingIn: 2 * 24 * 60)
        #expect(verdict([done, next]) == .weekday(next))
    }

    // MARK: Grades

    private func grade(_ id: String, title: String, value: Double? = nil, weight: Double? = nil) -> DisciplineDetailGrade {
        DisciplineDetailGrade(id: id, label: title, title: title, value: value, weight: weight, date: nil, daysUntil: nil)
    }

    private func detail(
        grades: [DisciplineDetailGrade],
        finalExam: DisciplineDetailGrade? = nil,
        finalGrade: Double? = nil,
        approved: Bool? = nil,
        wentToFinals: Bool = false
    ) -> DisciplineDetail {
        DisciplineDetail(
            id: "d1", semesterId: "sem1", code: "MAT202", name: "Cálculo II",
            department: nil, ementa: nil, teacherName: nil, hours: 60, missedHours: 0,
            sections: [DisciplineGradeSection(id: "c1", grades: grades)],
            finalExam: finalExam, finalGrade: finalGrade, approved: approved, wentToFinals: wentToFinals
        )
    }

    @Test
    func nothingReleasedMeansNoAverage() {
        let summary = IntentSupport.gradesSummary(detail: detail(grades: [grade("g1", title: "Prova 1")]))
        #expect(summary.released.isEmpty)
        #expect(summary.average == nil)
        #expect(!summary.closed)
    }

    @Test
    func releasedGradesSpeakInOrderWithThePartialMean() {
        let summary = IntentSupport.gradesSummary(detail: detail(grades: [
            grade("g1", title: "Prova 1", value: 8),
            grade("g2", title: "Prova 2", value: 6),
            grade("g3", title: "Prova 3"),
        ]))
        #expect(summary.released.map(\.title) == ["Prova 1", "Prova 2"])
        #expect(summary.average == 7)
        #expect(!summary.closed)
    }

    @Test
    func closedDisciplineLeadsWithThePostedMean() {
        let summary = IntentSupport.gradesSummary(detail: detail(
            grades: [grade("g1", title: "Prova 1", value: 9)], finalGrade: 8.6, approved: true
        ))
        #expect(summary.closed)
        #expect(summary.average == 8.6)
    }

    @Test
    func liveFinalsMeanIsNotAClosedVerdict() {
        let summary = IntentSupport.gradesSummary(detail: detail(
            grades: [grade("g1", title: "Prova 1", value: 4)], finalGrade: 5.2, approved: nil, wentToFinals: true
        ))
        #expect(!summary.closed)
        #expect(summary.average == 4)
    }

    @Test
    func provaFinalRowJoinsOnceItHasAValue() {
        let pending = IntentSupport.gradesSummary(detail: detail(
            grades: [grade("g1", title: "Prova 1", value: 4)], finalExam: grade("f", title: "Prova Final")
        ))
        #expect(pending.released.map(\.title) == ["Prova 1"])

        let graded = IntentSupport.gradesSummary(detail: detail(
            grades: [grade("g1", title: "Prova 1", value: 4)], finalExam: grade("f", title: "Prova Final", value: 7)
        ))
        #expect(graded.released.map(\.title) == ["Prova 1", "Prova Final"])
    }
}

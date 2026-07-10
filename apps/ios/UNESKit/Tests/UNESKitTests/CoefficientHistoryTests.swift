import Foundation
import Testing

@testable import UNESKit

/// The cross-semester coefficient: hours-weighted mean of every discipline
/// taken, one cumulative spark point per semester.
struct CoefficientHistoryTests {
    private func semester(_ id: String, code: String, start: String, end: String) -> SemesterRecord {
        SemesterRecord(id: id, code: code, description: "Semestre \(code)", startDate: start, endDate: end)
    }

    @Test
    func weighsEveryClosedDisciplineByItsHours() throws {
        let history = CoefficientHistory(
            // Out of order on purpose — the walk sorts by start date.
            semesters: [
                semester("sem3", code: "20261", start: "2026-02-24", end: "2026-07-07"),
                semester("sem1", code: "20251", start: "2025-02-01", end: "2025-06-30"),
                semester("sem2", code: "20252", start: "2025-08-01", end: "2025-12-15"),
            ],
            disciplines: [
                // Two groups (theory + practice) of one 60h discipline.
                DisciplineRecord(id: "d1", semesterId: "sem1", code: "ALGI", name: "Algoritmos I", hours: 60),
                // No catalog hours — the offer's 30 must win.
                DisciplineRecord(id: "d2", semesterId: "sem1", code: "CALC", name: "Cálculo II"),
                // Retake: same discipline id under another semester.
                DisciplineRecord(id: "d2", semesterId: "sem2", code: "CALC", name: "Cálculo II", hours: 30),
                DisciplineRecord(id: "d3", semesterId: "sem3", code: "LPOO", name: "POO", hours: 30),
                DisciplineRecord(id: "d4", semesterId: "sem3", code: "FIS2", name: "Física II", hours: 60),
            ],
            disciplineOffers: [
                DisciplineOfferRecord(id: "o1", semesterId: "sem1", disciplineId: "d1"),
                DisciplineOfferRecord(id: "o2", semesterId: "sem1", disciplineId: "d2", hours: 30),
                DisciplineOfferRecord(id: "o3", semesterId: "sem2", disciplineId: "d2"),
                DisciplineOfferRecord(id: "o4", semesterId: "sem3", disciplineId: "d3"),
                DisciplineOfferRecord(id: "o5", semesterId: "sem3", disciplineId: "d4"),
            ],
            classes: [
                ClassRecord(id: "c1t", semesterId: "sem1", offerId: "o1", hours: 40, groupName: "T01"),
                ClassRecord(id: "c1p", semesterId: "sem1", offerId: "o1", hours: 20, groupName: "T01P01"),
                ClassRecord(id: "c2", semesterId: "sem1", offerId: "o2", hours: 30),
                ClassRecord(id: "c3", semesterId: "sem2", offerId: "o3", hours: 30),
                ClassRecord(id: "c4", semesterId: "sem3", offerId: "o4", hours: 30),
                ClassRecord(id: "c5", semesterId: "sem3", offerId: "o5", hours: 60),
            ],
            studentClasses: [
                // The practice row replicates the discipline's result.
                StudentClassRecord(id: "sc1t", semesterId: "sem1", classId: "c1t", finalGrade: "8.0"),
                StudentClassRecord(id: "sc1p", semesterId: "sem1", classId: "c1p", finalGrade: "8.0"),
                StudentClassRecord(id: "sc2", semesterId: "sem1", classId: "c2", finalGrade: "5.0"),
                StudentClassRecord(id: "sc3", semesterId: "sem2", classId: "c3", finalGrade: "10.0"),
                // sem3 is open: one class closed early, one still running.
                StudentClassRecord(id: "sc4", semesterId: "sem3", classId: "c4", finalGrade: "9.0"),
                StudentClassRecord(id: "sc5", semesterId: "sem3", classId: "c5"),
            ]
        )

        let summary = try #require(history.summary())
        // sem1: (8×60 + 5×30) / 90 = 7.0
        // sem2: retaken CALC counts again — (630 + 10×30) / 120 = 7.75
        // sem3: closed POO joins, running FIS2 stays out — (930 + 9×30) / 150 = 8.0
        #expect(summary.spark == [7.0, 7.75, 8.0])
        #expect(summary.value == 8.0)
        #expect(summary.delta == 0.25)
    }

    @Test
    func missingResultScoresZeroOnceTheStudentMovesOn() throws {
        let closed = StudentClassRecord(id: "sc1", semesterId: "sem1", classId: "c1", finalGrade: "8.0")
        let abandoned = StudentClassRecord(id: "sc2", semesterId: "sem1", classId: "c2")
        let later = StudentClassRecord(id: "sc3", semesterId: "sem2", classId: "c3", finalGrade: "9.0")
        let history = CoefficientHistory(
            semesters: [
                semester("sem1", code: "20251", start: "2025-02-01", end: "2025-06-30"),
                semester("sem2", code: "20252", start: "2025-08-01", end: "2025-12-15"),
            ],
            disciplines: [
                DisciplineRecord(id: "d1", semesterId: "sem1", code: "ALGI", name: "Algoritmos I", hours: 60),
                DisciplineRecord(id: "d2", semesterId: "sem1", code: "CALC", name: "Cálculo II", hours: 20),
                DisciplineRecord(id: "d3", semesterId: "sem2", code: "LPOO", name: "POO", hours: 40),
            ],
            disciplineOffers: [
                DisciplineOfferRecord(id: "o1", semesterId: "sem1", disciplineId: "d1"),
                DisciplineOfferRecord(id: "o2", semesterId: "sem1", disciplineId: "d2"),
                DisciplineOfferRecord(id: "o3", semesterId: "sem2", disciplineId: "d3"),
            ],
            classes: [
                ClassRecord(id: "c1", semesterId: "sem1", offerId: "o1", hours: 60),
                ClassRecord(id: "c2", semesterId: "sem1", offerId: "o2", hours: 20),
                ClassRecord(id: "c3", semesterId: "sem2", offerId: "o3", hours: 40),
            ],
            studentClasses: [closed, abandoned, later]
        )

        let summary = try #require(history.summary())
        // sem1: the abandonment drags with its full 20h — (8×60 + 0×20) / 80 = 6.0
        // sem2: (480 + 9×40) / 120 = 7.0
        #expect(summary.spark == [6.0, 7.0])
        #expect(summary.value == 7.0)
        #expect(summary.delta == 1.0)
    }

    @Test
    func openSemesterLeavesRunningClassesOut() throws {
        let history = CoefficientHistory(
            semesters: [semester("sem1", code: "20261", start: "2026-02-24", end: "2026-07-07")],
            disciplines: [
                DisciplineRecord(id: "d1", semesterId: "sem1", code: "ALGI", name: "Algoritmos I", hours: 60),
                DisciplineRecord(id: "d2", semesterId: "sem1", code: "CALC", name: "Cálculo II", hours: 60),
            ],
            disciplineOffers: [
                DisciplineOfferRecord(id: "o1", semesterId: "sem1", disciplineId: "d1"),
                DisciplineOfferRecord(id: "o2", semesterId: "sem1", disciplineId: "d2"),
            ],
            classes: [
                ClassRecord(id: "c1", semesterId: "sem1", offerId: "o1", hours: 60),
                ClassRecord(id: "c2", semesterId: "sem1", offerId: "o2", hours: 60),
            ],
            studentClasses: [
                StudentClassRecord(id: "sc1", semesterId: "sem1", classId: "c1", finalGrade: "8.5"),
                // Still running — excluded, never a zero.
                StudentClassRecord(id: "sc2", semesterId: "sem1", classId: "c2"),
            ]
        )

        let summary = try #require(history.summary())
        #expect(summary.value == 8.5)
        #expect(summary.spark == [8.5])
        #expect(summary.delta == nil)
    }

    @Test
    func noClosedResultMeansNoSummary() {
        let history = CoefficientHistory(
            semesters: [semester("sem1", code: "20261", start: "2026-02-24", end: "2026-07-07")],
            disciplines: [DisciplineRecord(id: "d1", semesterId: "sem1", code: "ALGI", name: "Algoritmos I", hours: 60)],
            disciplineOffers: [DisciplineOfferRecord(id: "o1", semesterId: "sem1", disciplineId: "d1")],
            classes: [ClassRecord(id: "c1", semesterId: "sem1", offerId: "o1", hours: 60)],
            studentClasses: [StudentClassRecord(id: "sc1", semesterId: "sem1", classId: "c1")]
        )

        #expect(history.summary() == nil)
    }
}

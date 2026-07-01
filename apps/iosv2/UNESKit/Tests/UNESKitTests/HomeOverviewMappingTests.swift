import Foundation
import Testing

@testable import UNESKit

/// Mapping of the semester snapshot into the Home overview.
///
/// Fixture week: April 16, 2026 is a Thursday (upstream day 4).
/// - ALGI (c1): Thursday 08:00–10:00, Friday 08:00–10:00, 60h
/// - CALC (c2): Thursday 10:20–11:10 + 11:10–12:00 (split slots), 60h
struct HomeOverviewMappingTests {
    let calendar = Calendar.current

    private func date(day: Int, hour: Int, minute: Int) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: 4, day: day, hour: hour, minute: minute))!
    }

    private func snapshot(
        studentGrades: [StudentGradeRecord] = [],
        lectures: [LectureRecord] = [],
        missedByClass: [String: Int] = [:]
    ) -> SemesterSnapshot {
        SemesterSnapshot(
            semester: SemesterRecord(
                id: "sem1", code: "20261", description: "Semestre 2026.1",
                startDate: "2026-01-01", endDate: "2026-12-31"
            ),
            disciplines: [
                DisciplineRecord(id: "d1", semesterId: "sem1", code: "ALGI", name: "Algoritmos I"),
                DisciplineRecord(id: "d2", semesterId: "sem1", code: "CALC", name: "Cálculo II"),
            ],
            disciplineOffers: [
                DisciplineOfferRecord(id: "o1", semesterId: "sem1", disciplineId: "d1"),
                DisciplineOfferRecord(id: "o2", semesterId: "sem1", disciplineId: "d2"),
            ],
            classes: [
                ClassRecord(id: "c1", semesterId: "sem1", offerId: "o1", hours: 60),
                ClassRecord(id: "c2", semesterId: "sem1", offerId: "o2", hours: 60),
            ],
            teachers: [TeacherRecord(id: "t1", semesterId: "sem1", name: "Adriana Matos Ferreira")],
            classTeachers: [ClassTeacherRecord(semesterId: "sem1", classId: "c2", teacherId: "t1")],
            spaces: [SpaceRecord(id: "s1", semesterId: "sem1", location: "MT-14")],
            allocations: [
                AllocationRecord(
                    id: "a1", semesterId: "sem1", classId: "c1", spaceId: nil,
                    day: 4, startTime: "08:00:00", endTime: "10:00:00"
                ),
                AllocationRecord(
                    id: "a2", semesterId: "sem1", classId: "c1", spaceId: nil,
                    day: 5, startTime: "08:00:00", endTime: "10:00:00"
                ),
                AllocationRecord(
                    id: "a3", semesterId: "sem1", classId: "c2", spaceId: "s1",
                    day: 4, startTime: "10:20:00", endTime: "11:10:00"
                ),
                AllocationRecord(
                    id: "a4", semesterId: "sem1", classId: "c2", spaceId: "s1",
                    day: 4, startTime: "11:10:00", endTime: "12:00:00"
                ),
            ],
            studentClasses: [
                StudentClassRecord(id: "sc1", semesterId: "sem1", classId: "c1", missedClasses: missedByClass["c1"]),
                StudentClassRecord(id: "sc2", semesterId: "sem1", classId: "c2", missedClasses: missedByClass["c2"]),
            ],
            studentGrades: studentGrades,
            lectures: lectures
        )
    }

    @Test
    func mergesSplitSlotsIntoOneSession() {
        let overview = snapshot().homeOverview(now: date(day: 16, hour: 9, minute: 41), calendar: calendar)

        #expect(overview.today.count == 2)
        #expect(overview.today[0].code == "ALGI")
        #expect(overview.today[0].startMinute == 8 * 60)
        #expect(overview.today[1].code == "CALC")
        #expect(overview.today[1].startMinute == 10 * 60 + 20)
        #expect(overview.today[1].endMinute == 12 * 60)
        #expect(overview.today[1].room == "MT-14")
    }

    @Test
    func heroIsTheNextOccurrenceWithMergedEnd() {
        let now = date(day: 16, hour: 9, minute: 41)
        let lectures: [LectureRecord] = [
            LectureRecord(id: "l1", semesterId: "sem1", classId: "c2", date: "2026-04-16", subject: "Integrais por partes"),
        ]
        let hero = snapshot(lectures: lectures).homeOverview(now: now, calendar: calendar).hero

        #expect(hero?.disciplineName == "Cálculo II")
        #expect(hero?.startsAt == date(day: 16, hour: 10, minute: 20))
        #expect(hero?.endsAt == date(day: 16, hour: 12, minute: 0))
        #expect(hero?.endTime == "12:00")
        #expect(hero?.topic == "Integrais por partes")
        #expect(hero?.room == "MT-14")
        #expect(hero?.teacherName == "Adriana Matos Ferreira")
    }

    @Test
    func heroCrossesMidnightIntoTheNextDay() {
        let hero = snapshot().homeOverview(now: date(day: 16, hour: 23, minute: 0), calendar: calendar).hero

        #expect(hero?.disciplineName == "Algoritmos I")
        #expect(hero?.startsAt == date(day: 17, hour: 8, minute: 0))
    }

    @Test
    func attendanceCountsHoursAndGatesOnHeldLectures() {
        let now = date(day: 16, hour: 9, minute: 41)
        let ungated = snapshot(missedByClass: ["c1": 3, "c2": 2])
            .homeOverview(now: now, calendar: calendar)
        #expect(ungated.attendance == nil)

        let attendance = snapshot(
            lectures: [LectureRecord(id: "l1", semesterId: "sem1", classId: "c1", date: "2026-04-10", subject: nil)],
            missedByClass: ["c1": 3, "c2": 2]
        )
        .homeOverview(now: now, calendar: calendar)
        .attendance

        // 5h missed of 120h → 95.8% rounds to 96; 25% of 120h = 30h cap.
        #expect(attendance?.percent == 96)
        #expect(attendance?.remainingAbsences == 25)
    }

    @Test
    func coefficientAveragesPostedGradesInOrder() {
        let grades: [StudentGradeRecord] = [
            StudentGradeRecord(
                id: "g1", semesterId: "sem1", studentClassId: "sc1", name: "Prova 2",
                nameShort: "P2", ordinal: 2, value: "9.0", date: "2026-04-01"
            ),
            StudentGradeRecord(
                id: "g2", semesterId: "sem1", studentClassId: "sc1", name: "Prova 1",
                nameShort: "P1", ordinal: 1, value: "7.0", date: "2026-03-01"
            ),
            StudentGradeRecord(
                id: "g3", semesterId: "sem1", studentClassId: "sc2", name: "Prova 3",
                nameShort: "P3", ordinal: 3, value: nil, date: nil
            ),
        ]
        let coefficient = snapshot(studentGrades: grades)
            .homeOverview(now: date(day: 16, hour: 9, minute: 41), calendar: calendar)
            .coefficient

        #expect(coefficient?.value == 8.0)
        #expect(coefficient?.spark == [7.0, 9.0])
        #expect(coefficient?.delta == 2.0)
    }

    @Test
    func nextExamIsTheEarliestUngradedFutureEvaluation() {
        let grades: [StudentGradeRecord] = [
            StudentGradeRecord(
                id: "g1", semesterId: "sem1", studentClassId: "sc1", name: "Prova 1",
                nameShort: "P1", ordinal: 1, value: "7.0", date: "2026-03-01"
            ),
            StudentGradeRecord(
                id: "g2", semesterId: "sem1", studentClassId: "sc2", name: "Prova 2",
                nameShort: "P2", ordinal: 2, value: nil, date: "2026-04-23"
            ),
            StudentGradeRecord(
                id: "g3", semesterId: "sem1", studentClassId: "sc1", name: "Prova 2",
                nameShort: nil, ordinal: 2, value: nil, date: "2026-04-30"
            ),
        ]
        let exam = snapshot(studentGrades: grades)
            .homeOverview(now: date(day: 16, hour: 9, minute: 41), calendar: calendar)
            .nextExam

        #expect(exam?.label == "P2")
        #expect(exam?.disciplineName == "Cálculo II")
        #expect(exam?.daysUntil == 7)
        // April 23, 2026 is a Thursday — CALC meets at 10:20.
        #expect(exam?.time == "10:20")
    }

    @Test
    func gradesTruncateAndNeverRoundUp() {
        #expect(formatGrade(6.95) == "6,9")
        #expect(formatGrade(8.5) == "8,5")
        #expect(formatGrade(nil) == "—")
    }
}

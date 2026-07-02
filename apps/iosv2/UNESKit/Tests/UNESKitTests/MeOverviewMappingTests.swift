import Foundation
import Testing

@testable import UNESKit

/// Mapping of the semester snapshot into the Me overview.
///
/// Fixture semester: 2026-02-24 (Tue) through 2026-07-07 (Tue), 134 days.
/// Fixture week: April 16, 2026 is a Thursday (upstream day 4).
/// - ALGI (c1): Thursday 08:00–10:00 in s2, Friday 08:00–10:00
/// - CALC (c2): Thursday 10:20–11:10 + 11:10–12:00 (split slots) in s1
struct MeOverviewMappingTests {
    let calendar = Calendar.current

    private func date(month: Int, day: Int, hour: Int, minute: Int) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: month, day: day, hour: hour, minute: minute))!
    }

    private func snapshot(studentGrades: [StudentGradeRecord] = []) -> SemesterSnapshot {
        SemesterSnapshot(
            semester: SemesterRecord(
                id: "sem1", code: "20261", description: "Semestre 2026.1",
                startDate: "2026-02-24", endDate: "2026-07-07"
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
            spaces: [
                SpaceRecord(id: "s1", semesterId: "sem1", location: "MT-14", campus: "UEFS", modulo: "Módulo 5"),
                SpaceRecord(id: "s2", semesterId: "sem1", location: "LB-02", campus: "UEFS", modulo: "Módulo 1"),
            ],
            allocations: [
                AllocationRecord(
                    id: "a1", semesterId: "sem1", classId: "c1", spaceId: "s2",
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
                StudentClassRecord(id: "sc1", semesterId: "sem1", classId: "c1"),
                StudentClassRecord(id: "sc2", semesterId: "sem1", classId: "c2"),
            ],
            studentGrades: studentGrades
        )
    }

    @Test
    func progressCountsWeeksBetweenTheSemesterDates() throws {
        let overview = snapshot().meOverview(now: date(month: 4, day: 16, hour: 9, minute: 41), calendar: calendar)

        let progress = try #require(overview.progress)
        // 51 elapsed days of 134 → week 8 of 20, 38%.
        #expect(progress.week == 8)
        #expect(progress.totalWeeks == 20)
        #expect(progress.percent == 38)
        #expect(progress.startStamp == "2026-02-24")
        #expect(progress.endStamp == "2026-07-07")
    }

    @Test
    func progressClampsOutsideTheSemester() {
        let before = snapshot().meOverview(now: date(month: 1, day: 10, hour: 9, minute: 0), calendar: calendar)
        #expect(before.progress?.week == 1)
        #expect(before.progress?.percent == 0)

        let after = snapshot().meOverview(now: date(month: 9, day: 1, hour: 9, minute: 0), calendar: calendar)
        #expect(after.progress?.week == 20)
        #expect(after.progress?.percent == 100)
    }

    @Test
    func countdownCountsTheSemesterRemainder() throws {
        let grades = [
            StudentGradeRecord(
                id: "g1", semesterId: "sem1", studentClassId: "sc1",
                nameShort: "P1", ordinal: 1, value: "8.3", date: "2026-03-19"
            ),
            StudentGradeRecord(
                id: "g2", semesterId: "sem1", studentClassId: "sc1",
                nameShort: "P2", ordinal: 2, value: nil, date: "2026-04-22"
            ),
            // Past but ungraded — no longer "scheduled".
            StudentGradeRecord(
                id: "g3", semesterId: "sem1", studentClassId: "sc2",
                nameShort: "P1", ordinal: 1, value: nil, date: "2026-03-01"
            ),
            StudentGradeRecord(
                id: "g4", semesterId: "sem1", studentClassId: "sc2",
                nameShort: "P2", ordinal: 2, value: nil, date: nil
            ),
        ]
        let overview = snapshot(studentGrades: grades)
            .meOverview(now: date(month: 4, day: 16, hour: 9, minute: 41), calendar: calendar)

        let countdown = try #require(overview.countdown)
        #expect(countdown.daysLeft == 82)
        #expect(countdown.weeksLeft == 11)
        #expect(countdown.hoursLeft == 1968)
        // Today keeps only CALC (ALGI started at 08:00); then 11 Thursdays ×2
        // and 12 Fridays ×1.
        #expect(countdown.classesLeft == 35)
        #expect(countdown.weekendsLeft == 12)
        #expect(countdown.scheduledExams == 1)
        #expect(countdown.disciplineCount == 2)
    }

    @Test
    func campusPicksTheMostFrequentLocation() {
        let overview = snapshot().meOverview(now: date(month: 4, day: 16, hour: 9, minute: 41), calendar: calendar)

        // s1 hosts two slots, s2 one — Módulo 5 wins.
        #expect(overview.campus == "UEFS · Módulo 5")
    }
}

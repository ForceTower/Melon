import Foundation
import Testing

@testable import UNESKit

/// Mapping of the semester snapshot into the Horário week.
///
/// Fixture week: April 16, 2026 is a Thursday (upstream day 4), so the
/// Monday-first week runs April 13–19 — ISO week 16.
/// - ALGI (c1): Thursday 08:00–10:00, Friday 08:00–10:00
/// - CALC (c2): Thursday 10:20–11:10 + 11:10–12:00 (split slots)
struct ScheduleMappingTests {
    let calendar = Calendar.current

    private func date(day: Int, hour: Int, minute: Int) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: 4, day: day, hour: hour, minute: minute))!
    }

    private func snapshot(
        spaces: [SpaceRecord] = [
            SpaceRecord(id: "s1", semesterId: "sem1", location: "MT-14", campus: "Feira", modulo: "MT"),
        ],
        lectures: [LectureRecord] = []
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
            spaces: spaces,
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
                StudentClassRecord(id: "sc1", semesterId: "sem1", classId: "c1", missedClasses: nil),
                StudentClassRecord(id: "sc2", semesterId: "sem1", classId: "c2", missedClasses: nil),
            ],
            lectures: lectures
        )
    }

    @Test
    func weekIsMondayFirstAroundNow() {
        let overview = snapshot().scheduleOverview(now: date(day: 16, hour: 9, minute: 41), calendar: calendar)

        #expect(overview.semesterId == "sem1")
        #expect(overview.weekOfYear == 16)
        #expect(overview.days.map(\.dayNumber) == [13, 14, 15, 16, 17, 18, 19])
        #expect(overview.days[0].dayStamp == "2026-04-13")
        #expect(overview.todayIndex(now: date(day: 16, hour: 9, minute: 41)) == 3)
    }

    @Test
    func classesLandOnTheirWeekdayWithMergedSlots() {
        let overview = snapshot().scheduleOverview(now: date(day: 16, hour: 9, minute: 41), calendar: calendar)

        let thursday = overview.days[3]
        #expect(thursday.classes.map(\.code) == ["ALGI", "CALC"])
        #expect(thursday.classes[1].startMinute == 10 * 60 + 20)
        #expect(thursday.classes[1].endMinute == 12 * 60)
        #expect(thursday.classes[1].teacherName == "Adriana Matos Ferreira")

        #expect(overview.days[4].classes.map(\.code) == ["ALGI"])
        #expect(overview.days[5].classes.isEmpty)
        #expect(overview.days[6].classes.isEmpty)
    }

    @Test
    func locationSplitsIntoModuleRoomAndCampus() {
        let overview = snapshot().scheduleOverview(now: date(day: 16, hour: 9, minute: 41), calendar: calendar)

        let calc = overview.days[3].classes[1]
        #expect(calc.modulo == "MT")
        #expect(calc.room == "MT-14")
        #expect(calc.campus == "Feira")

        let algi = overview.days[3].classes[0]
        #expect(algi.modulo == nil)
        #expect(algi.room == nil)
        #expect(algi.campus == nil)
    }

    @Test
    func emptyLocationFieldsBecomeNil() {
        let spaces = [SpaceRecord(id: "s1", semesterId: "sem1", location: "PV-22", campus: "", modulo: " ")]
        let overview = snapshot(spaces: spaces)
            .scheduleOverview(now: date(day: 16, hour: 9, minute: 41), calendar: calendar)

        let calc = overview.days[3].classes[1]
        #expect(calc.room == "PV-22")
        #expect(calc.modulo == nil)
        #expect(calc.campus == nil)
    }

    @Test
    func topicResolvesAgainstEachDaysOwnDate() {
        let lectures = [
            LectureRecord(id: "l1", semesterId: "sem1", classId: "c1", date: "2026-04-17", subject: "Busca binária"),
        ]
        let overview = snapshot(lectures: lectures)
            .scheduleOverview(now: date(day: 16, hour: 9, minute: 41), calendar: calendar)

        #expect(overview.days[3].classes[0].topic == nil)
        #expect(overview.days[4].classes[0].topic == "Busca binária")
    }

    @Test
    func classStateFollowsTheClock() {
        let overview = snapshot().scheduleOverview(now: date(day: 16, hour: 9, minute: 41), calendar: calendar)
        let thursday = overview.days[3]
        let algi = thursday.classes[0]
        let calc = thursday.classes[1]

        #expect(algi.state(isToday: false, nowMinutes: 9 * 60) == .future)
        #expect(algi.state(isToday: true, nowMinutes: 9 * 60) == .now)
        #expect(algi.state(isToday: true, nowMinutes: 10 * 60) == .done)
        #expect(calc.state(isToday: true, nowMinutes: 9 * 60 + 41) == .next)
        #expect(calc.state(isToday: true, nowMinutes: 8 * 60) == .later)
    }
}

import Foundation
import Testing
import WidgetKit

@testable import UNESKit

/// State selection of the "Próxima aula" widget timeline.
///
/// Fixture week: April 16, 2026 is a Thursday (upstream day 4).
/// - ALGI: Thursday 08:00–10:00 and Friday 08:00–10:00
/// - CALC: Thursday 10:20–12:00 (the day's last class)
struct NextClassProviderTests {
    let calendar = Calendar.current

    private func date(day: Int, hour: Int, minute: Int = 0) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: 4, day: day, hour: hour, minute: minute))!
    }

    private func schedule() -> WidgetScheduleSnapshot {
        func session(_ classId: String, day: Int, start: Int, end: Int, code: String) -> WidgetScheduleSnapshot.Session {
            WidgetScheduleSnapshot.Session(
                classId: classId, day: day, startMinute: start, endMinute: end,
                code: code, title: code, room: "MT-14", teacherName: nil, colorIndex: 0
            )
        }
        return WidgetScheduleSnapshot(
            semesterCode: "20261",
            sessions: [
                session("c1", day: 4, start: 8 * 60, end: 10 * 60, code: "ALGI"),
                session("c2", day: 4, start: 10 * 60 + 20, end: 12 * 60, code: "CALC"),
                session("c1", day: 5, start: 8 * 60, end: 10 * 60, code: "ALGI"),
            ]
        )
    }

    private func timeline(at now: Date, schedule: WidgetScheduleSnapshot?) -> Timeline<NextClassEntry> {
        NextClassProvider.timeline(now: now, schedule: schedule, calendar: calendar)
    }

    private func status(of timeline: Timeline<NextClassEntry>, at date: Date) -> NextClassStatus? {
        timeline.entries.first { $0.date == date }?.status
    }

    @Test
    func showsTheUpcomingClassBeforeItStarts() {
        let timeline = timeline(at: date(day: 16, hour: 7), schedule: schedule())

        guard case let .upcoming(occurrence) = timeline.entries.first?.status else {
            Issue.record("expected upcoming, got \(String(describing: timeline.entries.first?.status))")
            return
        }
        #expect(occurrence.code == "ALGI")
        #expect(occurrence.start == date(day: 16, hour: 8))
    }

    @Test
    func runningClassHoldsTheWidgetUntilHalfway() {
        // ALGI runs 08:00–10:00; halfway lands at 09:00.
        let timeline = timeline(at: date(day: 16, hour: 8, minute: 30), schedule: schedule())

        guard case let .inClass(occurrence) = timeline.entries.first?.status else {
            Issue.record("expected inClass, got \(String(describing: timeline.entries.first?.status))")
            return
        }
        #expect(occurrence.code == "ALGI")
    }

    @Test
    func widgetHandsOverToTheNextClassAtHalfway() {
        let timeline = timeline(at: date(day: 16, hour: 8, minute: 30), schedule: schedule())

        guard case let .upcoming(occurrence) = status(of: timeline, at: date(day: 16, hour: 9)) else {
            Issue.record("expected upcoming at the halfway entry")
            return
        }
        #expect(occurrence.code == "CALC")
        #expect(occurrence.start == date(day: 16, hour: 10, minute: 20))
    }

    @Test
    func lastClassHoldsTheWidgetUntilItEnds() {
        // CALC's halfway (11:10) is past, but nothing follows today.
        let timeline = timeline(at: date(day: 16, hour: 11, minute: 30), schedule: schedule())

        guard case let .inClass(occurrence) = timeline.entries.first?.status else {
            Issue.record("expected inClass, got \(String(describing: timeline.entries.first?.status))")
            return
        }
        #expect(occurrence.code == "CALC")
    }

    @Test
    func dayIsDoneOnceTheLastClassEnds() {
        let timeline = timeline(at: date(day: 16, hour: 11, minute: 30), schedule: schedule())

        guard case let .dayDone(completed, next) = status(of: timeline, at: date(day: 16, hour: 12)) else {
            Issue.record("expected dayDone at the class-end entry")
            return
        }
        #expect(completed == 2)
        #expect(next?.code == "ALGI")
        #expect(next?.start == date(day: 17, hour: 8))
    }

    @Test
    func dayDonePointsAtTheNextDaysFirstClass() {
        let timeline = timeline(at: date(day: 16, hour: 13), schedule: schedule())

        guard case let .dayDone(completed, next) = timeline.entries.first?.status else {
            Issue.record("expected dayDone, got \(String(describing: timeline.entries.first?.status))")
            return
        }
        #expect(completed == 2)
        #expect(next?.start == date(day: 17, hour: 8))
    }

    @Test
    func midnightRollsTheWidgetIntoTheNextDay() {
        let timeline = timeline(at: date(day: 16, hour: 13), schedule: schedule())

        guard case let .upcoming(occurrence) = status(of: timeline, at: date(day: 17, hour: 0)) else {
            Issue.record("expected upcoming at the midnight entry")
            return
        }
        #expect(occurrence.code == "ALGI")
        #expect(occurrence.start == date(day: 17, hour: 8))
    }

    @Test
    func patternRepeatsBeyondTheSemesterDates() {
        // The app keeps showing the most recent semester's week after its
        // end date (active(today:) falls back to it) — the widget must too.
        let timeline = timeline(at: date(day: 16, hour: 13).addingTimeInterval(400 * 24 * 3600), schedule: schedule())

        guard case let .dayDone(_, next) = timeline.entries.first?.status else {
            Issue.record("expected dayDone, got \(String(describing: timeline.entries.first?.status))")
            return
        }
        #expect(next != nil)
    }

    // MARK: - NextClassStatus.compute (the entry point the Siri intents share)

    @Test
    func computeReportsTheRunningClass() {
        let occurrences = schedule().occurrences(from: date(day: 16, hour: 7), days: 9, calendar: calendar)

        let (status, today) = NextClassStatus.compute(
            at: date(day: 16, hour: 8, minute: 30), occurrences: occurrences, calendar: calendar
        )

        guard case let .inClass(occurrence) = status else {
            Issue.record("expected inClass, got \(status)")
            return
        }
        #expect(occurrence.code == "ALGI")
        #expect(today.count == 2)
    }

    @Test
    func computeHandsOverToTheNextClassAtHalfway() {
        let occurrences = schedule().occurrences(from: date(day: 16, hour: 7), days: 9, calendar: calendar)

        let (status, _) = NextClassStatus.compute(
            at: date(day: 16, hour: 9), occurrences: occurrences, calendar: calendar
        )

        guard case let .upcoming(occurrence) = status else {
            Issue.record("expected upcoming, got \(status)")
            return
        }
        #expect(occurrence.code == "CALC")
    }

    @Test
    func computeReportsDayDoneWithTheNextDaysClass() {
        let occurrences = schedule().occurrences(from: date(day: 16, hour: 7), days: 9, calendar: calendar)

        let (status, today) = NextClassStatus.compute(
            at: date(day: 16, hour: 12, minute: 30), occurrences: occurrences, calendar: calendar
        )

        guard case let .dayDone(completed, next) = status else {
            Issue.record("expected dayDone, got \(status)")
            return
        }
        #expect(completed == 2)
        #expect(next?.start == date(day: 17, hour: 8))
        #expect(today.count == 2)
    }

    @Test
    func signedOutWithoutAPublishedSnapshot() {
        let timeline = timeline(at: date(day: 16, hour: 7), schedule: nil)

        #expect(timeline.entries.count == 1)
        #expect(timeline.entries.first?.status == .signedOut)
    }

    @Test
    func emptySemesterFallsBackToTheQuietCard() {
        let empty = WidgetScheduleSnapshot(semesterCode: "20261", sessions: [])
        let timeline = timeline(at: date(day: 16, hour: 7), schedule: empty)

        #expect(timeline.entries.first?.status == .dayDone(completed: 0, next: nil))
    }
}

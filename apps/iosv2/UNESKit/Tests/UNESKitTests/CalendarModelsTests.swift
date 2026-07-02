import Foundation
import Testing

@testable import UNESKit

struct CalendarModelsTests {
    static let calendar = Calendar.current

    static func day(_ year: Int, _ month: Int, _ day: Int) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day))!
    }

    /// Apr 17 2026 — the design prototype's pinned "hoje".
    static let today = day(2026, 4, 17)

    static func event(
        id: String = "e",
        title: String = "Evento",
        start: Date,
        end: Date? = nil,
        closed: Bool = false,
        scope: AcademicEvent.Scope = .general,
        origin: AcademicEvent.Origin = .manual
    ) -> CalendarEvent {
        CalendarEvent(id: id, title: title, start: start, end: end, closed: closed, scope: scope, origin: origin)
    }

    // MARK: Mapping

    @Test
    func mappingResolvesDatesAndStripsTheStudentSuffix() {
        let upstream = AcademicEvent(
            id: "e1",
            summary: "Período para trancamento de disciplinas — Estudante",
            start: "2026-03-30",
            end: "2026-04-15",
            fixed: false,
            closed: false,
            scope: .general,
            origin: .manual
        )

        let event = CalendarEvent(upstream)

        #expect(event?.title == "Período para trancamento de disciplinas")
        #expect(event?.start == Self.day(2026, 3, 30))
        #expect(event?.end == Self.day(2026, 4, 15))
        #expect(event?.spanDays == 17)
    }

    @Test
    func mappingRejectsMalformedStamps() {
        let upstream = AcademicEvent(
            id: "e1",
            summary: "Evento",
            start: "hoje",
            end: nil,
            fixed: false,
            closed: false,
            scope: .general,
            origin: .manual
        )

        #expect(CalendarEvent(upstream) == nil)
    }

    // MARK: Category

    @Test
    func closedEventsAreHolidaysRegardlessOfOrigin() {
        #expect(Self.event(start: Self.today, closed: true, origin: .evaluation).category == .holiday)
        #expect(Self.event(start: Self.today, origin: .evaluation).category == .exam)
        #expect(Self.event(start: Self.today, origin: .finalExam).category == .exam)
        #expect(Self.event(start: Self.today, origin: .secondCall).category == .exam)
        #expect(Self.event(start: Self.today, origin: .manual).category == .deadline)
        #expect(Self.event(start: Self.today, origin: .unknown).category == .deadline)
    }

    // MARK: Status

    @Test
    func statusComparesWholeDays() {
        let single = Self.event(start: Self.day(2026, 4, 16))
        #expect(CalendarMath.status(single, today: Self.today) == .past)

        let runningRange = Self.event(start: Self.day(2026, 4, 13), end: Self.day(2026, 4, 20))
        #expect(CalendarMath.status(runningRange, today: Self.today) == .active)

        let startingToday = Self.event(start: Self.today)
        #expect(CalendarMath.status(startingToday, today: Self.today) == .active)

        let upcoming = Self.event(start: Self.day(2026, 4, 21))
        #expect(CalendarMath.status(upcoming, today: Self.today) == .future)
    }

    // MARK: Countdown

    @Test
    func countdownPhrasesCoverTheWholeTimeline() {
        func phrase(start: Date, end: Date? = nil) -> String {
            CalendarMath.countdown(Self.event(start: start, end: end), today: Self.today).phrase
        }

        #expect(phrase(start: Self.today) == "hoje")
        #expect(phrase(start: Self.day(2026, 4, 18)) == "amanhã")
        #expect(phrase(start: Self.day(2026, 4, 22)) == "em 5 dias")
        #expect(phrase(start: Self.day(2026, 4, 13), end: Self.day(2026, 4, 17)) == "termina hoje")
        #expect(phrase(start: Self.day(2026, 4, 16), end: Self.day(2026, 4, 18)) == "termina amanhã")
        #expect(phrase(start: Self.day(2026, 4, 13), end: Self.day(2026, 4, 20)) == "termina em 3 dias")
        #expect(phrase(start: Self.day(2026, 4, 16)) == "há 1 dia")
        #expect(phrase(start: Self.day(2026, 4, 10)) == "há 7 dias")
    }

    @Test
    func countdownSplitsTheNumberForTheHero() {
        let running = CalendarMath.countdown(
            Self.event(start: Self.day(2026, 4, 13), end: Self.day(2026, 4, 20)),
            today: Self.today
        )
        #expect(running.number == "3")
        #expect(running.tail == "dias restantes")

        let endsTomorrow = CalendarMath.countdown(
            Self.event(start: Self.day(2026, 4, 16), end: Self.day(2026, 4, 18)),
            today: Self.today
        )
        #expect(endsTomorrow.number == "1")
        #expect(endsTomorrow.tail == "dia restante")

        let startsToday = CalendarMath.countdown(Self.event(start: Self.today), today: Self.today)
        #expect(startsToday.number == "hoje")
        #expect(startsToday.tail.isEmpty)
    }

    // MARK: Hero pick

    @Test
    func nextDeadlinePrefersTheActiveEventClosingSoonest() {
        let closingLater = Self.event(id: "later", start: Self.day(2026, 4, 10), end: Self.day(2026, 4, 30))
        let closingSoon = Self.event(id: "soon", start: Self.day(2026, 4, 13), end: Self.day(2026, 4, 20))
        let upcoming = Self.event(id: "next", start: Self.day(2026, 4, 18))

        let hero = CalendarMath.nextDeadline(in: [closingLater, closingSoon, upcoming], today: Self.today)

        #expect(hero?.id == "soon")
    }

    @Test
    func nextDeadlineSkipsRunningHolidaysAndFallsBackToTheFuture() {
        let holiday = Self.event(id: "holiday", start: Self.day(2026, 4, 16), end: Self.day(2026, 4, 19), closed: true)
        let upcoming = Self.event(id: "next", start: Self.day(2026, 4, 22))
        let done = Self.event(id: "done", start: Self.day(2026, 4, 10))

        let hero = CalendarMath.nextDeadline(in: [holiday, upcoming, done], today: Self.today)

        #expect(hero?.id == "next")
        #expect(CalendarMath.nextDeadline(in: [done], today: Self.today) == nil)
    }

    // MARK: Day lookup + grouping

    @Test
    func eventsOnADayMatchAnyOverlap() {
        let range = Self.event(id: "range", start: Self.day(2026, 4, 13), end: Self.day(2026, 4, 20))
        let single = Self.event(id: "single", start: Self.day(2026, 4, 17))
        let other = Self.event(id: "other", start: Self.day(2026, 4, 25))

        let hits = CalendarMath.events(on: Self.today, in: [other, single, range])

        #expect(hits.map(\.id) == ["range", "single"])
    }

    @Test
    func groupingBucketsByStartMonthInOrder() {
        let april = Self.event(id: "apr", start: Self.day(2026, 4, 21))
        let may = Self.event(id: "may", start: Self.day(2026, 5, 1))
        let june = Self.event(id: "jun", start: Self.day(2026, 6, 4))

        let groups = [may, april, june].groupedByMonth()

        #expect(groups.map(\.month) == [4, 5, 6])
        #expect(groups.first?.events.map(\.id) == ["apr"])
    }

    // MARK: Formatting

    @Test
    func dateRangesCollapseWithinAMonth() {
        #expect(CalendarFormat.dateRange(start: Self.day(2026, 4, 13), end: Self.day(2026, 4, 20)) == "13 – 20 abr")
        #expect(CalendarFormat.dateRange(start: Self.day(2026, 4, 27), end: Self.day(2026, 5, 1)) == "27 abr – 01 mai")
        #expect(CalendarFormat.dateRange(start: Self.day(2026, 4, 9), end: nil) == "09 abr")
    }

    @Test
    func syncLabelReadsRelative() {
        let fetchedAt = Self.today
        let fourMinutes = Self.calendar.date(byAdding: .minute, value: 4, to: fetchedAt)!
        #expect(CalendarFormat.syncLabel(fetchedAt: fetchedAt, now: fetchedAt) == "Sincronizado com o SAGRES · agora")
        #expect(CalendarFormat.syncLabel(fetchedAt: fetchedAt, now: fourMinutes) == "Sincronizado com o SAGRES · há 4 min")
    }
}

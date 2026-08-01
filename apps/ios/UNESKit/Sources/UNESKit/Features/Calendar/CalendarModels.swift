import Foundation
import SwiftUI

/// One academic-calendar entry with its day stamps resolved to local
/// midnights, so view math never parses strings.
struct CalendarEvent: Equatable, Identifiable, Sendable {
    let id: String
    /// Display title — the " — Estudante" suffix SAGRES appends to
    /// student-facing rows adds nothing inside the app.
    let title: String
    let start: Date
    /// Midnight of the last day; nil for single-day events.
    let end: Date?
    /// Repeats every year — surfaces the "data fixa" chip in the detail sheet.
    let fixed: Bool
    /// Campus-shut day. Closed events render as holidays regardless of origin.
    let closed: Bool
    let scope: AcademicEvent.Scope
    let origin: AcademicEvent.Origin
    /// The student's own entry behind this row — nil for the institutional
    /// feed. Set means the row is editable and carries the personal extras
    /// (class tag, reminder, notes).
    let personal: PersonalEvent?

    var isPersonal: Bool { personal != nil }

    var endOrStart: Date { end ?? start }

    /// Days the event spans, inclusive.
    var spanDays: Int {
        CalendarMath.daysBetween(start, endOrStart) + 1
    }

    var category: CalendarCategory {
        if let personal { return personal.category.calendarCategory }
        if closed { return .holiday }
        switch origin {
        case .evaluation, .finalExam, .secondCall, .secondEpoch: return .exam
        case .manual, .unknown: return .deadline
        }
    }

    /// What follows the category on a row: the linked class code when there
    /// is one, "meu" for the student's own entries, the âmbito otherwise.
    var provenanceLabel: String {
        if let personal {
            return personal.discipline?.code ?? .localized(.calendarPersonalBadge)
        }
        return scope.label
    }

    init(
        id: String,
        title: String,
        start: Date,
        end: Date? = nil,
        fixed: Bool = false,
        closed: Bool = false,
        scope: AcademicEvent.Scope = .general,
        origin: AcademicEvent.Origin = .manual,
        personal: PersonalEvent? = nil
    ) {
        self.id = id
        self.title = title
        self.start = start
        self.end = end
        self.fixed = fixed
        self.closed = closed
        self.scope = scope
        self.origin = origin
        self.personal = personal
    }

    init?(_ event: AcademicEvent, calendar: Calendar = .current) {
        guard let start = CalendarFormat.parse(event.start, calendar: calendar) else { return nil }
        self.init(
            id: event.id,
            title: event.summary.replacingOccurrences(of: " — Estudante", with: ""),
            start: start,
            end: event.end.flatMap { CalendarFormat.parse($0, calendar: calendar) },
            fixed: event.fixed,
            closed: event.closed,
            scope: event.scope,
            origin: event.origin
        )
    }

    /// Personal entries join the same timeline. They carry no scope of their
    /// own — the scope filter routes them through its "Meus" segment.
    init?(_ event: PersonalEvent, calendar: Calendar = .current) {
        guard let start = CalendarFormat.parse(event.start, calendar: calendar) else { return nil }
        self.init(
            id: event.id,
            title: event.title,
            start: start,
            end: event.end.flatMap { CalendarFormat.parse($0, calendar: calendar) },
            personal: event
        )
    }
}

extension PersonalEvent.Category {
    var calendarCategory: CalendarCategory {
        switch self {
        case .task: .task
        case .exam: .exam
        case .study: .study
        case .life: .life
        }
    }

    var label: String {
        switch self {
        case .task: .localized(.calendarPersonalCategoryTask)
        case .exam: .localized(.calendarPersonalCategoryExam)
        case .study: .localized(.calendarPersonalCategoryStudy)
        case .life: .localized(.calendarPersonalCategoryLife)
        }
    }
}

extension PersonalEvent.Reminder {
    /// Segment copy — "1 dia", short enough for four equal segments.
    var label: String {
        switch self {
        case .none: .localized(.calendarPersonalReminderNone)
        case .dayBefore: .localized(.calendarPersonalReminderDay)
        case .threeDays: .localized(.calendarPersonalReminderThreeDays)
        case .week: .localized(.calendarPersonalReminderWeek)
        }
    }

    /// Detail-sheet copy — "1 dia antes".
    var detailLabel: String {
        switch self {
        case .none: .localized(.calendarPersonalReminderNone)
        case .dayBefore: .localized(.calendarPersonalReminderDayLong)
        case .threeDays: .localized(.calendarPersonalReminderThreeDaysLong)
        case .week: .localized(.calendarPersonalReminderWeekLong)
        }
    }
}

/// One entry of the composer's class picker: the tag that gets stored plus
/// the palette slot the discipline already uses elsewhere in the app.
struct PersonalEventDisciplineOption: Equatable, Identifiable, Sendable {
    let tag: PersonalEvent.DisciplineTag
    let colorIndex: Int

    var id: String { tag.id }
}

/// Visual category behind the tone tiles, dots and filter segments. The last
/// three only ever come from the student's own entries.
enum CalendarCategory: CaseIterable, Hashable, Sendable {
    case deadline, exam, holiday, task, study, life

    var label: String {
        switch self {
        case .deadline: .localized(.calendarCategoryDeadline)
        case .exam: .localized(.calendarCategoryExam)
        case .holiday: .localized(.calendarCategoryHoliday)
        case .task: .localized(.calendarPersonalCategoryTask)
        case .study: .localized(.calendarPersonalCategoryStudy)
        case .life: .localized(.calendarPersonalCategoryLife)
        }
    }

    var icon: String {
        switch self {
        case .deadline: "clock"
        case .exam: "doc.text"
        case .holiday: "sun.max"
        case .task: "checkmark.square"
        case .study: "book"
        case .life: "star"
        }
    }

    /// Mirrors Android's `CalendarMath.categorize(event).name.lowercase()`
    /// analytics property value.
    var analyticsValue: String {
        switch self {
        case .deadline: "deadline"
        case .exam: "exam"
        case .holiday: "holiday"
        case .task: "task"
        case .study: "study"
        case .life: "life"
        }
    }

    /// coral / violet / tangerine, then magenta / teal / moss for the
    /// student's own kinds — all lifted for dark surfaces.
    var color: Color {
        switch self {
        case .deadline: UNESColor.readable(0xE85D4E)
        case .exam: UNESColor.readable(0x7A5AD0)
        case .holiday: UNESColor.readable(0xE8894E)
        case .task: UNESColor.readable(0xB23A7A)
        case .study: UNESColor.readable(0x3B9EAE)
        case .life: UNESColor.readable(0x5C8C3E)
        }
    }

    /// Mesh behind the hero when this category headlines it.
    var mesh: MeshView.Variant {
        switch self {
        case .deadline, .task, .life: .rose
        case .exam, .study: .cool
        case .holiday: .warm
        }
    }
}

enum CalendarStatus: Equatable, Sendable {
    case past, active, future
}

/// One decoration dot under a day in the month grid — filled for the
/// institutional feed, a ring for the student's own entries.
struct CalendarDayDot: Hashable, Sendable {
    let category: CalendarCategory
    let isPersonal: Bool
}

/// The agenda-row countdown, pre-split so the hero and detail sheet can
/// render the number at display size.
struct CalendarCountdown: Equatable, Sendable {
    /// Full phrase — "em 12 dias", "termina amanhã".
    let phrase: String
    /// Emphasized part — "12", "hoje", "amanhã".
    let number: String
    /// What follows the number — "dias", "dias restantes".
    let tail: String
}

enum CalendarMath {
    /// Integer days `b - a`, ignoring time of day.
    static func daysBetween(_ a: Date, _ b: Date, calendar: Calendar = .current) -> Int {
        let da = calendar.startOfDay(for: a)
        let db = calendar.startOfDay(for: b)
        return calendar.dateComponents([.day], from: da, to: db).day ?? 0
    }

    static func status(_ event: CalendarEvent, today: Date, calendar: Calendar = .current) -> CalendarStatus {
        let toStart = daysBetween(today, event.start, calendar: calendar)
        let toEnd = daysBetween(today, event.endOrStart, calendar: calendar)
        if toEnd < 0 { return .past }
        if toStart > 0 { return .future }
        return .active
    }

    static func countdown(_ event: CalendarEvent, today: Date, calendar: Calendar = .current) -> CalendarCountdown {
        let toStart = daysBetween(today, event.start, calendar: calendar)
        let toEnd = daysBetween(today, event.endOrStart, calendar: calendar)
        switch (toStart, toEnd) {
        case (0, _):
            return CalendarCountdown(
                phrase: .localized(.calendarCountdownToday),
                number: .localized(.calendarCountdownToday),
                tail: ""
            )
        case (1, _):
            return CalendarCountdown(
                phrase: .localized(.calendarCountdownTomorrow),
                number: .localized(.calendarCountdownTomorrow),
                tail: ""
            )
        case (let s, _) where s > 1:
            return CalendarCountdown(
                phrase: .localized(.calendarCountdownInDays(s)),
                number: "\(s)",
                tail: .localized(.calendarCountdownDaysUnit)
            )
        case (_, 0):
            return CalendarCountdown(
                phrase: .localized(.calendarCountdownEndsToday),
                number: .localized(.calendarCountdownToday),
                tail: .localized(.calendarCountdownLastDay)
            )
        case (_, 1):
            return CalendarCountdown(
                phrase: .localized(.calendarCountdownEndsTomorrow),
                number: "1",
                tail: .localized(.calendarCountdownDayLeft)
            )
        case (_, let e) where e > 1:
            return CalendarCountdown(
                phrase: .localized(.calendarCountdownEndsInDays(e)),
                number: "\(e)",
                tail: .localized(.calendarCountdownDaysLeft)
            )
        default:
            let since = -toStart
            return CalendarCountdown(
                phrase: .localized(.calendarCountdownDaysAgo(since)),
                number: "\(since)",
                tail: .localized(since == 1 ? .calendarCountdownAgoUnitOne : .calendarCountdownAgoUnitOther)
            )
        }
    }

    /// The hero pick: the active event closing soonest, else the next
    /// upcoming one. Closed (holiday) events never headline while running —
    /// there is nothing to act on.
    static func nextDeadline(in events: [CalendarEvent], today: Date, calendar: Calendar = .current) -> CalendarEvent? {
        let active = events.filter { !$0.closed && status($0, today: today, calendar: calendar) == .active }
        if let soonest = active.min(by: { $0.endOrStart < $1.endOrStart }) {
            return soonest
        }
        return events
            .filter { status($0, today: today, calendar: calendar) == .future }
            .min { $0.start < $1.start }
    }

    /// Events overlapping `day`, earliest first. All dates are local
    /// midnights, so plain comparison is the overlap test.
    static func events(on day: Date, in events: [CalendarEvent]) -> [CalendarEvent] {
        events
            .filter { $0.start <= day && day <= $0.endOrStart }
            .sorted { ($0.start, $0.title) < ($1.start, $1.title) }
    }
}

// MARK: - Filters

/// The segmented row. "Pessoal" entries have no segment of their own — they
/// live under "Tudo" and under the scope row's "Meus".
enum CalendarCategoryFilter: String, CaseIterable, Equatable, Sendable {
    case all, deadline, exam, task, study, holiday

    var label: String {
        switch self {
        case .all: .localized(.calendarFilterAll)
        case .deadline: .localized(.calendarFilterDeadlines)
        case .exam: .localized(.calendarFilterExams)
        case .task: .localized(.calendarFilterTasks)
        case .study: .localized(.calendarFilterStudy)
        case .holiday: .localized(.calendarFilterHolidays)
        }
    }

    /// The segment's leading dot; the "Tudo" segment has none.
    var category: CalendarCategory? {
        switch self {
        case .all: nil
        case .deadline: .deadline
        case .exam: .exam
        case .task: .task
        case .study: .study
        case .holiday: .holiday
        }
    }

    func matches(_ event: CalendarEvent) -> Bool {
        category.map { event.category == $0 } ?? true
    }
}

enum CalendarScopeFilter: String, CaseIterable, Equatable, Sendable {
    case all, personal, general, faculty, course, classScope

    var label: String {
        switch self {
        case .all: .localized(.calendarScopeAll)
        case .personal: .localized(.calendarScopePersonal)
        case .general: .localized(.calendarScopeGeneral)
        case .faculty: .localized(.calendarScopeFaculty)
        case .course: .localized(.calendarScopeCourse)
        case .classScope: .localized(.calendarScopeClassScope)
        }
    }

    var scope: AcademicEvent.Scope? {
        switch self {
        case .all, .personal: nil
        case .general: .general
        case .faculty: .faculty
        case .course: .course
        case .classScope: .classScope
        }
    }

    /// Personal entries carry a placeholder scope, so the institutional
    /// segments have to exclude them explicitly.
    func matches(_ event: CalendarEvent) -> Bool {
        switch self {
        case .all: true
        case .personal: event.isPersonal
        default: !event.isPersonal && event.scope == scope
        }
    }
}

extension AcademicEvent.Scope {
    /// "Âmbito" display label.
    var label: String {
        switch self {
        case .general: .localized(.calendarScopeGeneral)
        case .faculty: .localized(.calendarScopeFaculty)
        case .course: .localized(.calendarScopeCourse)
        case .classScope: .localized(.calendarScopeClassScope)
        case .campus: .localized(.calendarScopeCampus)
        case .unknown: "—"
        }
    }
}

// MARK: - Grouping

struct CalendarMonthGroup: Equatable, Identifiable, Sendable {
    /// Both from `Calendar.dateComponents` — `month` is 1-based.
    let year: Int
    let month: Int
    let events: [CalendarEvent]

    var id: String { String(format: "%04d-%02d", year, month) }
}

extension [CalendarEvent] {
    /// Buckets by start month, both levels in chronological order.
    func groupedByMonth(calendar: Calendar = .current) -> [CalendarMonthGroup] {
        var buckets: [String: (year: Int, month: Int, events: [CalendarEvent])] = [:]
        for event in self {
            let components = calendar.dateComponents([.year, .month], from: event.start)
            let year = components.year ?? 0
            let month = components.month ?? 0
            buckets[String(format: "%04d-%02d", year, month), default: (year, month, [])].events.append(event)
        }
        return buckets.keys.sorted().map { key in
            let bucket = buckets[key]!
            return CalendarMonthGroup(year: bucket.year, month: bucket.month, events: bucket.events)
        }
    }
}

// MARK: - Fixtures

extension [CalendarEvent] {
    /// The institutional feed and the student's own entries on one timeline —
    /// the same merge the screen renders.
    static func preview(today: Date = .now) -> [CalendarEvent] {
        let institutional = [AcademicEvent].preview(now: today).compactMap { CalendarEvent($0) }
        let personal = [PersonalEvent].preview(now: today).compactMap { CalendarEvent($0) }
        return (institutional + personal).sorted { ($0.start, $0.title) < ($1.start, $1.title) }
    }
}

extension [PersonalEventDisciplineOption] {
    static let preview: [PersonalEventDisciplineOption] = [
        PersonalEventDisciplineOption(
            tag: PersonalEvent.DisciplineTag(id: "d1", code: "EXA805", name: "Algoritmos e Programação II"),
            colorIndex: 0
        ),
        PersonalEventDisciplineOption(
            tag: PersonalEvent.DisciplineTag(id: "d2", code: "EXA704", name: "Cálculo Diferencial II"),
            colorIndex: 1
        ),
        PersonalEventDisciplineOption(
            tag: PersonalEvent.DisciplineTag(id: "d3", code: "EXA412", name: "Física II"),
            colorIndex: 3
        ),
    ]
}

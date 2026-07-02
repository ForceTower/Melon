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

    var endOrStart: Date { end ?? start }

    /// Days the event spans, inclusive.
    var spanDays: Int {
        CalendarMath.daysBetween(start, endOrStart) + 1
    }

    var category: CalendarCategory {
        if closed { return .holiday }
        switch origin {
        case .evaluation, .finalExam, .secondCall, .secondEpoch: return .exam
        case .manual, .unknown: return .deadline
        }
    }

    init(
        id: String,
        title: String,
        start: Date,
        end: Date? = nil,
        fixed: Bool = false,
        closed: Bool = false,
        scope: AcademicEvent.Scope = .general,
        origin: AcademicEvent.Origin = .manual
    ) {
        self.id = id
        self.title = title
        self.start = start
        self.end = end
        self.fixed = fixed
        self.closed = closed
        self.scope = scope
        self.origin = origin
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
}

/// Visual category behind the tone tiles, dots and filter segments.
enum CalendarCategory: CaseIterable, Equatable, Sendable {
    case deadline, exam, holiday

    var label: String {
        switch self {
        case .deadline: "Prazo"
        case .exam: "Avaliação"
        case .holiday: "Feriado"
        }
    }

    var icon: String {
        switch self {
        case .deadline: "clock"
        case .exam: "doc.text"
        case .holiday: "sun.max"
        }
    }

    /// coral / violet / tangerine, lifted for dark surfaces.
    var color: Color {
        switch self {
        case .deadline: UNESColor.readable(0xE85D4E)
        case .exam: UNESColor.readable(0x7A5AD0)
        case .holiday: UNESColor.readable(0xE8894E)
        }
    }

    /// Mesh behind the hero when this category headlines it.
    var mesh: MeshView.Variant {
        switch self {
        case .deadline: .rose
        case .exam: .cool
        case .holiday: .warm
        }
    }
}

enum CalendarStatus: Equatable, Sendable {
    case past, active, future
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
            return CalendarCountdown(phrase: "hoje", number: "hoje", tail: "")
        case (1, _):
            return CalendarCountdown(phrase: "amanhã", number: "amanhã", tail: "")
        case (let s, _) where s > 1:
            return CalendarCountdown(phrase: "em \(s) dias", number: "\(s)", tail: "dias")
        case (_, 0):
            return CalendarCountdown(phrase: "termina hoje", number: "hoje", tail: "último dia")
        case (_, 1):
            return CalendarCountdown(phrase: "termina amanhã", number: "1", tail: "dia restante")
        case (_, let e) where e > 1:
            return CalendarCountdown(phrase: "termina em \(e) dias", number: "\(e)", tail: "dias restantes")
        default:
            let since = -toStart
            let days = since == 1 ? "1 dia" : "\(since) dias"
            return CalendarCountdown(phrase: "há \(days)", number: "\(since)", tail: since == 1 ? "dia atrás" : "dias atrás")
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

enum CalendarCategoryFilter: String, CaseIterable, Equatable, Sendable {
    case all, deadline, exam, holiday

    var label: String {
        switch self {
        case .all: "Tudo"
        case .deadline: "Prazos"
        case .exam: "Provas"
        case .holiday: "Feriados"
        }
    }

    /// The segment's leading dot; the "Tudo" segment has none.
    var category: CalendarCategory? {
        switch self {
        case .all: nil
        case .deadline: .deadline
        case .exam: .exam
        case .holiday: .holiday
        }
    }

    func matches(_ event: CalendarEvent) -> Bool {
        category.map { event.category == $0 } ?? true
    }
}

enum CalendarScopeFilter: String, CaseIterable, Equatable, Sendable {
    case all, general, faculty, course, classScope

    var label: String {
        switch self {
        case .all: "Todos"
        case .general: "Geral"
        case .faculty: "Universidade"
        case .course: "Curso"
        case .classScope: "Turma"
        }
    }

    var scope: AcademicEvent.Scope? {
        switch self {
        case .all: nil
        case .general: .general
        case .faculty: .faculty
        case .course: .course
        case .classScope: .classScope
        }
    }

    func matches(_ event: CalendarEvent) -> Bool {
        scope.map { event.scope == $0 } ?? true
    }
}

extension AcademicEvent.Scope {
    /// "Âmbito" display label.
    var label: String {
        switch self {
        case .general: "Geral"
        case .faculty: "Universidade"
        case .course: "Curso"
        case .classScope: "Turma"
        case .campus: "Campus"
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
    static func preview(today: Date = .now) -> [CalendarEvent] {
        [AcademicEvent].preview(now: today).compactMap { CalendarEvent($0) }
    }
}

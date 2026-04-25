import SwiftUI
import UIKit

/// Audience the event reaches. Mirrors the upstream `scope` field returned by
/// the academic-calendar feed (see `screens-calendar-data.jsx`).
enum CalendarScope: String, Hashable {
    case general = "GENERAL"
    case faculty = "FACULTY"
    case course  = "COURSE"
    case classScope = "CLASS"
    case campus  = "CAMPUS"

    var label: String {
        switch self {
        case .general:    return "Geral"
        case .faculty:    return "Universidade"
        case .course:     return "Curso"
        case .classScope: return "Turma"
        case .campus:     return "Campus"
        }
    }
}

/// How the event ended up in the calendar — used together with `closed` to
/// derive its visual category. Same labels the server emits.
enum CalendarOrigin: String, Hashable {
    case manual = "MANUAL"
    case evaluation = "EVALUATION"
    case finalExam = "FINAL_EXAM"
    case secondCall = "SECOND_CALL"
    case secondEpoch = "SECOND_EPOCH"
}

/// Visual category derived from `closed` + `origin`.
enum CalendarCategory: String, CaseIterable {
    case holiday, exam, deadline

    var label: String {
        switch self {
        case .holiday:  return "Feriado"
        case .exam:     return "Prova Final"
        case .deadline: return "Prazo"
        }
    }

    /// Accent applied to the rail, glyph and active-state shadow on a card.
    /// Exam uses an adaptive color: plum on light surfaces (matches the JSX
    /// `#2D1B4E`) and a light lavender on dark, since plum-on-plum reads as
    /// near-invisible on the dark theme.
    var color: Color {
        switch self {
        case .holiday:  return UNESColor.amber
        case .deadline: return UNESColor.coral
        case .exam:     return Self.examInk
        }
    }

    private static let examInk = Color(uiColor: UIColor { @Sendable trait in
        trait.userInterfaceStyle == .dark
            ? UIColor(red: 0xC4 / 255, green: 0xB1 / 255, blue: 0xE8 / 255, alpha: 1)  // lavender
            : UIColor(red: 0x2D / 255, green: 0x1B / 255, blue: 0x4E / 255, alpha: 1)  // plum
    })
}

enum CalendarStatus { case past, active, future }

struct CalendarEvent: Identifiable, Hashable {
    let id: String
    let description: String
    /// Local-date midnight. The JS source uses `YYYY-MM-DD` strings; here we
    /// pre-resolve to `Date` so view code never has to parse.
    let start: Date
    /// `nil` for single-day events.
    let end: Date?
    /// Repeats every year — surfaces an "anual" tag on the row.
    let fixed: Bool
    /// "Closed" days where the campus is shut. Holiday cards render filled.
    let closed: Bool
    let scope: CalendarScope
    let origin: CalendarOrigin

    var endOrStart: Date { end ?? start }

    /// Drop the `" — Estudante"` suffix the SAGRES feed appends to scope-specific
    /// rows, plus the prefix words ("Período ", "Feriado — ") so cards inside the
    /// ribbon variant don't waste their (very limited) horizontal space repeating
    /// the category label that already sits in the eyebrow.
    var displayDescription: String {
        description.replacingOccurrences(of: " — Estudante", with: "")
    }

    /// Tighter form used inside the ribbon's column-laid pills.
    var ribbonDescription: String {
        displayDescription
            .replacingOccurrences(of: "Período ", with: "")
            .replacingOccurrences(of: "Feriado — ", with: "")
    }
}

// MARK: - Helpers

enum CalendarMath {
    /// Pinned "today" — keeps the prototype's hero and progress bar
    /// deterministic across runs.
    static let today: Date = {
        var c = DateComponents()
        c.year = 2026
        c.month = 4
        c.day = 17
        return Calendar.current.date(from: c)!
    }()

    /// Integer days between two dates ignoring time of day. `b - a`.
    static func daysBetween(_ a: Date, _ b: Date) -> Int {
        let cal = Calendar.current
        let da = cal.startOfDay(for: a)
        let db = cal.startOfDay(for: b)
        return cal.dateComponents([.day], from: da, to: db).day ?? 0
    }

    static func categorize(_ ev: CalendarEvent) -> CalendarCategory {
        if ev.closed { return .holiday }
        switch ev.origin {
        case .evaluation, .finalExam, .secondCall, .secondEpoch:
            return .exam
        case .manual:
            return .deadline
        }
    }

    static func status(_ ev: CalendarEvent, today: Date = today) -> CalendarStatus {
        let s = ev.start
        let e = ev.end ?? s
        let ds = daysBetween(today, s)
        let de = daysBetween(today, e)
        if de < 0 { return .past }
        if ds > 0 { return .future }
        return .active
    }

    /// Localized countdown phrase ("hoje", "em 3 dias", "termina em 2 dias", …).
    static func countdown(_ ev: CalendarEvent, today: Date = today) -> String {
        let s = ev.start
        let e = ev.end ?? s
        let ds = daysBetween(today, s)
        let de = daysBetween(today, e)
        if ds == 0 { return "hoje" }
        if ds == 1 { return "amanhã" }
        if ds == -1 && ev.end != nil { return "termina amanhã" }
        if ds > 0 { return "em \(ds) dias" }
        if ds < 0, de >= 0 {
            if de == 0 { return "termina em hoje" }
            return "termina em \(de) dia\(de > 1 ? "s" : "")"
        }
        return "há \(abs(ds)) dias"
    }

    /// Hero countdown is split into a big number and a tail label. The split
    /// drives the typography contrast in `CalHero` and mirrors
    /// `parseCountdownNumber` / `parseCountdownTail` in the JSX.
    static func countdownParts(_ ev: CalendarEvent, today: Date = today) -> (number: String, tail: String) {
        let cd = countdown(ev, today: today)
        switch cd {
        case "hoje":            return ("hoje", "")
        case "amanhã":          return ("amanhã", "")
        case "termina amanhã":  return ("1", "dia até fechar")
        default: break
        }
        // Pull the first integer out of the phrase. Works for "em N dias",
        // "termina em N dias", "há N dias" — the bare-Scanner approach only
        // handled the single-leading-word forms and dropped "termina em …"
        // through to the catch-all, which rendered the whole phrase at the
        // 48pt hero size.
        let firstInt = cd.split(whereSeparator: { !$0.isNumber })
            .compactMap { Int($0) }
            .first
        if let n = firstInt {
            if cd.hasPrefix("termina em ")  { return ("\(n)", "dias até fechar") }
            if cd.hasPrefix("em ")          { return ("\(n)", "dias") }
            if cd.hasPrefix("há ")          { return ("\(n)", "dias atrás") }
            return ("\(n)", "")
        }
        return (cd, "")
    }

    /// Pick the next actionable event for the hero card. Active deadlines
    /// (closing soonest) win, else the nearest upcoming event.
    static func nextDeadline(in events: [CalendarEvent], today: Date = today) -> CalendarEvent? {
        let active = events.filter { !$0.closed && status($0, today: today) == .active }
        if !active.isEmpty {
            return active.sorted { $0.endOrStart < $1.endOrStart }.first
        }
        let future = events.filter { status($0, today: today) == .future }
        if !future.isEmpty {
            return future.sorted { $0.start < $1.start }.first
        }
        return nil
    }
}

// MARK: - Filters

enum CalendarCategoryFilter: String, CaseIterable, Identifiable {
    case all, deadline, exam, holiday

    var id: String { rawValue }

    var label: String {
        switch self {
        case .all:      return "Tudo"
        case .deadline: return "Prazos"
        case .exam:     return "Provas"
        case .holiday:  return "Feriados"
        }
    }

    func matches(_ ev: CalendarEvent) -> Bool {
        switch self {
        case .all:      return true
        case .deadline: return CalendarMath.categorize(ev) == .deadline
        case .exam:     return CalendarMath.categorize(ev) == .exam
        case .holiday:  return CalendarMath.categorize(ev) == .holiday
        }
    }
}

enum CalendarScopeFilter: String, CaseIterable, Identifiable {
    case all, general, faculty, course, classScope

    var id: String { rawValue }

    var label: String {
        switch self {
        case .all:        return "Todos"
        case .general:    return "Geral"
        case .faculty:    return "Universidade"
        case .course:     return "Curso"
        case .classScope: return "Turma"
        }
    }

    func matches(_ ev: CalendarEvent) -> Bool {
        switch self {
        case .all:        return true
        case .general:    return ev.scope == .general
        case .faculty:    return ev.scope == .faculty
        case .course:     return ev.scope == .course
        case .classScope: return ev.scope == .classScope
        }
    }
}

// MARK: - Grouping

struct CalendarMonthGroup: Identifiable {
    let year: Int
    let month: Int
    let events: [CalendarEvent]

    var id: String { "\(year)-\(month)" }
}

extension Array where Element == CalendarEvent {
    /// Group events by their start month, preserving chronological order. Used
    /// by both the agenda and ribbon variants.
    func groupedByMonth() -> [CalendarMonthGroup] {
        let cal = Calendar.current
        var buckets: [String: (year: Int, month: Int, events: [CalendarEvent])] = [:]
        for ev in self {
            let comps = cal.dateComponents([.year, .month], from: ev.start)
            let y = comps.year ?? 0
            let m = comps.month ?? 0
            let key = String(format: "%04d-%02d", y, m)
            buckets[key, default: (y, m, [])].events.append(ev)
        }
        return buckets.keys.sorted().map { key in
            let entry = buckets[key]!
            return CalendarMonthGroup(year: entry.year, month: entry.month, events: entry.events)
        }
    }
}

// MARK: - Formatting

enum CalendarFormat {
    static let monthsShort: [String] = [
        "jan","fev","mar","abr","mai","jun","jul","ago","set","out","nov","dez",
    ]

    static let monthsLong: [String] = [
        "janeiro","fevereiro","março","abril","maio","junho",
        "julho","agosto","setembro","outubro","novembro","dezembro",
    ]

    /// 0 = dom, matching `Calendar.component(.weekday, …) - 1`.
    static let weekdaysShort: [String] = ["dom","seg","ter","qua","qui","sex","sáb"]

    /// "27 abr"
    static func dateShort(_ d: Date) -> String {
        let cal = Calendar.current
        let day = cal.component(.day, from: d)
        let month = cal.component(.month, from: d) - 1
        return String(format: "%02d %@", day, monthsShort[month])
    }

    /// "27 – 01 mai" / "27 abr – 03 mai" depending on whether the months differ.
    static func dateRange(start: Date, end: Date?) -> String {
        guard let end else { return dateShort(start) }
        let cal = Calendar.current
        let sm = cal.component(.month, from: start)
        let em = cal.component(.month, from: end)
        if sm == em {
            let sd = cal.component(.day, from: start)
            let ed = cal.component(.day, from: end)
            return String(format: "%02d – %02d %@", sd, ed, monthsShort[sm - 1])
        }
        return "\(dateShort(start)) – \(dateShort(end))"
    }

    static func weekday(_ d: Date) -> String {
        let w = Calendar.current.component(.weekday, from: d) - 1
        return weekdaysShort[w]
    }
}

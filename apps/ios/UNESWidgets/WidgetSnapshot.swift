import Foundation
import SwiftUI

/// Wire-format snapshot the host app writes to the App Group container and
/// the widget extension reads back. Two targets carry their own Codable
/// type with the same field names — the JSON is the contract. Keep this in
/// sync with `apps/ios/UNES/Features/Widgets/WidgetSnapshotWriter.swift`.
///
/// Only raw schedule data lives here. Time-derived state (running /
/// upcoming / dayDone, countdowns) is recomputed from `Date()` at every
/// timeline tick, so a stale snapshot still produces a correct entry —
/// the widget just sees the same classes positioned around a newer clock.
struct WidgetSnapshot: Codable, Sendable {
    static let appGroup = "group.dev.forcetower.unes.ios"
    static let fileName = "next-class-snapshot.json"

    let generatedAt: Date
    let todayDateIso: String
    let today: [Class]
    /// First class on the next day that has any classes scheduled. Drives
    /// the `dayDone` copy. Null on Fridays / over breaks when nothing is
    /// upcoming in the week we have data for.
    let nextDay: NextDay?

    struct Class: Codable, Sendable {
        let classId: String
        let code: String
        let title: String
        let prof: String?
        let room: String?
        let topic: String?
        /// "HH:mm" — start of the class allocation.
        let startTime: String
        /// "HH:mm" — null when upstream didn't ship one.
        let endTime: String?
    }

    struct NextDay: Codable, Sendable {
        /// "YYYY-MM-DD".
        let dateIso: String
        /// Distance from today's date in days (1 = tomorrow, 2 = day after, ...).
        let daysAway: Int
        let first: Class
    }

    static func containerURL() -> URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroup)
    }

    static func load() -> WidgetSnapshot? {
        guard let dir = containerURL() else { return nil }
        let url = dir.appendingPathComponent(fileName)
        guard let data = try? Data(contentsOf: url) else { return nil }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try? decoder.decode(WidgetSnapshot.self, from: data)
    }

    /// Returns the effective "now" the provider should render against. In
    /// DEBUG builds, an ISO8601 string under
    /// `UserDefaults(suiteName: appGroup)["widget.debug.now"]` overrides the
    /// system clock so we can preview widget states without changing the
    /// host Mac clock. Set via:
    ///
    ///   xcrun simctl spawn booted defaults write group.dev.forcetower.unes.ios \
    ///     widget.debug.now -string "2026-05-12T10:00:00-03:00"
    ///
    /// Clear via `defaults delete`. Compiles out in Release.
    static func resolvedNow() -> Date {
        #if DEBUG
        if let override = debugClockOverride() {
            return override
        }
        #endif
        return Date()
    }

    #if DEBUG
    static var isDebugClockOverridden: Bool {
        debugClockOverride() != nil
    }

    private static func debugClockOverride() -> Date? {
        guard let defaults = UserDefaults(suiteName: appGroup),
              let raw = defaults.string(forKey: "widget.debug.now") else { return nil }
        return iso8601Parser.date(from: raw)
    }

    private static let iso8601Parser: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()
    #endif
}

// MARK: - Entry rendering

extension WidgetSnapshot {
    /// Pure derivation of a `NextClassEntry` from this snapshot at `now`.
    /// State + countdowns recomputed from absolute times — the host doesn't
    /// have to rewrite the file just because the clock advanced.
    func renderEntry(at now: Date, calendar: Calendar = .current) -> NextClassEntry {
        let nowMinutes = minutesOfDay(now, in: calendar)
        let todayIsToday = todayDateIso == isoDate(now, in: calendar)

        // If the snapshot was written on a different local date than now,
        // today's allocations no longer apply — fall through to nextDay if
        // present, else dayDone.
        let currentClasses = todayIsToday ? today : []

        let running = currentClasses.first { c in
            guard let start = parseHhMm(c.startTime), let end = parseHhMm(c.endTime) else { return false }
            return nowMinutes >= start && nowMinutes < end
        }

        let upcoming = currentClasses
            .compactMap { c -> (Class, Int)? in
                guard let start = parseHhMm(c.startTime), start > nowMinutes else { return nil }
                return (c, start)
            }
            .min { $0.1 < $1.1 }
            .map { $0.0 }

        let completedCount = currentClasses.filter { c in
            guard let end = parseHhMm(c.endTime) else { return false }
            return nowMinutes >= end
        }.count

        let bars = currentClasses.map { c -> NextClassEntry.TodayBar in
            let end = parseHhMm(c.endTime) ?? parseHhMm(c.startTime) ?? 0
            let state: NextClassEntry.TodayBar.State
            if nowMinutes >= end {
                state = .done
            } else if let next = upcoming, next.classId == c.classId {
                state = .next
            } else if let r = running, r.classId == c.classId {
                state = .next
            } else {
                state = .later
            }
            return NextClassEntry.TodayBar(
                code: shortCode(from: c.code),
                time: c.startTime,
                state: state,
                colorHex: WidgetColor.subjectHex(for: c.code)
            )
        }

        // In the final stretch of a running class we shift attention to the
        // next class so the student gets a heads-up before walking out, but
        // only when there's actually another class to flag — otherwise we
        // keep showing the running one so the widget doesn't go blank for
        // the last 30 minutes of the day.
        let inClassSwitchThreshold = 30
        if let r = running {
            let start = parseHhMm(r.startTime) ?? nowMinutes
            let end = parseHhMm(r.endTime) ?? (nowMinutes + 60)
            let endsIn = max(0, end - nowMinutes)
            let shouldHandOffToUpcoming = endsIn <= inClassSwitchThreshold && upcoming != nil
            if !shouldHandOffToUpcoming {
                return NextClassEntry(
                    date: now,
                    state: .inClass,
                    code: r.code,
                    shortCode: shortCode(from: r.code),
                    title: r.title,
                    shortTitle: shortTitle(from: r.title),
                    prof: r.prof ?? "",
                    room: r.room ?? "",
                    building: "",
                    startsIn: 0,
                    endsIn: endsIn,
                    totalDurationMin: max(1, end - start),
                    startTime: r.startTime,
                    endTime: r.endTime ?? "",
                    topic: r.topic,
                    todayBars: bars,
                    dayDoneLine: nil,
                    completedTodayCount: completedCount
                )
            }
        }

        if let n = upcoming {
            let start = parseHhMm(n.startTime) ?? nowMinutes
            let end = parseHhMm(n.endTime) ?? start
            return NextClassEntry(
                date: now,
                state: .upcoming,
                code: n.code,
                shortCode: shortCode(from: n.code),
                title: n.title,
                shortTitle: shortTitle(from: n.title),
                prof: n.prof ?? "",
                room: n.room ?? "",
                building: "",
                startsIn: max(0, start - nowMinutes),
                endsIn: 0,
                totalDurationMin: max(1, end - start),
                startTime: n.startTime,
                endTime: n.endTime ?? "",
                topic: n.topic,
                todayBars: bars,
                dayDoneLine: nil,
                completedTodayCount: completedCount
            )
        }

        // No class left today. If tomorrow (or the next populated day) has
        // one, surface it as upcoming so the main subject area on Small /
        // Large renders real data — countdown spans the day boundary. The
        // Medium widget keeps its dedicated dayDone treatment via the
        // `dayDoneLine` carried alongside.
        if let n = nextDay {
            let nextStart = parseHhMm(n.first.startTime) ?? 0
            let nextEnd = parseHhMm(n.first.endTime) ?? nextStart
            let startsIn = max(0, n.daysAway * 1440 - nowMinutes + nextStart)
            return NextClassEntry(
                date: now,
                state: .dayDone,
                code: n.first.code,
                shortCode: shortCode(from: n.first.code),
                title: n.first.title,
                shortTitle: shortTitle(from: n.first.title),
                prof: n.first.prof ?? "",
                room: n.first.room ?? "",
                building: "",
                startsIn: startsIn,
                endsIn: 0,
                totalDurationMin: max(1, nextEnd - nextStart),
                startTime: n.first.startTime,
                endTime: n.first.endTime ?? "",
                topic: n.first.topic,
                todayBars: bars,
                dayDoneLine: dayDoneLine(),
                completedTodayCount: completedCount
            )
        }

        return NextClassEntry(
            date: now,
            state: .dayDone,
            code: "", shortCode: "",
            title: "", shortTitle: "",
            prof: "", room: "", building: "",
            startsIn: 0, endsIn: 0, totalDurationMin: 0,
            startTime: "", endTime: "",
            topic: nil,
            todayBars: bars,
            dayDoneLine: nil,
            completedTodayCount: completedCount
        )
    }

    private func dayDoneLine() -> String? {
        guard let nextDay else { return nil }
        let when = nextDay.daysAway == 1 ? "amanhã" : "em \(nextDay.daysAway) dias"
        var line = "\(when), \(nextDay.first.startTime) · \(nextDay.first.title)"
        if let room = nextDay.first.room, !room.isEmpty {
            line += " — \(room)"
        }
        return line
    }
}

private func parseHhMm(_ value: String?) -> Int? {
    guard let value, value.count >= 4 else { return nil }
    let parts = value.split(separator: ":")
    guard parts.count >= 2, let h = Int(parts[0]), let m = Int(parts[1]) else { return nil }
    return h * 60 + m
}

private func minutesOfDay(_ date: Date, in calendar: Calendar) -> Int {
    let comps = calendar.dateComponents([.hour, .minute], from: date)
    return (comps.hour ?? 0) * 60 + (comps.minute ?? 0)
}

private let isoDayFormatter: DateFormatter = {
    let f = DateFormatter()
    f.locale = Locale(identifier: "en_US_POSIX")
    f.dateFormat = "yyyy-MM-dd"
    f.timeZone = TimeZone.current
    return f
}()

private func isoDate(_ date: Date, in calendar: Calendar) -> String {
    isoDayFormatter.timeZone = calendar.timeZone
    return isoDayFormatter.string(from: date)
}

/// Approximation of the design's `shortCode` — upstream doesn't carry one,
/// so we take the first token of the disciplina code (e.g. "MAT01023" stays
/// "MAT01023"; "CALC II" → "CALC"). Mirrors the fixture's intent.
private func shortCode(from code: String) -> String {
    let firstWord = code.split(separator: " ").first.map(String.init) ?? code
    return String(firstWord.prefix(6))
}

/// First clause before " — " / " – " / ":" if present, capped at 14 chars.
/// Upstream titles are full discipline names; the design shows a compact form.
private func shortTitle(from title: String) -> String {
    let separators = [" — ", " – ", " - ", ": "]
    var base = title
    for sep in separators {
        if let range = base.range(of: sep) {
            base = String(base[..<range.lowerBound])
        }
    }
    if base.count <= 14 { return base }
    return String(base.prefix(13)) + "…"
}

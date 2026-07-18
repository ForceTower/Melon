import Foundation

/// Locale-aware display strings for campus events. Built on `FormatStyle`
/// (Sendable) rather than cached `DateFormatter`s, like `HomeFormat`. Every
/// date renders in the event's time zone so the schedule reads the same for
/// a student traveling outside the campus zone.
enum CampusEventFormat {
    private static let locale = Locale.autoupdatingCurrent

    /// "Seg" / "Mon" — abbreviated weekday, capitalized, no trailing dot.
    static func weekdayShort(for date: Date, in timeZone: TimeZone) -> String {
        let short = date.formatted(style(.dateTime.weekday(.abbreviated), timeZone))
            .replacingOccurrences(of: ".", with: "")
        return short.prefix(1).uppercased() + short.dropFirst()
    }

    /// "Segunda-feira" / "Monday" — the schedule day heading.
    static func weekdayLong(for date: Date, in timeZone: TimeZone) -> String {
        let wide = date.formatted(style(.dateTime.weekday(.wide), timeZone))
        return wide.prefix(1).uppercased() + wide.dropFirst()
    }

    /// "04" — the day-tab number.
    static func dayNumber(for date: Date, in timeZone: TimeZone) -> String {
        date.formatted(style(.dateTime.day(.twoDigits), timeZone))
    }

    /// "6 de agosto" / "August 6".
    static func fullDate(for date: Date, in timeZone: TimeZone) -> String {
        date.formatted(style(.dateTime.day().month(.wide), timeZone))
    }

    /// "4 – 8 de agosto" / "August 4 – 8".
    static func dateRange(from start: Date, to end: Date, in timeZone: TimeZone) -> String {
        (start..<end).formatted(intervalStyle(.interval.day().month(.wide), timeZone))
    }

    /// "4 – 8 de agosto de 2025" / "August 4 – 8, 2025" — the welcome footer.
    static func dateRangeWithYear(from start: Date, to end: Date, in timeZone: TimeZone) -> String {
        (start..<end).formatted(intervalStyle(.interval.day().month(.wide).year(), timeZone))
    }

    /// "08:00" / "8:00 AM".
    static func time(for date: Date, in timeZone: TimeZone) -> String {
        date.formatted(style(Date.FormatStyle(date: .omitted, time: .shortened), timeZone))
    }

    /// "08:00 – 09:30", or just the start for open-ended activities.
    static func timeRange(from start: Date, to end: Date?, in timeZone: TimeZone) -> String {
        guard let end else { return time(for: start, in: timeZone) }
        return "\(time(for: start, in: timeZone)) – \(time(for: end, in: timeZone))"
    }

    /// "2025-08-04" — plain ISO calendar day (event zone), used as an
    /// analytics property so it matches the Android `LocalDate.toString()`.
    static func isoDay(for date: Date, in timeZone: TimeZone) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", components.year ?? 0, components.month ?? 0, components.day ?? 0)
    }

    private static func style(_ base: Date.FormatStyle, _ timeZone: TimeZone) -> Date.FormatStyle {
        var style = base
        style.locale = locale
        style.timeZone = timeZone
        return style
    }

    private static func intervalStyle(_ base: Date.IntervalFormatStyle, _ timeZone: TimeZone) -> Date.IntervalFormatStyle {
        var style = base
        style.locale = locale
        style.timeZone = timeZone
        return style
    }

    // MARK: Countdown

    struct Countdown {
        var days: Int
        var hours: Int
        var minutes: Int
        var seconds: Int
    }

    static func countdown(until target: Date, now: Date) -> Countdown {
        let left = max(0, Int(target.timeIntervalSince(now).rounded()))
        return Countdown(
            days: left / 86_400,
            hours: left % 86_400 / 3600,
            minutes: left % 3600 / 60,
            seconds: left % 60
        )
    }

    static func padded(_ value: Int) -> String {
        String(format: "%02d", value)
    }
}

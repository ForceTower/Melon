import Foundation

/// Locale-aware display strings for the Calendário screen. Built on
/// `FormatStyle` (Sendable) rather than fixed pt-BR arrays.
enum CalendarFormat {
    private static let locale = Locale.autoupdatingCurrent

    /// yyyy-MM-dd → local midnight.
    static func parse(_ stamp: String, calendar: Calendar = .current) -> Date? {
        let parts = stamp.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }

    /// "17 abr" / "Apr 17"
    static func dateShort(_ date: Date) -> String {
        let day = date.formatted(.dateTime.day(.twoDigits).locale(locale))
        let month = date.formatted(.dateTime.month(.abbreviated).locale(locale))
            .replacingOccurrences(of: ".", with: "")
        return "\(day) \(month)"
    }

    /// "13 – 20 abr" within a month, "27 abr – 03 mai" across months.
    static func dateRange(start: Date, end: Date?, calendar: Calendar = .current) -> String {
        guard let end else { return dateShort(start) }
        if calendar.component(.month, from: start) == calendar.component(.month, from: end),
           calendar.component(.year, from: start) == calendar.component(.year, from: end) {
            let startDay = start.formatted(.dateTime.day(.twoDigits).locale(locale))
            let endDay = end.formatted(.dateTime.day(.twoDigits).locale(locale))
            let month = start.formatted(.dateTime.month(.abbreviated).locale(locale))
                .replacingOccurrences(of: ".", with: "")
            return "\(startDay) – \(endDay) \(month)"
        }
        return "\(dateShort(start)) – \(dateShort(end))"
    }

    /// "Abril" / "April" — agenda month headers ask for the capitalized long name.
    static func monthTitle(_ month: Int, calendar: Calendar = .current) -> String {
        let date = calendar.date(from: DateComponents(year: 2000, month: month, day: 1)) ?? .now
        let wide = date.formatted(.dateTime.month(.wide).locale(locale))
        return wide.prefix(1).uppercased() + wide.dropFirst()
    }

    /// "qui" / "thu"
    static func weekday(_ date: Date) -> String {
        date.formatted(.dateTime.weekday(.abbreviated).locale(locale))
            .replacingOccurrences(of: ".", with: "")
            .lowercased()
    }

    /// "09" — agenda rows keep day numbers at two digits so columns align.
    static func dayNumber(_ date: Date) -> String {
        date.formatted(.dateTime.day(.twoDigits).locale(locale))
    }

    /// "17 de abril" / "April 17" — the grid's selected-day heading.
    static func dayTitle(_ date: Date) -> String {
        date.formatted(.dateTime.day().month(.wide).locale(locale))
    }

    /// "1 evento" / "12 eventos"
    static func eventCount(_ count: Int) -> String {
        .localized(.calendarEventCount(count))
    }

    /// "Dia único" / "5 dias" — the detail sheet's Duração cell.
    static func duration(days: Int) -> String {
        days <= 1 ? .localized(.calendarDurationSingleDay) : .localized(.calendarDurationDays(days))
    }

    /// "Sincronizado com o SAGRES · há 4 min" — relative to when the feed
    /// actually landed, not the prototype's hardcoded stamp.
    static func syncLabel(fetchedAt: Date, now: Date) -> String {
        let seconds = max(0, now.timeIntervalSince(fetchedAt))
        switch seconds {
        case ..<90: return .localized(.calendarSyncNow)
        case ..<3600: return .localized(.calendarSyncMinAgo(Int(seconds / 60)))
        case ..<86_400: return .localized(.calendarSyncHoursAgo(Int(seconds / 3600)))
        default: return .localized(.calendarSyncDaysAgo(Int(seconds / 86_400)))
        }
    }
}

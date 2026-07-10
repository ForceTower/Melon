import Foundation

/// Locale-aware display strings for the Home screen. Built on `FormatStyle`
/// (Sendable) rather than cached `DateFormatter`s.
enum HomeFormat {
    private static let locale = Locale.autoupdatingCurrent

    /// "Quinta · 17 abr" / "Thursday · Apr 17" — the view renders it uppercase.
    static func dayEyebrow(for date: Date) -> String {
        "\(weekdayName(for: date)) · \(shortDate(for: date))"
    }

    /// The wide weekday, capitalized; drops the pt-BR "-feira" tail when present.
    static func weekdayName(for date: Date) -> String {
        let wide = date.formatted(.dateTime.weekday(.wide).locale(locale))
        let trimmed = wide.replacingOccurrences(of: "-feira", with: "")
        return trimmed.prefix(1).uppercased() + trimmed.dropFirst()
    }

    /// "Qui" / "Thu" — abbreviated weekday, capitalized, no trailing dot.
    static func weekdayShort(for date: Date) -> String {
        let short = date.formatted(.dateTime.weekday(.abbreviated).locale(locale))
            .replacingOccurrences(of: ".", with: "")
        return short.prefix(1).uppercased() + short.dropFirst()
    }

    /// "17 abr" / "Apr 17".
    static func shortDate(for date: Date) -> String {
        let day = date.formatted(.dateTime.day().locale(locale))
        let month = date.formatted(.dateTime.month(.abbreviated).locale(locale))
            .replacingOccurrences(of: ".", with: "")
        return "\(day) \(month)"
    }

    /// "22 abr" from an upstream "yyyy-MM-dd" stamp.
    static func shortDate(fromDayStamp stamp: String, calendar: Calendar = .current) -> String? {
        let parts = stamp.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3,
              let date = calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
        else { return nil }
        return shortDate(for: date)
    }

    /// "Atualizado há 2 min" / "Updated 2 min ago" footer.
    static func updatedLabel(lastRefreshed: Date, now: Date) -> String {
        let seconds = max(0, now.timeIntervalSince(lastRefreshed))
        switch seconds {
        case ..<90: return .localized(.homeUpdatedJustNow)
        case ..<3600: return .localized(.homeUpdatedMinAgo(Int(seconds / 60)))
        case ..<86_400: return .localized(.homeUpdatedHoursAgo(Int(seconds / 3600)))
        default: return .localized(.homeUpdatedDaysAgo(Int(seconds / 86_400)))
        }
    }

    /// The hero countdown, mirroring the design: under an hour "39" + "min",
    /// under a day "1h05".
    struct Countdown {
        var big: String
        var unit: String?
    }

    static func countdown(until target: Date, now: Date) -> Countdown? {
        let left = max(0, target.timeIntervalSince(now))
        guard left < 24 * 3600 else { return nil }
        let totalMinutes = Int(left) / 60
        if totalMinutes >= 60 {
            return Countdown(big: "\(totalMinutes / 60)h\(String(format: "%02d", totalMinutes % 60))", unit: nil)
        }
        return Countdown(big: "\(totalMinutes)", unit: "min")
    }

    /// "9:41" — current-time label on the day list's live line.
    static func nowLabel(minutes: Int) -> String {
        "\(minutes / 60):" + String(format: "%02d", minutes % 60)
    }

    /// Full SAGRES names run long — first two names read naturally.
    static func teacherShort(_ name: String) -> String {
        name.split(separator: " ").prefix(2).joined(separator: " ")
    }
}

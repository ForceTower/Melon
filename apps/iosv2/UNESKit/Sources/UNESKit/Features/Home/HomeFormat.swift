import Foundation

/// pt-BR display strings for the Home screen. Built on `FormatStyle` (Sendable)
/// rather than cached `DateFormatter`s.
enum HomeFormat {
    private static let ptBR = Locale(identifier: "pt_BR")

    /// "Quinta · 17 abr" — the view renders it uppercase.
    static func dayEyebrow(for date: Date) -> String {
        "\(weekdayName(for: date)) · \(shortDate(for: date))"
    }

    /// "Quinta" — the wide pt-BR weekday without the "-feira" tail.
    static func weekdayName(for date: Date) -> String {
        let wide = date.formatted(.dateTime.weekday(.wide).locale(ptBR))
        let trimmed = wide.replacingOccurrences(of: "-feira", with: "")
        return trimmed.prefix(1).uppercased() + trimmed.dropFirst()
    }

    /// "Qui" — abbreviated weekday, capitalized, no trailing dot.
    static func weekdayShort(for date: Date) -> String {
        let short = date.formatted(.dateTime.weekday(.abbreviated).locale(ptBR))
            .replacingOccurrences(of: ".", with: "")
        return short.prefix(1).uppercased() + short.dropFirst()
    }

    /// "17 abr".
    static func shortDate(for date: Date) -> String {
        let day = date.formatted(.dateTime.day().locale(ptBR))
        let month = date.formatted(.dateTime.month(.abbreviated).locale(ptBR))
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

    /// "Atualizado há 2 min" footer.
    static func updatedLabel(lastRefreshed: Date, now: Date) -> String {
        let seconds = max(0, now.timeIntervalSince(lastRefreshed))
        switch seconds {
        case ..<90: return "Atualizado agora"
        case ..<3600: return "Atualizado há \(Int(seconds / 60)) min"
        case ..<86_400: return "Atualizado há \(Int(seconds / 3600))h"
        default: return "Atualizado há \(Int(seconds / 86_400))d"
        }
    }

    /// The hero countdown, mirroring the design: under an hour "39" + "min",
    /// under a day "1h05", and a live "em MM:SS" line underneath.
    struct Countdown {
        var big: String
        var unit: String?
        var sub: String
    }

    static func countdown(until target: Date, now: Date) -> Countdown? {
        let left = max(0, target.timeIntervalSince(now))
        guard left < 24 * 3600 else { return nil }
        let totalSeconds = Int(left)
        let totalMinutes = totalSeconds / 60
        let sub = String(format: "em %02d:%02d", totalMinutes, totalSeconds % 60)
        if totalMinutes >= 60 {
            return Countdown(
                big: "\(totalMinutes / 60)h\(String(format: "%02d", totalMinutes % 60))",
                unit: nil,
                sub: sub
            )
        }
        return Countdown(big: "\(totalMinutes)", unit: "min", sub: sub)
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

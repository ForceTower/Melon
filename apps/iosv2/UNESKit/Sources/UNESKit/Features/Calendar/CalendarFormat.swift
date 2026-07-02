import Foundation

/// pt-BR display strings for the Calendário screen. Month and weekday names
/// are fixed arrays — the whole screen is pt-BR regardless of device locale.
enum CalendarFormat {
    static let monthsShort = [
        "jan", "fev", "mar", "abr", "mai", "jun",
        "jul", "ago", "set", "out", "nov", "dez",
    ]

    static let monthsLong = [
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    ]

    /// 0 = dom, matching `Calendar.component(.weekday, …) - 1`.
    static let weekdaysShort = ["dom", "seg", "ter", "qua", "qui", "sex", "sáb"]

    /// yyyy-MM-dd → local midnight.
    static func parse(_ stamp: String, calendar: Calendar = .current) -> Date? {
        let parts = stamp.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }

    /// "17 abr"
    static func dateShort(_ date: Date, calendar: Calendar = .current) -> String {
        let day = calendar.component(.day, from: date)
        let month = calendar.component(.month, from: date)
        return String(format: "%02d %@", day, monthsShort[month - 1])
    }

    /// "13 – 20 abr" within a month, "27 abr – 03 mai" across months.
    static func dateRange(start: Date, end: Date?, calendar: Calendar = .current) -> String {
        guard let end else { return dateShort(start, calendar: calendar) }
        if calendar.component(.month, from: start) == calendar.component(.month, from: end),
           calendar.component(.year, from: start) == calendar.component(.year, from: end) {
            let month = calendar.component(.month, from: start)
            return String(
                format: "%02d – %02d %@",
                calendar.component(.day, from: start),
                calendar.component(.day, from: end),
                monthsShort[month - 1]
            )
        }
        return "\(dateShort(start, calendar: calendar)) – \(dateShort(end, calendar: calendar))"
    }

    /// "Abril" — agenda month headers ask for the capitalized long name.
    static func monthTitle(_ month: Int) -> String {
        monthsLong[month - 1].capitalized(with: Locale(identifier: "pt_BR"))
    }

    /// "qui"
    static func weekday(_ date: Date, calendar: Calendar = .current) -> String {
        weekdaysShort[calendar.component(.weekday, from: date) - 1]
    }

    /// "09" — agenda rows keep day numbers at two digits so columns align.
    static func dayNumber(_ date: Date, calendar: Calendar = .current) -> String {
        String(format: "%02d", calendar.component(.day, from: date))
    }

    /// "17 de abril" — the grid's selected-day heading.
    static func dayTitle(_ date: Date, calendar: Calendar = .current) -> String {
        let day = calendar.component(.day, from: date)
        let month = calendar.component(.month, from: date)
        return "\(day) de \(monthsLong[month - 1])"
    }

    /// "1 evento" / "12 eventos"
    static func eventCount(_ count: Int) -> String {
        count == 1 ? "1 evento" : "\(count) eventos"
    }

    /// "Dia único" / "5 dias" — the detail sheet's Duração cell.
    static func duration(days: Int) -> String {
        days <= 1 ? "Dia único" : "\(days) dias"
    }

    /// "Sincronizado com o SAGRES · há 4 min" — relative to when the feed
    /// actually landed, not the prototype's hardcoded stamp.
    static func syncLabel(fetchedAt: Date, now: Date) -> String {
        let seconds = max(0, now.timeIntervalSince(fetchedAt))
        let relative = switch seconds {
        case ..<90: "agora"
        case ..<3600: "há \(Int(seconds / 60)) min"
        case ..<86_400: "há \(Int(seconds / 3600))h"
        default: "há \(Int(seconds / 86_400))d"
        }
        return "Sincronizado com o SAGRES · \(relative)"
    }
}

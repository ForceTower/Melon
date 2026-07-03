import Foundation

/// Locale-aware display strings for the matrícula flow.
enum EnrollmentFormat {
    private static let locale = Locale.autoupdatingCurrent

    /// Weekday names on the upstream 0 = Sunday … 6 = Saturday scale, which
    /// lines up with `Calendar`'s symbol arrays.
    private static let calendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = locale
        return calendar
    }()

    /// "Seg" / "Mon" — abbreviated weekday, capitalized, no trailing dot.
    static func dayShort(_ day: Int) -> String {
        guard calendar.shortWeekdaySymbols.indices.contains(day) else { return "?" }
        let symbol = calendar.shortWeekdaySymbols[day].replacingOccurrences(of: ".", with: "")
        return symbol.prefix(1).uppercased() + symbol.dropFirst()
    }

    /// "segunda" / "monday" — wide weekday, lowercased, without the pt-BR "-feira" tail.
    static func dayFull(_ day: Int) -> String {
        guard calendar.weekdaySymbols.indices.contains(day) else { return "?" }
        return calendar.weekdaySymbols[day]
            .replacingOccurrences(of: "-feira", with: "")
            .lowercased()
    }

    /// "15 jun" — pt-BR month abbreviation without the trailing period.
    static func dayLabel(_ date: Date?) -> String {
        guard let date else { return "—" }
        return string(from: date, format: "d MMM").replacingOccurrences(of: ".", with: "")
    }

    /// "22 jun · 23h59" (pt-BR) / "22 Jun · 23:59" for the window's end bound.
    static func endLabel(_ date: Date?) -> String {
        guard let date else { return "—" }
        // pt-BR writes the deadline time as "23h59"; other locales use "23:59".
        let separator = locale.language.languageCode == .portuguese ? "h" : ":"
        return "\(dayLabel(date)) · \(string(from: date, format: "H'\(separator)'mm"))"
    }

    private static func string(from date: Date, format: String) -> String {
        let formatter = DateFormatter()
        formatter.locale = locale
        // The deadline is SAGRES wall-clock time (UEFS, no DST); rendering in
        // the device zone would shift it for anyone traveling.
        formatter.timeZone = TimeZone(identifier: "America/Bahia")
        formatter.dateFormat = format
        return formatter.string(from: date)
    }

    static func sectionCountLabel(_ count: Int) -> String {
        .localized(.enrollmentSectionsCount(count))
    }

    static func conflictCountLabel(_ count: Int) -> String {
        .localized(.enrollmentConflictsCount(count))
    }

    static func shiftLabel(_ shift: EnrollmentShift) -> String {
        switch shift {
        case .morning: .localized(.enrollmentShiftMorning)
        case .afternoon: .localized(.enrollmentShiftAfternoon)
        case .night: .localized(.enrollmentShiftNight)
        case .undefined: .localized(.enrollmentShiftUndefined)
        }
    }

    static func blockerLabel(_ blocker: EnrollmentBlocker) -> String {
        switch blocker {
        case let .conflicts(count): conflictCountLabel(count)
        case let .underMinimum(missing): .localized(.enrollmentBlockerUnderMin(missing))
        case let .overMaximum(excess): .localized(.enrollmentBlockerOverMax(excess))
        case .empty: .localized(.enrollmentBlockerEmpty)
        }
    }

    static func message(for error: any Error) -> String {
        guard let failure = error as? EnrollmentFailure else {
            return .localized(.enrollmentErrorGeneric)
        }
        // Exhaustive so a new failure case forces its copy here.
        switch failure {
        case .sessionExpired:
            return .localized(.enrollmentErrorSessionExpired)
        case .network:
            return .localized(.enrollmentErrorNetwork)
        case .server(let message?):
            return message
        case .server(nil):
            return .localized(.enrollmentErrorGeneric)
        }
    }

    /// One line per distinct time band: ("Seg, Qua", "13:30–15:30").
    static func scheduleLines(for section: EnrollmentSection) -> [(days: String, time: String)] {
        var order: [String] = []
        var daysByTime: [String: [Int]] = [:]
        for slot in section.slots {
            let time = "\(ScheduleFormat.timeLabel(slot.startMinute))–\(ScheduleFormat.timeLabel(slot.endMinute))"
            if daysByTime[time] == nil { order.append(time) }
            daysByTime[time, default: []].append(slot.day)
        }
        return order.map { time in
            let days = daysByTime[time, default: []].sorted().map(dayShort).joined(separator: ", ")
            return (days: days, time: time)
        }
    }
}

extension EnrollmentWindow {
    /// Whole days until the window closes, floored at zero.
    func daysLeft(now: Date) -> Int {
        guard let endDate else { return 0 }
        return max(0, Int(ceil(endDate.timeIntervalSince(now) / 86_400)))
    }

    /// Share of the window still ahead, 0…1 — the countdown ring's fill.
    func remainingFraction(now: Date) -> Double {
        guard let startDate, let endDate, endDate > startDate else { return 0 }
        let fraction = endDate.timeIntervalSince(now) / endDate.timeIntervalSince(startDate)
        return min(1, max(0, fraction))
    }
}

import Foundation

/// Display strings for the Horário screen.
enum ScheduleFormat {
    /// Monday-first weekday names for the day section header.
    static var dayNames: [String] {
        [
            .localized(.scheduleWeekdayMonday),
            .localized(.scheduleWeekdayTuesday),
            .localized(.scheduleWeekdayWednesday),
            .localized(.scheduleWeekdayThursday),
            .localized(.scheduleWeekdayFriday),
            .localized(.scheduleWeekdaySaturday),
            .localized(.scheduleWeekdaySunday),
        ]
    }

    /// Single-letter column labels for the week strip.
    static var dayLetters: [String] {
        [
            .localized(.scheduleWeekdayMondayLetter),
            .localized(.scheduleWeekdayTuesdayLetter),
            .localized(.scheduleWeekdayWednesdayLetter),
            .localized(.scheduleWeekdayThursdayLetter),
            .localized(.scheduleWeekdayFridayLetter),
            .localized(.scheduleWeekdaySaturdayLetter),
            .localized(.scheduleWeekdaySundayLetter),
        ]
    }

    /// "08:40" from minutes into the day.
    static func timeLabel(_ minutes: Int) -> String {
        String(format: "%02d:%02d", minutes / 60, minutes % 60)
    }

    /// "1h40" / "2h" / "45min" — durations, gaps, and countdowns.
    static func durationLabel(_ minutes: Int) -> String {
        guard minutes >= 60 else { return "\(minutes)min" }
        let remainder = minutes % 60
        return remainder == 0
            ? "\(minutes / 60)h"
            : "\(minutes / 60)h\(String(format: "%02d", remainder))"
    }

    /// "Semana 16 · 14–20 abr" from the week's first and last day stamps;
    /// cross-month weeks read "28 abr – 4 mai".
    static func weekEyebrow(weekOfYear: Int, first: String, last: String, calendar: Calendar = .current) -> String {
        guard let firstDate = parseDayStamp(first, calendar: calendar),
              let lastDate = parseDayStamp(last, calendar: calendar)
        else { return String.localized(.scheduleWeekNumber(weekOfYear)) }
        let sameMonth = calendar.component(.month, from: firstDate) == calendar.component(.month, from: lastDate)
        let range = sameMonth
            ? "\(calendar.component(.day, from: firstDate))–\(HomeFormat.shortDate(for: lastDate))"
            : "\(HomeFormat.shortDate(for: firstDate)) – \(HomeFormat.shortDate(for: lastDate))"
        return String.localized(.scheduleWeekNumberRange(weekOfYear, range))
    }

    /// "4 aulas · 08:00–18:00" / "1 aula · …" / "sem aulas".
    static func daySummary(for classes: [ScheduleClass]) -> String {
        guard let first = classes.first, let last = classes.last else { return String.localized(.scheduleDayNoClasses) }
        let span = "\(timeLabel(first.startMinute))–\(timeLabel(last.endMinute ?? last.startMinute))"
        return "\(String.localized(.scheduleClassesCount(classes.count))) · \(span)"
    }

    /// "C. Ribeiro" from the full upstream name.
    static func shortTeacherName(_ name: String) -> String {
        let parts = name.split(separator: " ")
        guard parts.count >= 2, let initial = parts[0].first else { return name }
        return "\(initial). \(parts[1])"
    }

    private static func parseDayStamp(_ stamp: String, calendar: Calendar) -> Date? {
        let parts = stamp.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }
}

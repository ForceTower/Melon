#if os(watchOS)
import Foundation

enum WatchFormat {
    /// Whole calendar days from `now` to a "yyyy-MM-dd" stamp; nil when the
    /// stamp doesn't parse.
    static func daysUntil(stamp: String, now: Date, calendar: Calendar = .current) -> Int? {
        let parts = stamp.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3,
              let date = calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
        else { return nil }
        return calendar.dateComponents(
            [.day],
            from: calendar.startOfDay(for: now),
            to: calendar.startOfDay(for: date)
        ).day
    }

    /// How a grade row dates itself: released grades show the posted date
    /// ("12 mar"), pending ones how close they are ("em 5 dias").
    static func gradeDate(stamp: String?, now: Date, calendar: Calendar = .current) -> String? {
        guard let stamp else { return nil }
        guard let days = daysUntil(stamp: stamp, now: now, calendar: calendar), days > 0 else {
            return HomeFormat.shortDate(fromDayStamp: stamp, calendar: calendar)
        }
        switch days {
        case 1: return .localized(.watchDateTomorrow)
        default: return .localized(.watchInDayOther(days))
        }
    }

    /// "SEG" from a date.
    static func weekdayShort(_ date: Date, locale: Locale = .autoupdatingCurrent) -> String {
        date.formatted(.dateTime.weekday(.abbreviated).locale(locale))
            .replacingOccurrences(of: ".", with: "")
            .uppercased(with: locale)
    }

    /// "4 aulas" — bare-word plural, so two catalog keys.
    static func classCount(_ count: Int) -> String {
        count == 1 ? .localized(.watchClassCountOne(count)) : .localized(.watchClassCountOther(count))
    }

    /// "08:00" from minutes into the day.
    static func timeLabel(minutes: Int) -> String {
        String(format: "%02d:%02d", minutes / 60, minutes % 60)
    }

    /// Upstream weekday number (0 = Sunday) → its date in the week containing
    /// `now`, Monday-first like the Horário tab.
    static func weekDates(now: Date, calendar: Calendar = .current) -> [Int: Date] {
        let today = calendar.startOfDay(for: now)
        let weekday = calendar.component(.weekday, from: today) - 1
        guard let monday = calendar.date(byAdding: .day, value: -((weekday + 6) % 7), to: today)
        else { return [:] }
        var dates: [Int: Date] = [:]
        for day in 0..<7 {
            let mondayFirstOffset = (day + 6) % 7
            dates[day] = calendar.date(byAdding: .day, value: mondayFirstOffset, to: monday)
        }
        return dates
    }
}
#endif

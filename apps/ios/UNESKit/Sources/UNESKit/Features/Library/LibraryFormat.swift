import Foundation

enum LibraryFormat {
    /// Grouped integer in the current locale — "1.234" in pt-BR.
    static func count(_ value: Int) -> String {
        value.formatted(.number.grouping(.automatic))
    }

    /// Short day-month — "5 de ago." in pt-BR, "Aug 5" in en.
    static func shortDate(_ date: Date) -> String {
        date.formatted(.dateTime.day().month(.abbreviated))
    }

    static func year(_ date: Date) -> String {
        String(Calendar.current.component(.year, from: date))
    }

    /// "agora mesmo" under a minute, then minutes, then hours.
    static func ago(from checkedAt: Date, to now: Date) -> String {
        let minutes = Int(now.timeIntervalSince(checkedAt) / 60)
        if minutes < 1 { return .localized(.libraryAgoNow) }
        if minutes < 60 { return .localized(.libraryAgoMinutes(minutes)) }
        return .localized(.libraryAgoHours(minutes / 60))
    }
}

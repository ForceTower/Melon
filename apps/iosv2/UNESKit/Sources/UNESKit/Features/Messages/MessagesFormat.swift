import Foundation

// MARK: - Buckets

/// The inbox date sections, by whole calendar days — a message from
/// yesterday evening belongs to "Ontem" even when fewer than 24h have passed.
enum MessageBucket: CaseIterable, Equatable, Sendable {
    case today, yesterday, thisWeek, thisMonth, older

    var label: String {
        switch self {
        case .today: .localized(.commonToday)
        case .yesterday: .localized(.messagesBucketYesterday)
        case .thisWeek: .localized(.messagesBucketThisWeek)
        case .thisMonth: .localized(.messagesBucketThisMonth)
        case .older: .localized(.messagesBucketOlder)
        }
    }
}

// MARK: - Filters

enum MessageFilter: String, CaseIterable, Equatable, Sendable {
    case all, unread, starred, disciplines, university, app

    var label: String {
        switch self {
        case .all: .localized(.messagesFilterAll)
        case .unread: .localized(.messagesFilterUnread)
        case .starred: .localized(.messagesFilterStarred)
        case .disciplines: .localized(.messagesFilterDisciplines)
        case .university: .localized(.messagesFilterUniversity)
        case .app: .localized(.messagesFilterAppModules)
        }
    }

    func matches(_ message: MessageItem) -> Bool {
        switch self {
        case .all: true
        case .unread: message.unread
        case .starred: message.starred
        case .disciplines: message.category == .disciplines
        case .university: message.category == .university
        case .app: message.category == .app
        }
    }
}

extension MessageCategory {
    var label: String {
        switch self {
        case .disciplines: .localized(.messagesFilterDisciplines)
        case .university: .localized(.messagesFilterUniversity)
        case .app: .localized(.messagesFilterAppModules)
        }
    }
}

// MARK: - Display strings

enum MessagesFormat {
    private static let locale = Locale.autoupdatingCurrent

    static func bucket(for date: Date, now: Date, calendar: Calendar = .current) -> MessageBucket {
        switch daysAgo(date, now: now, calendar: calendar) {
        case ...0: .today
        case 1: .yesterday
        case ...7: .thisWeek
        case ...31: .thisMonth
        default: .older
        }
    }

    /// The row's time label: "agora", "12 min", "3h" (same day), "ontem",
    /// "4 d", then "10 abr".
    static func relativeTime(for date: Date, now: Date, calendar: Calendar = .current) -> String {
        let minutes = Int(max(0, now.timeIntervalSince(date)) / 60)
        switch daysAgo(date, now: now, calendar: calendar) {
        case ...0:
            if minutes < 1 { return .localized(.messagesTimeNow) }
            if minutes < 60 { return .localized(.messagesTimeMinutesAgo(minutes)) }
            return .localized(.messagesTimeHoursAgo(minutes / 60))
        case 1: return .localized(.messagesTimeYesterday)
        case let days where days <= 6: return .localized(.messagesTimeDaysAgo(days))
        default: return HomeFormat.shortDate(for: date)
        }
    }

    /// "18 de abril de 2026 · 09:14" — the detail timestamp.
    static func fullTimestamp(for date: Date) -> String {
        let day = date.formatted(.dateTime.day().month(.wide).year().locale(locale))
        let time = date.formatted(.dateTime.hour(.twoDigits(amPM: .omitted)).minute().locale(locale))
        return "\(day) · \(time)"
    }

    /// The secondary sender line, derived from the origin — upstream carries
    /// no role/title for senders.
    static func roleLine(_ message: MessageItem) -> String {
        switch message.origin {
        case .discipline: message.disciplineName ?? String.localized(.messagesOriginDiscipline)
        case .secretariat: .localized(.messagesRoleSecretariat)
        case .campus: .localized(.messagesRoleCampus)
        case .app: .localized(.messagesRoleApp)
        case .direct: .localized(.messagesRoleDirect)
        }
    }

    /// The detail eyebrow above the sender name.
    static func kindLabel(_ origin: MessageOrigin) -> String {
        switch origin {
        case .discipline: .localized(.messagesOriginDiscipline)
        case .secretariat: .localized(.messagesKindSecretariat)
        case .campus: .localized(.messagesFilterUniversity)
        case .app: .localized(.messagesKindApp)
        case .direct: .localized(.messagesKindDirect)
        }
    }

    /// The avatar monogram for origins that show text instead of a symbol.
    /// The secretariat/campus/app monograms are fixed institutional codes,
    /// stable across locales.
    static func badgeLabel(_ message: MessageItem) -> String {
        switch message.origin {
        case .discipline:
            message.disciplineCode
                ?? message.disciplineName.map { String($0.prefix(4)).uppercased() }
                ?? String.localized(.messagesBadgeClassFallback)
        case .secretariat: "SEC"
        case .campus: "UEFS"
        case .app: "UNES"
        case .direct: ""
        }
    }

    private static func daysAgo(_ date: Date, now: Date, calendar: Calendar) -> Int {
        calendar.dateComponents(
            [.day],
            from: calendar.startOfDay(for: date),
            to: calendar.startOfDay(for: now)
        ).day ?? 0
    }
}

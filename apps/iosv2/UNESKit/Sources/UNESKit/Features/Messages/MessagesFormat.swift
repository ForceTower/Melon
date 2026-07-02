import Foundation

// MARK: - Buckets

/// The inbox date sections, by whole calendar days — a message from
/// yesterday evening belongs to "Ontem" even when fewer than 24h have passed.
enum MessageBucket: CaseIterable, Equatable, Sendable {
    case today, yesterday, thisWeek, thisMonth, older

    var label: String {
        switch self {
        case .today: "Hoje"
        case .yesterday: "Ontem"
        case .thisWeek: "Esta semana"
        case .thisMonth: "Este mês"
        case .older: "Mais antigas"
        }
    }
}

// MARK: - Filters

enum MessageFilter: String, CaseIterable, Equatable, Sendable {
    case all, unread, starred, disciplines, university, app

    var label: String {
        switch self {
        case .all: "Todas"
        case .unread: "Não lidas"
        case .starred: "Salvas"
        case .disciplines: "Disciplinas"
        case .university: "Universidade"
        case .app: "App & módulos"
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
        case .disciplines: "Disciplinas"
        case .university: "Universidade"
        case .app: "App & módulos"
        }
    }
}

// MARK: - Display strings

enum MessagesFormat {
    private static let ptBR = Locale(identifier: "pt_BR")

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
            if minutes < 1 { return "agora" }
            if minutes < 60 { return "\(minutes) min" }
            return "\(minutes / 60)h"
        case 1: return "ontem"
        case let days where days <= 6: return "\(days) d"
        default: return HomeFormat.shortDate(for: date)
        }
    }

    /// "18 de abril de 2026 · 09:14" — the detail timestamp.
    static func fullTimestamp(for date: Date) -> String {
        let day = date.formatted(.dateTime.day().month(.wide).year().locale(ptBR))
        let time = date.formatted(.dateTime.hour(.twoDigits(amPM: .omitted)).minute().locale(ptBR))
        return "\(day) · \(time)"
    }

    /// The secondary sender line, derived from the origin — upstream carries
    /// no role/title for senders.
    static func roleLine(_ message: MessageItem) -> String {
        switch message.origin {
        case .discipline: message.disciplineName ?? "Disciplina"
        case .secretariat: "Secretaria Acadêmica"
        case .campus: "Comunicado oficial"
        case .app: "Equipe do app"
        case .direct: "Mensagem pessoal"
        }
    }

    /// The detail eyebrow above the sender name.
    static func kindLabel(_ origin: MessageOrigin) -> String {
        switch origin {
        case .discipline: "Disciplina"
        case .secretariat: "Secretaria"
        case .campus: "Universidade"
        case .app: "App"
        case .direct: "Pessoal · para você"
        }
    }

    /// The avatar monogram for origins that show text instead of a symbol.
    static func badgeLabel(_ message: MessageItem) -> String {
        switch message.origin {
        case .discipline:
            message.disciplineCode
                ?? message.disciplineName.map { String($0.prefix(4)).uppercased() }
                ?? "AULA"
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

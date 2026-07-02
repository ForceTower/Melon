import Foundation

/// pt-BR display strings for the Eu screen.
enum MeFormat {
    private static let ptBR = Locale(identifier: "pt_BR")

    /// "última: agora" / "última: há 2 min" — the Sincronização row hint.
    static func lastSyncHint(syncedAt: Date?, now: Date) -> String {
        guard let syncedAt else { return "aguardando a primeira sincronização" }
        let seconds = max(0, now.timeIntervalSince(syncedAt))
        switch seconds {
        case ..<90: return "última: agora"
        case ..<3600: return "última: há \(Int(seconds / 60)) min"
        case ..<86_400: return "última: há \(Int(seconds / 3600))h"
        default: return "última: há \(Int(seconds / 86_400))d"
        }
    }

    /// "versão 1.2 · build 34" — the Sobre row hint.
    static var versionHint: String {
        "versão \(marketingVersion) · build \(buildNumber)"
    }

    /// "UNES v1.2" — footers.
    static var versionLabel: String {
        "UNES v\(marketingVersion)"
    }

    private static var marketingVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "dev"
    }

    private static var buildNumber: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "0"
    }

    /// "7 semestres · 142 mensagens" — the preserved-data card.
    static func dataSummaryLabel(_ summary: LocalDataSummary) -> String {
        let semesters = summary.semesters == 1 ? "1 semestre" : "\(summary.semesters) semestres"
        let messages = summary.messages == 1 ? "1 mensagem" : "\(summary.messages) mensagens"
        return "\(semesters) · \(messages)"
    }

    // MARK: Calendar teaser rows

    /// "28" from a yyyy-MM-dd stamp.
    static func eventDay(_ stamp: String) -> String {
        guard let date = parse(stamp) else { return "–" }
        return date.formatted(.dateTime.day().locale(ptBR))
    }

    /// "abr" from a yyyy-MM-dd stamp — the view renders it uppercase.
    static func eventMonth(_ stamp: String) -> String {
        guard let date = parse(stamp) else { return "" }
        return date.formatted(.dateTime.month(.abbreviated).locale(ptBR))
            .replacingOccurrences(of: ".", with: "")
    }

    /// "ter" from a yyyy-MM-dd stamp.
    static func eventWeekday(_ stamp: String) -> String {
        guard let date = parse(stamp) else { return "" }
        return date.formatted(.dateTime.weekday(.abbreviated).locale(ptBR))
            .replacingOccurrences(of: ".", with: "")
            .lowercased()
    }

    private static func parse(_ stamp: String, calendar: Calendar = .current) -> Date? {
        let parts = stamp.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }
}

extension AcademicEvent.Origin {
    /// The pt-BR tag under a calendar teaser row.
    var label: String {
        switch self {
        case .evaluation: "prova"
        case .finalExam: "prova final"
        case .secondCall: "segunda chamada"
        case .secondEpoch: "segunda época"
        case .manual, .unknown: "evento"
        }
    }
}

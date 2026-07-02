import Foundation

/// pt-BR display strings for the Eu screen.
enum MeFormat {
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

    /// "UNES v1.2 · build 34" — the Configurações footer.
    static var versionBuildLabel: String {
        "\(versionLabel) · build \(buildNumber)"
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
}

import Foundation

/// Locale-aware display strings for the Eu screen.
enum MeFormat {
    /// "versão 1.2 · build 34" — the Sobre row hint.
    static var versionHint: String {
        .localized(.meAboutVersionHint(marketingVersion, buildNumber))
    }

    /// "UNES v1.2" — footers. Brand + version, identical across locales.
    static var versionLabel: String {
        "UNES v\(marketingVersion)"
    }

    /// "UNES v1.2 · build 34" — the Configurações footer.
    static var versionBuildLabel: String {
        .localized(.meAboutVersionBuild(versionLabel, buildNumber))
    }

    private static var marketingVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "dev"
    }

    private static var buildNumber: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "0"
    }
}

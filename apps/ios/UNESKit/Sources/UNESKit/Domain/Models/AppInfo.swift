import Foundation

/// Build / device metadata surfaced by the "Sobre o aplicativo" sheet.
struct AppInfo: Equatable, Sendable, Identifiable {
    let version: String
    let build: String
    /// MD5 hex digest of the installation UUID minted on first launch —
    /// the legacy apps' recipe: stable per install, safe to share in debug
    /// reports, and never the raw id.
    let machineId: String
    /// Hardware identifier, e.g. "iPhone15,3".
    let deviceModel: String
    /// "iOS 18.4" — system name plus version.
    let osVersion: String
    /// "estável" / "TestFlight" / "desenvolvimento".
    let channel: String
    /// "App Store" / "TestFlight" / "Xcode".
    let installSource: String

    /// The sheet is presented per install, so the machine id doubles as the
    /// sheet-item identity.
    var id: String { machineId }

    /// The plaintext block the sheet's copy button puts on the pasteboard.
    var debugText: String {
        """
        UNES — debug info
        versão     \(version) (\(channel))
        build      \(build)
        machine id \(machineId)
        aparelho   \(deviceModel) · \(osVersion)
        """
    }
}

extension AppInfo {
    static let preview = AppInfo(
        version: "4.2.1",
        build: "1842",
        machineId: "7c3a9f1eb204d8a65f4e0d2c9b817a36",
        deviceModel: "iPhone17,3",
        osVersion: "iOS 26.0",
        channel: "estável",
        installSource: "App Store"
    )
}

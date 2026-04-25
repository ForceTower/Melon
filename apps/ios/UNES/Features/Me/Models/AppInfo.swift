import SwiftUI

/// Snapshot of the build / device metadata surfaced in the "Sobre o aplicativo"
/// sheet. Mirrors the `ABOUT_INFO` block in `screens-me.jsx`, but every value
/// is read from the running device — there is no fixture path.
struct AppInfo {
    let version: String
    let build: String
    /// Vendor-scoped UUID. Stable per (app vendor, device) pair, safe to share
    /// in debug reports.
    let machineId: String
    /// Hardware identifier (`iPhone15,3`) plus iOS version, e.g.
    /// `"iPhone15,3 · iOS 17.4"`.
    let phoneModel: String
    /// Translated channel label shown next to the version row
    /// ("estável" / "TestFlight" / "desenvolvimento").
    let channel: String
    /// User-facing install source ("App Store" / "TestFlight" / "Xcode").
    let installSource: String

    static var current: AppInfo {
        let device = UIDevice.current
        return AppInfo(
            version: Bundle.main.appVersion,
            build: Bundle.main.buildNumber,
            machineId: device.identifierForVendor?.uuidString.lowercased() ?? "—",
            phoneModel: "\(modelIdentifier()) · iOS \(device.systemVersion)",
            channel: detectedChannel().channel,
            installSource: detectedChannel().installSource
        )
    }

    /// Multi-line plaintext copied to the pasteboard from the sheet's button.
    var debugText: String {
        """
        UNES — debug info
        versão     \(version)
        build      \(build)
        machine id \(machineId)
        aparelho   \(phoneModel)
        """
    }

    /// Hardware identifier from `uname` (e.g. `iPhone15,3`). On the Simulator,
    /// the host iOS reports the simulator host's machine; the
    /// `SIMULATOR_MODEL_IDENTIFIER` env var carries the simulated device.
    static func modelIdentifier() -> String {
        if let simulator = ProcessInfo.processInfo.environment["SIMULATOR_MODEL_IDENTIFIER"] {
            return simulator
        }
        var sysinfo = utsname()
        uname(&sysinfo)
        return String(bytes: Data(bytes: &sysinfo.machine, count: Int(_SYS_NAMELEN)), encoding: .ascii)?
            .trimmingCharacters(in: .controlCharacters) ?? "unknown"
    }

    private static func detectedChannel() -> (channel: String, installSource: String) {
        #if DEBUG
        return ("desenvolvimento", "Xcode")
        #else
        if Bundle.main.appStoreReceiptURL?.lastPathComponent == "sandboxReceipt" {
            return ("TestFlight", "TestFlight")
        }
        return ("estável", "App Store")
        #endif
    }
}

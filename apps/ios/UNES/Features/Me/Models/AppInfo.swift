import StoreKit
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
        let initial = initialChannel()
        return AppInfo(
            version: Bundle.main.appVersion,
            build: Bundle.main.buildNumber,
            machineId: device.identifierForVendor?.uuidString.lowercased() ?? "—",
            phoneModel: "\(modelIdentifier()) · iOS \(device.systemVersion)",
            channel: initial.channel,
            installSource: initial.installSource
        )
    }

    /// Returns a copy with `channel`/`installSource` refined via
    /// `AppTransaction.shared` so we can distinguish TestFlight from App Store
    /// without the (deprecated) `appStoreReceiptURL` heuristic. Falls back to
    /// the release defaults if the StoreKit receipt is unavailable.
    func resolved() async -> AppInfo {
        let resolved = await Self.resolvedChannel()
        return AppInfo(
            version: version,
            build: build,
            machineId: machineId,
            phoneModel: phoneModel,
            channel: resolved.channel,
            installSource: resolved.installSource
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

    /// Synchronous default used at `AppInfo.current` time. Release builds
    /// assume App Store; `resolved()` upgrades the label to TestFlight when
    /// the StoreKit environment reports a sandbox receipt.
    private static func initialChannel() -> (channel: String, installSource: String) {
        #if DEBUG
        return ("desenvolvimento", "Xcode")
        #else
        return ("estável", "App Store")
        #endif
    }

    private static func resolvedChannel() async -> (channel: String, installSource: String) {
        #if DEBUG
        return ("desenvolvimento", "Xcode")
        #else
        do {
            let result = try await AppTransaction.shared
            if case .verified(let transaction) = result,
               transaction.environment == .sandbox {
                return ("TestFlight", "TestFlight")
            }
        } catch {
            // Receipt unavailable — fall through to the release default.
        }
        return ("estável", "App Store")
        #endif
    }
}

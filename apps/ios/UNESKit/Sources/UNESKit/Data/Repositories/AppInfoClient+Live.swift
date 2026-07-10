import ComposableArchitecture
import Foundation
import StoreKit

private let log = Log.scoped("AppInfoClient")

extension AppInfoClient: DependencyKey {
    static let liveValue = AppInfoClient(
        current: { .live(channel: initialChannel) },
        resolved: {
            let channel = await resolvedChannel()
            log.debug("resolved channel=\(channel.channel)")
            return .live(channel: channel)
        }
    )

    private typealias Channel = (channel: String, installSource: String)

    /// Synchronous default: release builds assume App Store until
    /// `resolvedChannel` lands.
    private static var initialChannel: Channel {
        #if DEBUG
        ("desenvolvimento", "Xcode")
        #else
        (String.localized(.dataChannelStable), "App Store")
        #endif
    }

    /// Distinguishes TestFlight from the App Store through the StoreKit
    /// receipt; falls back to the release default when it is unavailable.
    private static func resolvedChannel() async -> Channel {
        #if DEBUG
        return ("desenvolvimento", "Xcode")
        #else
        if case .verified(let transaction) = try? await AppTransaction.shared,
           transaction.environment == .sandbox {
            return ("TestFlight", "TestFlight")
        }
        return (String.localized(.dataChannelStable), "App Store")
        #endif
    }
}

private extension AppInfo {
    static func live(channel: (channel: String, installSource: String)) -> AppInfo {
        AppInfo(
            version: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "dev",
            build: Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "0",
            machineId: MachineIdentity.id,
            deviceModel: modelIdentifier(),
            osVersion: osVersionLabel(),
            channel: channel.channel,
            installSource: channel.installSource
        )
    }

    /// Hardware identifier from `uname` (e.g. `iPhone15,3`). On the Simulator
    /// `uname` reports the host Mac; the env var carries the simulated device.
    private static func modelIdentifier() -> String {
        if let simulator = ProcessInfo.processInfo.environment["SIMULATOR_MODEL_IDENTIFIER"] {
            return simulator
        }
        var sysinfo = utsname()
        uname(&sysinfo)
        return String(bytes: Data(bytes: &sysinfo.machine, count: Int(_SYS_NAMELEN)), encoding: .ascii)?
            .trimmingCharacters(in: .controlCharacters) ?? "unknown"
    }

    private static func osVersionLabel() -> String {
        #if os(iOS)
        let name = "iOS"
        #else
        let name = "macOS"
        #endif
        let os = ProcessInfo.processInfo.operatingSystemVersion
        let patch = os.patchVersion > 0 ? ".\(os.patchVersion)" : ""
        return "\(name) \(os.majorVersion).\(os.minorVersion)\(patch)"
    }
}

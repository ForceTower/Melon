import FirebaseRemoteConfig
import Foundation
import UNESKit

/// The base layer, and the one being migrated away from: Remote Config is
/// becoming a paid feature, so lever sits in front of it (see
/// `CompositeRemoteSettings`) and this stays as the answer for every key lever
/// has not taken over yet.
///
/// `configValue(forKey:)` is total — an unpublished key reads as `false`/`""` —
/// which is what makes this a valid bottom of the stack. Between launches
/// Remote Config's own disk cache keeps the last activated values, so gates
/// hold their state offline.
///
/// Nothing is stored: `RemoteConfig.remoteConfig()` is the shared instance, and
/// reaching for it per call keeps this a `Sendable` value in a target that
/// compiles with strict concurrency.
nonisolated struct FirebaseRemoteSettings: RemoteSettings {
    private let log = Log.scoped("FirebaseRemoteSettings")

    func bool(_ key: RemoteBoolKey) -> Bool {
        RemoteConfig.remoteConfig().configValue(forKey: key.rawValue).boolValue
    }

    func string(_ key: RemoteStringKey) -> String {
        RemoteConfig.remoteConfig().configValue(forKey: key.rawValue).stringValue
    }

    /// Launch fetch for the baseline, then the real-time stream so console
    /// publishes land while the app is running.
    func start(onChange: @escaping @Sendable () -> Void) {
        let remoteConfig = RemoteConfig.remoteConfig()
        #if DEBUG
            // No 12h fetch cache while developing — flag flips apply right away.
            let settings = RemoteConfigSettings()
            settings.minimumFetchInterval = 0
            remoteConfig.configSettings = settings
        #endif

        // `RemoteConfig` is not `Sendable`, so the task reaches for the shared
        // instance itself rather than capturing this one across isolation.
        Task { [log] in
            _ = try? await RemoteConfig.remoteConfig().fetchAndActivate()
            log.debug("remote config baseline fetch+activate completed")
            onChange()
        }

        _ = remoteConfig.addOnConfigUpdateListener { [log] update, error in
            if let error {
                log.warn("remote config update stream error", error: error)
            }
            guard update != nil, error == nil else { return }
            log.debug("remote config update received -> republishing flags")
            RemoteConfig.remoteConfig().activate { _, _ in onChange() }
        }
    }
}

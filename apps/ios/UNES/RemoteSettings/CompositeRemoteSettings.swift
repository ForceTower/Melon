import Foundation
import Lever
import UNESKit

/// lever answers where it has an opinion; Firebase answers everywhere else.
///
/// That ordering is the migration: a key moves off Remote Config the moment it
/// is published in lever, one key at a time, with no client release — and
/// deleting it from lever hands it straight back. Nothing needs both sides to
/// agree, and nothing breaks while only one of them is populated.
nonisolated struct CompositeRemoteSettings: RemoteSettings {
    private let lever: LeverRemoteSettings
    private let firebase: FirebaseRemoteSettings

    init(lever: LeverRemoteSettings, firebase: FirebaseRemoteSettings) {
        self.lever = lever
        self.firebase = firebase
    }

    func bool(_ key: RemoteBoolKey) -> Bool {
        lever.lookup(key) ?? firebase.bool(key)
    }

    func string(_ key: RemoteStringKey) -> String {
        lever.lookup(key) ?? firebase.string(key)
    }

    /// Either layer changing can change what a read resolves to, so both are
    /// reasons to republish the flags.
    func start(onChange: @escaping @Sendable () -> Void) {
        lever.start(onChange: onChange)
        firebase.start(onChange: onChange)
    }
}

extension CompositeRemoteSettings {
    /// The production stack. The `pk_` key is a public client identifier by
    /// design: it authorizes reading one environment's resolved values, which
    /// every user of the app can see anyway. Never put a secret in a config
    /// value.
    static func live() -> CompositeRemoteSettings {
        var configuration = LeverConfiguration(
            baseURL: URL(string: "https://rc.forcetower.dev")!,
            clientKey: "pk_kwXERv2Rt6NjY6pd52A2gMG0SHJLJrlu",
            // Feeds the server's targeting rules — `platform` is filled in by
            // the SDK, so a parameter can be split ios-vs-android or rolled out
            // by version without a client release.
            context: LeverContext(
                appVersion: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString")
                    as? String
            )
        )
        // Pins the cache file's identity to a name we own, so rotating the
        // client key still lands on a warm cache.
        configuration.cacheNamespace = "prod"
        configuration.logSink = LeverLogBridge()
        #if DEBUG
            // Same rule the Firebase layer follows: no fetch cache while
            // developing, so a publish lands on the next launch.
            configuration.minimumFetchInterval = .zero
        #endif

        return CompositeRemoteSettings(
            // Constructing the client loads its cache synchronously, which is
            // what makes the first gate read after launch correct rather than
            // eventually correct.
            lever: LeverRemoteSettings(client: LeverClient(configuration: configuration)),
            firebase: FirebaseRemoteSettings()
        )
    }
}

/// Routes the SDK's own logs into the app's pipeline, so a lever fetch failure
/// lands beside everything else in `melon-iosv2` instead of only in the console.
private nonisolated struct LeverLogBridge: LeverLogSink {
    private let logger = Log.scoped("Lever")

    func log(_ level: LeverLogLevel, _ message: String) {
        switch level {
        case .debug: logger.debug(message)
        case .info: logger.info(message)
        case .warn: logger.warn(message)
        case .error: logger.error(message)
        }
    }
}

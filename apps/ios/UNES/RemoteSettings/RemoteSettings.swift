import Foundation
import Lever
import UNESKit

/// Our remote config (https://github.com/ForceTower/lever), and the only source
/// every gate resolves against.
///
/// Reads are synchronous and total: a key lever has not published resolves to
/// the type's empty value (`false` / `""`), which is the floor every gate is
/// written against — a feature stays off and the captcha pair stays empty until
/// something says otherwise. The client loads its cache when it is constructed
/// and keeps the last activated values on disk, so gates hold their state
/// offline and across launches.
nonisolated struct RemoteSettings: Sendable {
    private let client: LeverClient
    private let log = Log.scoped("RemoteSettings")

    init(client: LeverClient) {
        self.client = client
    }

    func bool(_ key: RemoteBoolKey) -> Bool {
        client.value(for: LeverKey(key.rawValue, default: false))
    }

    func string(_ key: RemoteStringKey) -> String {
        client.value(for: LeverKey(key.rawValue, default: ""))
    }

    /// The client fetches from the moment it is constructed, so there is
    /// nothing to kick off — only its activations to listen for. One stream
    /// carries the launch fetch, the foreground refresh, the polling floor, and
    /// the SSE nudge that lands a console publish while the app is open.
    ///
    /// `onChange` arrives off the main actor.
    func start(onChange: @escaping @Sendable () -> Void) {
        Task { [client, log] in
            // Reading `updates` registers the continuation, so taking it first
            // and reporting once covers anything that activated between the
            // client being constructed and this task running.
            let updates = client.updates
            onChange()
            for await update in updates {
                log.debug("lever activated version=\(update.version) changed=\(update.changedKeys.count)")
                onChange()
            }
        }
    }
}

extension RemoteSettings {
    /// The production client. The base URL is lever's API origin; the dashboard
    /// is a separate deployment at rc.forcetower.dev and is not what clients
    /// talk to.
    ///
    /// The `pk_` key is a public client identifier by design: it authorizes
    /// reading one environment's resolved values, which every user of the app
    /// can see anyway. Never put a secret in a config value.
    static func live() -> RemoteSettings {
        var configuration = LeverConfiguration(
            baseURL: URL(string: "https://rc-api.forcetower.dev")!,
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
            // No fetch cache while developing, so a publish lands on the next
            // launch.
            configuration.minimumFetchInterval = .zero
        #endif

        // Constructing the client loads its cache synchronously, which is what
        // makes the first gate read after launch correct rather than
        // eventually correct.
        return RemoteSettings(client: LeverClient(configuration: configuration))
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

/// Parameter names are the keys shared with Android. Both platforms resolve
/// them from the same lever environment, so a gate that should differ between
/// the two is a platform condition on the parameter — not a second key.
nonisolated enum RemoteBoolKey: String {
    case enrollment = "enable_enrollment"
    case enrollmentCertificate = "enable_enrollment_certificate"
    case academicHistory = "enable_academic_history"
    case paradoxo = "enable_paradoxo"
    case materials = "enable_materials"
    case library = "enable_library"
    case campusEvent = "enable_campus_event"
    case evaluationReminders = "enable_evaluation_reminders"
    case retrospective = "enable_retrospective"
    case courseProgress = "enable_course_progress"
}

nonisolated enum RemoteStringKey: String {
    case documentCaptchaSiteKey = "document_captcha_site_key"
    case documentCaptchaBaseURL = "document_captcha_base_url"
}

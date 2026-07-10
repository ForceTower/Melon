import Foundation

/// Global, DI-free logging entry point. Types hold a tag-scoped logger and
/// log through it:
///
///     private let log = Log.scoped("HomeFeature")
///     log.info("mirror hydrated")
///     log.error("refresh failed", error: error)
///
/// Every record goes to OSLog (Xcode console / Console.app). Records at
/// `info` and above are also batched and shipped to the Melon API's
/// `/api/logs`, which re-emits them to the OTel backend — the same sink the
/// v1 app feeds as `melon-ios`; this app shows up as `melon-iosv2`.
/// Previews and test runs log locally but never ship.
public enum Log {
    /// Tag-bound logger for the caller. Store it as a member of the type
    /// that owns the tag so call sites don't repeat it.
    public static func scoped(_ tag: StaticString) -> ScopedLog {
        ScopedLog(tag: String(describing: tag))
    }
}

/// A tag-bound view over the shared log pipeline. Obtain via `Log.scoped(_:)`.
public struct ScopedLog: Sendable {
    let tag: String

    public func debug(_ message: String) {
        LogPipeline.shared.emit(.debug, tag: tag, message: message, error: nil)
    }

    public func info(_ message: String) {
        LogPipeline.shared.emit(.info, tag: tag, message: message, error: nil)
    }

    public func warn(_ message: String, error: (any Error)? = nil) {
        LogPipeline.shared.emit(.warn, tag: tag, message: message, error: error)
    }

    public func error(_ message: String, error: (any Error)? = nil) {
        LogPipeline.shared.emit(.error, tag: tag, message: message, error: error)
    }
}

enum LogLevel: Int, Comparable, Sendable {
    case debug
    case info
    case warn
    case error

    static func < (lhs: Self, rhs: Self) -> Bool { lhs.rawValue < rhs.rawValue }
}

struct LogPipeline: Sendable {
    static let shared = LogPipeline.live()

    let local: OSLogSink
    let remote: RemoteLogSink?
    /// Records below this level never leave the device.
    let remoteFloor: LogLevel

    func emit(_ level: LogLevel, tag: String, message: String, error: (any Error)?) {
        local.write(level, tag: tag, message: message, error: error)
        if level >= remoteFloor, let remote {
            remote.enqueue(level, tag: tag, message: message, error: error)
        }
    }

    private static func live() -> LogPipeline {
        let env = ProcessInfo.processInfo.environment
        let isPreview = env["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
        let isTest = env["XCTestConfigurationFilePath"] != nil || env["XCTestSessionIdentifier"] != nil
        return LogPipeline(
            local: OSLogSink(),
            remote: isPreview || isTest ? nil : RemoteLogSink(),
            remoteFloor: .info
        )
    }
}

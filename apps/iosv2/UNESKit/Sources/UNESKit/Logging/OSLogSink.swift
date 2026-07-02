import Foundation
import os

/// Local sink — one cached `os.Logger` per tag (the OSLog category), all
/// under the process bundle identifier so Console.app groups the app's
/// records together.
final class OSLogSink: Sendable {
    private let subsystem = Bundle.main.bundleIdentifier ?? "dev.forcetower.unes"
    private let loggers = OSAllocatedUnfairLock<[String: os.Logger]>(initialState: [:])

    func write(_ level: LogLevel, tag: String, message: String, error: (any Error)?) {
        let suffix = error.map { " — \(String(reflecting: type(of: $0))): \($0)" } ?? ""
        logger(for: tag).log(level: level.osLogType, "\(message, privacy: .public)\(suffix, privacy: .public)")
    }

    private func logger(for tag: String) -> os.Logger {
        loggers.withLock { cache in
            if let cached = cache[tag] { return cached }
            let logger = os.Logger(subsystem: subsystem, category: tag)
            cache[tag] = logger
            return logger
        }
    }
}

private extension LogLevel {
    var osLogType: OSLogType {
        switch self {
        case .debug: .debug
        case .info: .info
        case .warn: .default
        case .error: .error
        }
    }
}

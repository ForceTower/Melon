//
//  Log.swift
//  UNES
//
//  Global, DI-free entry point for logging. `AppDelegate` calls
//  `Log.bootstrap(_:)` once with the Kermit-backed logger from the Umbrella
//  graph; everything else just says:
//
//      private let log = Log.scoped("MyViewModel")
//
//  Scoped loggers hold a proxy that resolves the live logger at each call,
//  so it's safe to capture one as a `static let` — even before bootstrap,
//  pre-bootstrap calls fall through to `PrintAppLogger` (stdout) instead of
//  crashing. That matters for SwiftUI previews and early init.
//

import Foundation
import os

public nonisolated enum Log {
    fileprivate static let storage = LoggerStorage()

    /// Install the real logger. Call once from `AppDelegate` after the
    /// Umbrella graph has produced a Kermit-backed `AppLogger`.
    public static func bootstrap(_ logger: AppLogger) {
        storage.set(logger)
    }

    /// Tag-scoped logger for the caller. Store as an instance or static
    /// member and log without repeating the tag:
    ///
    ///     private let log = Log.scoped("SyncVM")
    ///     log.warn("token refresh failed", error: error)
    public static func scoped(_ tag: StaticString) -> ScopedAppLogger {
        ScopedAppLogger(base: ProxyAppLogger(storage: storage), tag: tag)
    }

    /// Raw, tagless accessor. Prefer `scoped(_:)`; this exists for code that
    /// genuinely needs to forward the `AppLogger` itself (e.g. injecting it
    /// into a type that already owns its tagging).
    public static var current: AppLogger {
        ProxyAppLogger(storage: storage)
    }
}

nonisolated final class LoggerStorage: Sendable {
    private let lock = OSAllocatedUnfairLock<AppLogger>(initialState: PrintAppLogger())

    func set(_ logger: AppLogger) {
        lock.withLock { $0 = logger }
    }

    func current() -> AppLogger {
        lock.withLock { $0 }
    }
}

/// Resolves to the currently-installed logger on every call, so scoped
/// loggers captured before `Log.bootstrap` still route correctly once the
/// real logger is installed.
nonisolated struct ProxyAppLogger: AppLogger {
    let storage: LoggerStorage

    func debug(_ tag: StaticString, _ message: String) {
        storage.current().debug(tag, message)
    }
    func info(_ tag: StaticString, _ message: String) {
        storage.current().info(tag, message)
    }
    func warn(_ tag: StaticString, _ message: String, error: Error?) {
        storage.current().warn(tag, message, error: error)
    }
    func error(_ tag: StaticString, _ message: String, error: Error?) {
        storage.current().error(tag, message, error: error)
    }
}

/// Fallback used before `Log.bootstrap` installs the real logger. Keeps
/// previews and early-init logs visible without dragging in the Umbrella
/// graph.
nonisolated struct PrintAppLogger: AppLogger {
    func debug(_ tag: StaticString, _ message: String) { emit("DEBUG", tag, message, error: nil) }
    func info(_ tag: StaticString, _ message: String) { emit("INFO", tag, message, error: nil) }
    func warn(_ tag: StaticString, _ message: String, error: Error?) { emit("WARN", tag, message, error: error) }
    func error(_ tag: StaticString, _ message: String, error: Error?) { emit("ERROR", tag, message, error: error) }

    private func emit(_ level: String, _ tag: StaticString, _ message: String, error: Error?) {
        let suffix = error.map { " — \(String(reflecting: type(of: $0))): \($0.localizedDescription)" } ?? ""
        print("[\(level)] [\(tag)] \(message)\(suffix)")
    }
}

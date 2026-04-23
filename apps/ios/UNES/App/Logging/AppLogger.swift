//
//  AppLogger.swift
//  UNES
//
//  Swift-native logging facade in front of the KMP-owned Kermit logger.
//  Feature code depends on this protocol, not on the Kotlin `Logger` class,
//  so the shared module can freely use Kermit without leaking a
//  non-Sendable type across Swift 6 strict-concurrency boundaries.
//

import Foundation

public nonisolated protocol AppLogger: Sendable {
    func debug(_ tag: StaticString, _ message: String)
    func info(_ tag: StaticString, _ message: String)
    func warn(_ tag: StaticString, _ message: String, error: Error?)
    func error(_ tag: StaticString, _ message: String, error: Error?)
}

public extension AppLogger {
    nonisolated func warn(_ tag: StaticString, _ message: String) { warn(tag, message, error: nil) }
    nonisolated func error(_ tag: StaticString, _ message: String) { error(tag, message, error: nil) }

    /// Binds this logger to a fixed tag so call sites don't have to repeat it.
    /// Store the returned value as a `let` on the type that owns the tag:
    ///
    ///     private let log = Log.scoped("DisciplinesListVM")
    ///     log.info("loaded \(count) disciplines")
    nonisolated func scoped(_ tag: StaticString) -> ScopedAppLogger {
        ScopedAppLogger(base: self, tag: tag)
    }
}

/// A tag-bound view over an `AppLogger`. Obtain one via `Log.scoped(_:)` or
/// `AppLogger.scoped(_:)`; forwards every call to its base logger with the
/// captured tag applied.
public nonisolated struct ScopedAppLogger: Sendable {
    private let base: AppLogger
    private let tag: StaticString

    public init(base: AppLogger, tag: StaticString) {
        self.base = base
        self.tag = tag
    }

    public func debug(_ message: String) { base.debug(tag, message) }
    public func info(_ message: String) { base.info(tag, message) }
    public func warn(_ message: String, error: Error? = nil) { base.warn(tag, message, error: error) }
    public func error(_ message: String, error: Error? = nil) { base.error(tag, message, error: error) }
}

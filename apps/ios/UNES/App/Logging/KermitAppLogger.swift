//
//  KermitAppLogger.swift
//  UNES
//
//  Production AppLogger backed by the Kermit Logger exposed on UmbrellaGraph.
//
//  Why `@unchecked Sendable`:
//  Kermit's `Logger` is a Kotlin class (ergo non-Sendable under strict
//  concurrency) but its `log*` methods are safe to call from any thread —
//  each underlying LogWriter handles its own thread-safety (OSLog is
//  thread-safe; our ApiLogWriter funnels through a Channel; the
//  Crashlytics writer forwards to FIRCrashlytics which is documented
//  thread-safe). Locking here would add contention for zero correctness
//  benefit, so we bypass the checker explicitly and keep this opt-out
//  confined to this file.
//
//  Swift `Error`s can't cheaply become Kotlin `Throwable`s, so for warn/error
//  we fold the localized description into the message. Crash reporting for
//  actual Kotlin exceptions happens on the Kotlin side (Crashlytics writer
//  in shared-kmp/core/logging).
//

import Foundation
@preconcurrency import Umbrella

// SKIE exports Kermit's `co.touchlab.kermit.Logger` as `KermitLogger` so
// there's no clash with Apple's `os.Logger`. The throwable parameter is
// `KotlinThrowable?` — plain `nil` needs an explicit type.
private nonisolated let noThrowable: KotlinThrowable? = nil

public nonisolated final class KermitAppLogger: AppLogger, @unchecked Sendable {
    private let logger: KermitLogger

    public init(logger: KermitLogger) {
        self.logger = logger
    }

    public func debug(_ tag: StaticString, _ message: String) {
        logger.d(messageString: message, throwable: noThrowable, tag: String(describing: tag))
    }

    public func info(_ tag: StaticString, _ message: String) {
        logger.i(messageString: message, throwable: noThrowable, tag: String(describing: tag))
    }

    public func warn(_ tag: StaticString, _ message: String, error: Error?) {
        logger.w(messageString: format(message, error: error), throwable: noThrowable, tag: String(describing: tag))
    }

    public func error(_ tag: StaticString, _ message: String, error: Error?) {
        logger.e(messageString: format(message, error: error), throwable: noThrowable, tag: String(describing: tag))
    }

    private func format(_ message: String, error: Error?) -> String {
        guard let error else { return message }
        return "\(message) — \(String(reflecting: type(of: error))): \(error.localizedDescription)"
    }
}

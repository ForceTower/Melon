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

public protocol AppLogger: Sendable {
    func debug(_ tag: StaticString, _ message: String)
    func info(_ tag: StaticString, _ message: String)
    func warn(_ tag: StaticString, _ message: String, error: Error?)
    func error(_ tag: StaticString, _ message: String, error: Error?)
}

public extension AppLogger {
    func warn(_ tag: StaticString, _ message: String) { warn(tag, message, error: nil) }
    func error(_ tag: StaticString, _ message: String) { error(tag, message, error: nil) }
}

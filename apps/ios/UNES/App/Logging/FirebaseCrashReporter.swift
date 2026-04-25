import FirebaseCrashlytics
import Foundation
@preconcurrency import Umbrella

// Host-side implementation of the KMP CrashReporter interface. The umbrella
// framework no longer links FirebaseCrashlytics symbols — it just hands
// breadcrumbs/non-fatals to whatever reporter the host provides.
//
// `nonisolated` is load-bearing: the target sets
// SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor, so without it these methods
// would implicitly be @MainActor. Kotlin invokes the writer on whichever
// thread emitted the log (including Firebase's own init dispatch queue) —
// an isolation mismatch trips _dispatch_assert_queue_fail and SIGTRAPs at
// launch. Firebase Crashlytics' methods are documented thread-safe, so
// nonisolated is the correct annotation; we also opt out of the Sendable
// checker since the class holds no mutable state worth locking around.
public nonisolated final class FirebaseCrashReporter: NSObject, LoggingCrashReporter, @unchecked Sendable {
    public func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    public func recordNonFatal(message: String, throwable: KotlinThrowable?) {
        var userInfo: [String: Any] = [NSLocalizedDescriptionKey: message]
        let domain: String
        if let throwable {
            domain = String(describing: type(of: throwable))
            if let m = throwable.message { userInfo["KotlinMessage"] = m }
            let stack = throwable.getStackTrace()
            if stack.size > 0 {
                var lines: [String] = []
                lines.reserveCapacity(Int(stack.size))
                for i in 0..<stack.size {
                    if let line = stack.get(index: i) as? String {
                        lines.append(line)
                    }
                }
                userInfo["KotlinStackTrace"] = lines.joined(separator: "\n")
            }
        } else {
            domain = "KotlinNonFatal"
        }
        Crashlytics.crashlytics().record(error: NSError(domain: domain, code: 0, userInfo: userInfo))
    }
}

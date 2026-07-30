import ComposableArchitecture
import Foundation

/// The key backing `sessionInvalid`. Features observe it as
/// `@Shared(.appStorage(SessionInvalidation.storageKey))`; the token refresher
/// writes it from outside the reducer world, so the write side is a client.
/// No periods — `@Shared(.appStorage)` falls back to notification-center
/// observation for keys key-value observing can't handle.
extension SessionInvalidation {
    static let storageKey = "session_invalid"
}

/// Latch raised when `api/auth/token/refresh` terminally fails — the session is
/// unrecoverable without a fresh login, but the local mirror stays intact and
/// the app keeps working on cached data.
@DependencyClient
struct SessionInvalidation: Sendable {
    var markInvalid: @Sendable () -> Void
    var clear: @Sendable () -> Void
}

extension SessionInvalidation: DependencyKey {
    static let liveValue = SessionInvalidation(
        markInvalid: { UserDefaults.standard.set(true, forKey: storageKey) },
        clear: { UserDefaults.standard.set(false, forKey: storageKey) }
    )

    static let testValue = SessionInvalidation()

    static let previewValue = SessionInvalidation(markInvalid: {}, clear: {})
}

extension DependencyValues {
    var sessionInvalidation: SessionInvalidation {
        get { self[SessionInvalidation.self] }
        set { self[SessionInvalidation.self] = newValue }
    }
}

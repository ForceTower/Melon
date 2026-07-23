import ComposableArchitecture

/// Device-side push plumbing: the system permission prompt, the FCM
/// registration-token registration against apps/api, and the fan-out of data
/// notifications to reducers. The token and notifications are delivered by
/// Firebase/iOS to the app delegate, which hands them over through
/// `PushTokens`/`PushEvents`.
@DependencyClient
struct PushClient: Sendable {
    /// Idempotent — iOS only ever shows the prompt once.
    var requestAuthorization: @Sendable () async -> Void
    /// App-delegate hand-off: persist the FCM registration token — refreshed
    /// by Firebase on every launch — and reconcile right away when a session
    /// already exists (fresh installs are registered by the onboarding sync
    /// step instead).
    var fcmTokenReceived: @Sendable (_ token: String) async -> Void
    /// Register the token and delete any stale identifier row still on the
    /// backend. Runs post-login, on token hand-offs, and on every return to
    /// the foreground.
    var reconcile: @Sendable () async -> Void
    /// Logout teardown: unmap every identifier this device registered, then
    /// drop the local caches. Must run while the session bearer is valid.
    var unregister: @Sendable () async -> Void
    /// App-delegate hand-off: the data payload of a notification presented
    /// while the app was in the foreground. Payloads carrying a `kind`
    /// discriminator surface on `dataEvents`; the rest are display-only.
    var dataNotificationReceived: @Sendable (_ data: [String: String]) async -> Void
    /// Data pushes for reducers to react to (refresh the mirror). Each
    /// subscription only sees pushes that arrive after it starts.
    var dataEvents: @Sendable () -> AsyncStream<PushDataEvent> = { .finished }
}

/// A push whose `data` payload carries a `kind` discriminator — the backend's
/// signal that mirror-visible data changed upstream (new message, grade,
/// lecture, …).
struct PushDataEvent: Equatable, Sendable {
    let kind: String
}

extension PushClient: TestDependencyKey {
    static let testValue = PushClient()

    static let previewValue = PushClient(
        requestAuthorization: {},
        fcmTokenReceived: { _ in },
        reconcile: {},
        unregister: {},
        dataNotificationReceived: { _ in },
        dataEvents: { .finished }
    )
}

extension DependencyValues {
    var push: PushClient {
        get { self[PushClient.self] }
        set { self[PushClient.self] = newValue }
    }
}

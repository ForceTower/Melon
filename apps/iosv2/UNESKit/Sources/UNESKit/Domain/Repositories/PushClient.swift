import ComposableArchitecture

/// Device-side push plumbing: the system permission prompt, the FCM-token
/// registration against apps/api, and the fan-out of data notifications to
/// reducers. Tokens and notifications are delivered by Firebase/iOS to the
/// app delegate, which hands them over through `PushTokens`/`PushEvents`.
@DependencyClient
struct PushClient: Sendable {
    /// Idempotent — iOS only ever shows the prompt once.
    var requestAuthorization: @Sendable () async -> Void
    /// App-delegate hand-off: persist the token and register it right away
    /// when a session already exists (fresh installs are registered by the
    /// onboarding sync step instead).
    var tokenReceived: @Sendable (_ token: String) async -> Void
    /// Post-login: register the token FCM minted before the session existed.
    var registerStoredToken: @Sendable () async -> Void
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
        tokenReceived: { _ in },
        registerStoredToken: {},
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

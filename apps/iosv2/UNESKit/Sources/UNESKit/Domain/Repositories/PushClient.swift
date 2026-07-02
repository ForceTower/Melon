import ComposableArchitecture

/// Device-side push plumbing: the system permission prompt and the FCM-token
/// registration against apps/api. The token itself is minted by Firebase in
/// the app delegate, which hands it over through `PushTokens`.
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
}

extension PushClient: TestDependencyKey {
    static let testValue = PushClient()

    static let previewValue = PushClient(
        requestAuthorization: {},
        tokenReceived: { _ in },
        registerStoredToken: {}
    )
}

extension DependencyValues {
    var push: PushClient {
        get { self[PushClient.self] }
        set { self[PushClient.self] = newValue }
    }
}

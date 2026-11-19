import ComposableArchitecture

/// Persisted auth session. Backed by the Keychain in the live implementation.
@DependencyClient
struct SessionStore: Sendable {
    var current: @Sendable () -> Session? = { nil }
    var save: @Sendable (Session) throws -> Void
    /// Also forgets the upstream credentials.
    var clear: @Sendable () throws -> Void
    /// The SAGRES login typed here, in its own item so writes never race token refreshes.
    var upstreamCredentials: @Sendable () -> AccountCredentials? = { nil }
    /// Best effort: the vault in Settings is the only reader.
    var saveUpstreamCredentials: @Sendable (AccountCredentials) -> Void
}

extension SessionStore: TestDependencyKey {
    static let testValue = SessionStore()

    static var previewValue: SessionStore { .inMemory() }

    /// Non-persistent store for previews and tests.
    static func inMemory(initial: Session? = nil) -> SessionStore {
        let session = LockIsolated<Session?>(initial)
        let credentials = LockIsolated<AccountCredentials?>(nil)
        return SessionStore(
            current: { session.value },
            save: { new in session.setValue(new) },
            clear: {
                session.setValue(nil)
                credentials.setValue(nil)
            },
            upstreamCredentials: { credentials.value },
            saveUpstreamCredentials: { new in credentials.setValue(new) }
        )
    }
}

extension DependencyValues {
    var sessionStore: SessionStore {
        get { self[SessionStore.self] }
        set { self[SessionStore.self] = newValue }
    }
}

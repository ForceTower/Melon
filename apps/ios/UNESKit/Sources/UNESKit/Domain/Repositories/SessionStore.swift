import ComposableArchitecture

/// Persisted auth session. Backed by the Keychain in the live implementation.
@DependencyClient
struct SessionStore: Sendable {
    var current: @Sendable () -> Session? = { nil }
    var save: @Sendable (Session) throws -> Void
    var clear: @Sendable () throws -> Void
}

extension SessionStore: TestDependencyKey {
    static let testValue = SessionStore()

    static var previewValue: SessionStore { .inMemory() }

    /// Non-persistent store for previews and tests.
    static func inMemory(initial: Session? = nil) -> SessionStore {
        let session = LockIsolated<Session?>(initial)
        return SessionStore(
            current: { session.value },
            save: { new in session.setValue(new) },
            clear: { session.setValue(nil) }
        )
    }
}

extension DependencyValues {
    var sessionStore: SessionStore {
        get { self[SessionStore.self] }
        set { self[SessionStore.self] = newValue }
    }
}

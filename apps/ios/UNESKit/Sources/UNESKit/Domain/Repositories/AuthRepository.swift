import ComposableArchitecture

/// Login against apps/api with SAGRES credentials or a passkey.
/// Successful logins persist the session in the `SessionStore`.
@DependencyClient
struct AuthRepository: Sendable {
    var login: @Sendable (_ username: String, _ password: String) async throws -> Session
    var beginPasskeyLogin: @Sendable () async throws -> PasskeyChallenge
    var completePasskeyLogin: @Sendable (_ sessionId: String, _ assertion: PasskeyAssertion) async throws -> Session
}

extension AuthRepository: TestDependencyKey {
    static let testValue = AuthRepository()

    static let previewValue = AuthRepository(
        login: { _, _ in .preview },
        beginPasskeyLogin: {
            PasskeyChallenge(sessionId: "preview", challenge: "", rpId: "melon.forcetower.dev", allowedCredentialIds: [])
        },
        completePasskeyLogin: { _, _ in .preview }
    )
}

extension DependencyValues {
    var authRepository: AuthRepository {
        get { self[AuthRepository.self] }
        set { self[AuthRepository.self] = newValue }
    }
}

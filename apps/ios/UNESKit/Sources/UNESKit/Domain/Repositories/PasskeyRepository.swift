import ComposableArchitecture

/// Manages the account's passkeys against apps/api: lists them, fetches
/// registration options, and enrolls a freshly minted credential.
@DependencyClient
struct PasskeyRepository: Sendable {
    var credentials: @Sendable () async throws -> [PasskeyCredential]
    var registrationOptions: @Sendable () async throws -> PasskeyRegistrationOptions
    var register: @Sendable (_ attestation: PasskeyAttestation, _ deviceName: String?) async throws -> Void
    var rename: @Sendable (_ id: String, _ deviceName: String) async throws -> Void
    var delete: @Sendable (_ id: String) async throws -> Void
}

extension PasskeyRepository: TestDependencyKey {
    static let testValue = PasskeyRepository()
    static let previewValue = PasskeyRepository(
        credentials: { [.preview] },
        registrationOptions: {
            PasskeyRegistrationOptions(
                challenge: "",
                rpId: "unes.forcetower.dev",
                userId: "",
                userName: "mariana.nogueira",
                userDisplayName: "Mariana Nogueira",
                excludedCredentialIds: [],
                algorithms: [-7, -257]
            )
        },
        register: { _, _ in },
        rename: { _, _ in },
        delete: { _ in }
    )
}

extension DependencyValues {
    var passkeyRepository: PasskeyRepository {
        get { self[PasskeyRepository.self] }
        set { self[PasskeyRepository.self] = newValue }
    }
}

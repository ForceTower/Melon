import ComposableArchitecture

/// Platform authenticator: signs a WebAuthn challenge with the device passkey
/// (`assert`) and enrolls a new one (`register`). Both surface the system's
/// own passkey sheet — Face ID / Touch ID and key selection are handled there.
@DependencyClient
struct PasskeyClient: Sendable {
    var assert: @Sendable (PasskeyChallenge) async throws -> PasskeyAssertion
    var register: @Sendable (_ options: PasskeyRegistrationOptions, _ target: PasskeyTarget) async throws -> PasskeyAttestation
}

extension PasskeyClient: TestDependencyKey {
    static let testValue = PasskeyClient()
}

extension DependencyValues {
    var passkeyClient: PasskeyClient {
        get { self[PasskeyClient.self] }
        set { self[PasskeyClient.self] = newValue }
    }
}

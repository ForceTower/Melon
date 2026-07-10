import ComposableArchitecture

/// Platform authenticator: signs a WebAuthn challenge with the device passkey.
@DependencyClient
struct PasskeyClient: Sendable {
    var assert: @Sendable (PasskeyChallenge) async throws -> PasskeyAssertion
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

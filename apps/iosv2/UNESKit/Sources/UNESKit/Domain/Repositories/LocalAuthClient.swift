import ComposableArchitecture

/// Biometric / passcode gate — stands in front of the stored password.
@DependencyClient
struct LocalAuthClient: Sendable {
    var authenticate: @Sendable (_ reason: String) async throws -> Bool
}

extension LocalAuthClient: TestDependencyKey {
    static let testValue = LocalAuthClient()
    static let previewValue = LocalAuthClient(authenticate: { _ in true })
}

extension DependencyValues {
    var localAuth: LocalAuthClient {
        get { self[LocalAuthClient.self] }
        set { self[LocalAuthClient.self] = newValue }
    }
}

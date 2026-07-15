import ComposableArchitecture
import Testing

@testable import UNESKit

@MainActor
struct LoginFeatureTests {
    @Test
    func credentialsLoginSucceeds() async {
        let store = TestStore(initialState: LoginFeature.State()) {
            LoginFeature()
        } withDependencies: {
            $0.authRepository.login = { @Sendable _, _ in .preview }
        }

        await store.send(.binding(.set(\.username, "mariana.souza"))) {
            $0.username = "mariana.souza"
        }
        await store.send(.binding(.set(\.password, "hunter2"))) {
            $0.password = "hunter2"
        }

        await store.send(.submitTapped) {
            $0.isLoading = true
        }
        await store.receive(.loginResponse(.success(.preview))) {
            $0.isLoading = false
        }
        await store.receive(.delegate(.loggedIn(username: "mariana.souza", session: .preview)))
    }

    @Test
    func invalidCredentialsShowError() async {
        let store = TestStore(
            initialState: LoginFeature.State(username: "mariana.souza", password: "wrong")
        ) {
            LoginFeature()
        } withDependencies: {
            $0.authRepository.login = { @Sendable _, _ in throw AuthError.invalidCredentials }
        }

        await store.send(.submitTapped) {
            $0.isLoading = true
        }
        await store.receive(.loginResponse(.failure(.invalidCredentials))) {
            $0.isLoading = false
            $0.errorMessage = AuthError.invalidCredentials.message
        }
    }

    @Test
    func passkeyLoginSucceedsWithoutUsername() async {
        let challenge = PasskeyChallenge(
            sessionId: "session-1",
            challenge: "Y2hhbGxlbmdl",
            rpId: "unes.forcetower.dev",
            allowedCredentialIds: []
        )
        let assertion = PasskeyAssertion(
            id: "cred",
            rawId: "cred",
            authenticatorAttachment: "platform",
            clientDataJSON: "data",
            authenticatorData: "auth",
            signature: "sig",
            userHandle: nil
        )

        let store = TestStore(initialState: LoginFeature.State()) {
            LoginFeature()
        } withDependencies: {
            $0.authRepository.beginPasskeyLogin = { @Sendable in challenge }
            $0.passkeyClient.assert = { @Sendable _ in assertion }
            $0.authRepository.completePasskeyLogin = { @Sendable sessionId, _ in
                #expect(sessionId == "session-1")
                return .preview
            }
        }

        await store.send(.passkeyTapped) {
            $0.isLoading = true
        }
        await store.receive(.passkeyResponse(.success(.preview))) {
            $0.isLoading = false
        }
        await store.receive(.delegate(.loggedIn(username: nil, session: .preview)))
    }

    @Test
    func cancelledPasskeyShowsNoError() async {
        let store = TestStore(initialState: LoginFeature.State()) {
            LoginFeature()
        } withDependencies: {
            $0.authRepository.beginPasskeyLogin = { @Sendable in throw AuthError.cancelled }
        }

        await store.send(.passkeyTapped) {
            $0.isLoading = true
        }
        await store.receive(.passkeyResponse(.failure(.cancelled))) {
            $0.isLoading = false
        }
    }
}

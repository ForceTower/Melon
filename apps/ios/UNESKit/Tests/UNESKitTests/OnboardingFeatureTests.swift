import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct OnboardingFeatureTests {
    @Test
    func splashAdvancesToWelcome() async {
        let clock = TestClock()
        let store = TestStore(initialState: OnboardingFeature.State()) {
            OnboardingFeature()
        } withDependencies: {
            $0.continuousClock = clock
        }

        await store.send(.task)
        await clock.advance(by: .seconds(2.6))
        await store.receive(.splashFinished) {
            $0.splash = false
        }
    }

    @Test
    func flowPushesThroughLoginSyncAndReady() async {
        let store = TestStore(initialState: OnboardingFeature.State(splash: false)) {
            OnboardingFeature()
        }

        await store.send(.exploreTapped) {
            $0.path[id: 0] = .intro(IntroFeature.State())
        }

        await store.send(.path(.element(id: 0, action: .intro(.delegate(.login))))) {
            $0.path[id: 1] = .login(LoginFeature.State())
        }

        await store.send(
            .path(.element(id: 1, action: .login(.delegate(.loggedIn(username: "mariana.souza", session: .preview)))))
        ) {
            $0.session = .preview
            $0.path[id: 2] = .sync(SyncFeature.State(greeting: "mariana.souza"))
        }

        await store.send(
            .path(.element(id: 2, action: .sync(.delegate(.done(profile: .preview, overview: .preview)))))
        ) {
            $0.path[id: 3] = .ready(ReadyFeature.State(userName: "João", overview: .preview))
        }

        await store.send(.path(.element(id: 3, action: .ready(.delegate(.enter)))))
        await store.receive(.delegate(.finished))
    }

    @Test
    func passkeyLoginGreetsGenerically() async {
        let store = TestStore(initialState: OnboardingFeature.State(splash: false)) {
            OnboardingFeature()
        }

        await store.send(.loginTapped) {
            $0.path[id: 0] = .login(LoginFeature.State())
        }

        await store.send(
            .path(.element(id: 0, action: .login(.delegate(.loggedIn(username: nil, session: .preview)))))
        ) {
            $0.session = .preview
            $0.path[id: 1] = .sync(SyncFeature.State(greeting: "estudante"))
        }
    }

    @Test
    func syncAuthFailureFallsBackToLogin() async {
        var initialState = OnboardingFeature.State(splash: false)
        initialState.path.append(.login(LoginFeature.State()))
        initialState.path.append(.sync(SyncFeature.State(greeting: "mariana.souza")))

        let store = TestStore(initialState: initialState) {
            OnboardingFeature()
        }

        await store.send(.path(.element(id: 1, action: .sync(.delegate(.authFailed))))) {
            $0.path.removeLast()
            $0.path[id: 0] = .login(LoginFeature.State(errorMessage: "Sua sessão expirou. Entre novamente."))
        }
    }
}

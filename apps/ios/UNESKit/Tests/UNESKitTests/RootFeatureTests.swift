import ComposableArchitecture
import Testing

@testable import UNESKit

@MainActor
struct RootFeatureTests {
    @Test
    func logoutSwapsTheShellForTheFarewell() async {
        let analyticsCalls = LockIsolated<[String]>([])
        let store = TestStore(initialState: RootFeature.State.connected(AppFeature.State())) {
            RootFeature()
        } withDependencies: {
            $0.analytics.reset = { analyticsCalls.withValue { $0.append("reset") } }
            $0.analytics.register = { properties in
                analyticsCalls.withValue { $0.append("register \(properties.keys.sorted().joined(separator: ","))") }
            }
        }

        await store.send(.connected(.me(.delegate(.loggedOut(firstName: "Mariana"))))) {
            $0 = .farewell(FarewellFeature.State(firstName: "Mariana"))
        }
        // Logout unlinks the person and re-stamps the device id (reset drops
        // super properties).
        #expect(analyticsCalls.value == ["reset", "register machine_id"])
    }

    @Test
    func farewellSignInLandsOnTheLogin() async {
        let store = TestStore(
            initialState: RootFeature.State.farewell(FarewellFeature.State(firstName: "Mariana"))
        ) {
            RootFeature()
        }
        // Stack element IDs are generator-drawn, so the pushed login state
        // cannot be reproduced exactly in an exhaustive assertion.
        store.exhaustivity = .off

        await store.send(.farewell(.signInTapped))
        await store.receive(\.farewell.delegate)

        guard case let .onboarding(onboarding) = store.state else {
            Issue.record("expected the onboarding shell, got \(store.state)")
            return
        }
        #expect(onboarding.splash == false)
        #expect(onboarding.path.count == 1)
        guard case .login = onboarding.path.first else {
            Issue.record("expected the login screen on top, got \(String(describing: onboarding.path.first))")
            return
        }
    }

    @Test
    func farewellFlashSettlesAfterTheBeat() async {
        let clock = TestClock()
        let store = TestStore(
            initialState: RootFeature.State.farewell(FarewellFeature.State(firstName: "Mariana"))
        ) {
            RootFeature()
        } withDependencies: {
            $0.continuousClock = clock
        }

        await store.send(.farewell(.task))
        await clock.advance(by: .milliseconds(900))
        await store.receive(\.farewell.flashFinished) {
            $0 = .farewell(FarewellFeature.State(firstName: "Mariana", isFlashing: false))
        }
    }
}

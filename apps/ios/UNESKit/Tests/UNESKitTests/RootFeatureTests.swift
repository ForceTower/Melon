import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct RootFeatureTests {
    @Test
    func logoutSwapsTheShellForTheFarewell() async {
        let analyticsCalls = LockIsolated<[String]>([])
        let teardown = LockIsolated<[String]>([])
        let store = TestStore(initialState: RootFeature.State.connected(AppFeature.State())) {
            RootFeature()
        } withDependencies: {
            $0.analytics.reset = { analyticsCalls.withValue { $0.append("reset") } }
            $0.analytics.register = { properties in
                analyticsCalls.withValue { $0.append("register \(properties.keys.sorted().joined(separator: ","))") }
            }
            $0.meRepository.wipeLocalData = { teardown.withValue { $0.append("wipe") } }
            $0.credentialInvalidation.reset = { teardown.withValue { $0.append("credentials") } }
            $0.sessionInvalidation.clear = { teardown.withValue { $0.append("session") } }
        }

        await store.send(.connected(.me(.delegate(.loggedOut(firstName: "Mariana"))))) {
            $0 = .farewell(FarewellFeature.State(firstName: "Mariana"))
        }
        await store.finish()
        // Logout unlinks the person and re-stamps the device id (reset drops
        // super properties).
        #expect(analyticsCalls.value == ["reset", "register machine_id"])
        #expect(teardown.value == ["wipe", "credentials", "session"])
    }

    @Test
    func logoutForgetsThePreviousAccountsLatches() async {
        let defaults = UserDefaults.inMemory
        defaults.set(true, forKey: CredentialInvalidation.storageKey)
        defaults.set("20191234", forKey: CredentialInvalidation.usernameKey)
        defaults.set(true, forKey: SessionInvalidation.storageKey)

        let store = TestStore(initialState: RootFeature.State.connected(AppFeature.State())) {
            RootFeature()
        } withDependencies: {
            $0.defaultAppStorage = defaults
            $0.sessionStore = .inMemory(initial: .preview)
            $0.commuteActivity.stopAll = {}
            $0.push.unregister = {}
            $0.meRepository.wipeLocalData = {}
            $0.credentialInvalidation = .liveValue
            $0.sessionInvalidation = .liveValue
        }
        store.exhaustivity = .off

        await store.send(.connected(.me(.logoutTapped)))
        await store.send(.connected(.me(.logoutConfirmed)))
        await store.receive(\.connected.me.delegate)
        await store.finish()

        // The next account must not open on this one's banners or portal
        // username. Home's `@Shared` default writes "" back once the key goes.
        #expect(defaults.bool(forKey: CredentialInvalidation.storageKey) == false)
        #expect((defaults.string(forKey: CredentialInvalidation.usernameKey) ?? "").isEmpty)
        #expect(defaults.bool(forKey: SessionInvalidation.storageKey) == false)
    }

    @Test
    func logoutStopsTheBackfillBeforeWipingTheMirror() async {
        let events = LockIsolated<[String]>([])
        let (started, startedContinuation) = AsyncStream.makeStream(of: Void.self)

        let store = TestStore(initialState: RootFeature.State.connected(AppFeature.State())) {
            RootFeature()
        } withDependencies: {
            $0.push.requestAuthorization = {}
            $0.push.dataEvents = { .finished }
            $0.intentRouter.routes = { .finished }
            $0.syncRepository.ping = {}
            $0.syncRepository.backfillMirror = {
                try await withTaskCancellationHandler {
                    startedContinuation.yield()
                    try await Task.never()
                } onCancel: {
                    events.withValue { $0.append("backfill cancelled") }
                }
            }
            Self.recordLogout(into: events, &$0)
        }
        store.exhaustivity = .off

        await store.send(.connected(.task))
        for await _ in started { break }
        await store.send(.connected(.me(.logoutTapped)))
        await store.send(.connected(.me(.logoutConfirmed)))
        await store.receive(\.connected.me.delegate)
        await store.finish()

        // A semester response landing after the wipe would write the old
        // account back into a signed-out mirror.
        #expect(events.value == ["backfill cancelled", "wipe"])
    }

    @Test
    func logoutStopsAPendingPushRefreshBeforeWipingTheMirror() async {
        let clock = TestClock()
        let events = LockIsolated<[String]>([])
        let (started, startedContinuation) = AsyncStream.makeStream(of: Void.self)

        let store = TestStore(initialState: RootFeature.State.connected(AppFeature.State())) {
            RootFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.date = .constant(Date(timeIntervalSince1970: 1_776_000_000))
            $0.homeRepository.refresh = { _ in
                try await withTaskCancellationHandler {
                    startedContinuation.yield()
                    try await Task.never()
                } onCancel: {
                    events.withValue { $0.append("refresh cancelled") }
                }
            }
            Self.recordLogout(into: events, &$0)
        }
        store.exhaustivity = .off

        await store.send(.connected(.pushDataReceived(PushDataEvent(kind: "grades"))))
        await clock.advance(by: .seconds(2))
        for await _ in started { break }
        await store.send(.connected(.me(.logoutTapped)))
        await store.send(.connected(.me(.logoutConfirmed)))
        await store.receive(\.connected.me.delegate)
        await store.finish()

        #expect(events.value == ["refresh cancelled", "wipe"])
    }

    @Test
    func aSessionThatWontClearKeepsTheShell() async {
        struct KeychainFailure: Error {}
        let wiped = LockIsolated(false)

        let store = TestStore(initialState: RootFeature.State.connected(AppFeature.State())) {
            RootFeature()
        } withDependencies: {
            $0.sessionStore = SessionStore(
                current: { .preview },
                save: { _ in },
                clear: { throw KeychainFailure() }
            )
            $0.commuteActivity.stopAll = {}
            $0.push.unregister = {}
            $0.meRepository.wipeLocalData = { wiped.setValue(true) }
            $0.credentialInvalidation = .liveValue
            $0.sessionInvalidation = .liveValue
        }
        store.exhaustivity = .off

        await store.send(.connected(.me(.logoutTapped)))
        await store.send(.connected(.me(.logoutConfirmed)))
        await store.finish()
        await store.skipReceivedActions()

        // The Keychain still holds the session, so a farewell would be a lie
        // the next launch undoes — and the mirror would already be gone.
        #expect(store.state.isConnected)
        #expect(wiped.value == false)

        await store.send(.connected(.me(.logoutTapped)))
        guard case let .connected(app) = store.state else { return }
        #expect(app.me.isLogoutPromptPresented)
    }

    private nonisolated static func recordLogout(
        into events: LockIsolated<[String]>,
        _ dependencies: inout DependencyValues
    ) {
        dependencies.sessionStore = .inMemory(initial: .preview)
        dependencies.commuteActivity.stopAll = {}
        dependencies.push.unregister = {}
        dependencies.meRepository.wipeLocalData = { events.withValue { $0.append("wipe") } }
        dependencies.credentialInvalidation = .liveValue
        dependencies.sessionInvalidation = .liveValue
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

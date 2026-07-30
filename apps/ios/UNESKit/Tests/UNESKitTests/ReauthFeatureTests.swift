import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct ReauthFeatureTests {
    @Test
    func aCorrectPasswordClearsTheFlagAndTellsHome() async {
        let cleared = LockIsolated(0)
        let submitted = LockIsolated<String?>(nil)

        let store = TestStore(initialState: ReauthFeature.State(username: "20191234", password: "nova")) {
            ReauthFeature()
        } withDependencies: {
            $0.credentialStatusRepository.reauthenticate = { password, _ in
                submitted.setValue(password)
            }
            $0.credentialInvalidation.clear = { cleared.withValue { $0 += 1 } }
        }

        await store.send(.submitTapped) { $0.isLoading = true }
        await store.receive(.reauthResponse(.success(true))) { $0.isLoading = false }
        await store.receive(.delegate(.succeeded))

        #expect(submitted.value == "nova")
        #expect(cleared.value == 1)
    }

    @Test
    func aWrongPasswordKeepsTheSheetOpenAndTheFlagSet() async {
        let cleared = LockIsolated(0)

        let store = TestStore(initialState: ReauthFeature.State(username: "20191234", password: "errada")) {
            ReauthFeature()
        } withDependencies: {
            $0.credentialStatusRepository.reauthenticate = { _, _ in throw ReauthFailure.invalidPassword }
            $0.credentialInvalidation.clear = { cleared.withValue { $0 += 1 } }
        }

        await store.send(.submitTapped) { $0.isLoading = true }
        await store.receive(.reauthResponse(.failure(.invalidPassword))) {
            $0.isLoading = false
            $0.errorMessage = String.localized(.reauthErrorInvalid)
        }

        // Still broken — the banner must survive so the user can retry.
        #expect(cleared.value == 0)
    }

    @Test
    func anUnavailableUpstreamOffersARetryRatherThanBlamingThePassword() async {
        let store = TestStore(initialState: ReauthFeature.State(username: "20191234", password: "certa")) {
            ReauthFeature()
        } withDependencies: {
            $0.credentialStatusRepository.reauthenticate = { _, _ in throw ReauthFailure.upstreamUnavailable }
        }

        await store.send(.submitTapped) { $0.isLoading = true }
        await store.receive(.reauthResponse(.failure(.upstreamUnavailable))) {
            $0.isLoading = false
            $0.errorMessage = String.localized(.reauthErrorUnavailable)
        }
    }

    @Test
    func anEmptyPasswordCannotBeSubmitted() async {
        let store = TestStore(initialState: ReauthFeature.State(username: "20191234")) {
            ReauthFeature()
        }

        #expect(store.state.canSubmit == false)
        await store.send(.submitTapped)
    }

    @Test
    func theCredentialsPushRoutesToTheSheet() {
        #expect(Deeplinks.parse("unes://reauth") == .reauth)
        #expect(Deeplinks.parse("UNES://REAUTH") == .reauth)
        #expect(Deeplinks.parse("unes://reauth?src=push") == .reauth)
    }
}

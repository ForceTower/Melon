import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct PasskeysFeatureTests {
    static nonisolated let phone = PasskeyCredential(
        id: "pk_phone",
        deviceName: "iPhone 15 Pro",
        isSynced: true,
        createdAt: Date(timeIntervalSince1970: 1_738_540_800)
    )

    static nonisolated let key = PasskeyCredential(
        id: "pk_key",
        deviceName: "YubiKey",
        isSynced: false,
        createdAt: Date(timeIntervalSince1970: 1_740_000_000)
    )

    static nonisolated let options = PasskeyRegistrationOptions(
        challenge: "challenge",
        rpId: "unes.forcetower.dev",
        userId: "user",
        userName: "mariana.nogueira",
        userDisplayName: "Mariana Nogueira",
        excludedCredentialIds: [],
        algorithms: [-7, -257]
    )

    static nonisolated let attestation = PasskeyAttestation(
        id: "pk_new",
        rawId: "pk_new",
        authenticatorAttachment: "platform",
        clientDataJSON: "",
        attestationObject: ""
    )

    static nonisolated let enrolled = PasskeyCredential(
        id: attestation.id,
        deviceName: "iPhone",
        isSynced: true,
        createdAt: Date(timeIntervalSince1970: 1_776_000_000)
    )

    private func makeStore(clock: TestClock<Duration>) -> TestStoreOf<PasskeysFeature> {
        TestStore(
            initialState: PasskeysFeature.State(credentials: [Self.phone, Self.key], hasLoaded: true)
        ) {
            PasskeysFeature()
        } withDependencies: {
            $0.continuousClock = clock
        }
    }

    private func stubEnrollment(_ store: TestStoreOf<PasskeysFeature>, registered: LockIsolated<[String]>) {
        store.dependencies.passkeyRepository.registrationOptions = { Self.options }
        store.dependencies.passkeyClient.register = { _, _ in Self.attestation }
        store.dependencies.passkeyRepository.register = { attestation, _ in
            registered.withValue { $0.append(attestation.id) }
        }
    }

    @Test
    func dismissingTheAddSheetMidCreateLetsItReopen() async {
        let clock = TestClock()
        let store = makeStore(clock: clock)
        store.dependencies.passkeyRepository.registrationOptions = {
            try await clock.sleep(for: .seconds(5))
            return Self.options
        }

        await store.send(.addTapped) {
            $0.isAddPresented = true
        }
        await store.send(.addContinueTapped) {
            $0.isCreating = true
        }
        await store.send(.addDismissed) {
            $0.isAddPresented = false
            $0.isCreating = false
        }
        await store.send(.addTapped) {
            $0.isAddPresented = true
        }
    }

    @Test
    func aNewKeyLandsInTheListHighlighted() async {
        let clock = TestClock()
        let registered = LockIsolated<[String]>([])
        let store = makeStore(clock: clock)
        stubEnrollment(store, registered: registered)
        store.dependencies.passkeyRepository.credentials = { [Self.phone, Self.key, Self.enrolled] }

        await store.send(.addTapped) {
            $0.isAddPresented = true
        }
        await store.send(.addContinueTapped) {
            $0.isCreating = true
        }
        await store.receive(\.created) {
            $0.isCreating = false
            $0.addStep = .success
            $0.newCredentialID = Self.attestation.id
        }
        await store.receive(\.credentialsResponse) {
            $0.credentials = [Self.phone, Self.key, Self.enrolled]
        }
        #expect(registered.value == [Self.attestation.id])

        await clock.advance(by: .milliseconds(1050))
        await store.receive(\.addSuccessCompleted) {
            $0.isAddPresented = false
            $0.addStep = .choose
            $0.toast = String.localized(.passkeysToastCreated)
        }
        await clock.advance(by: .seconds(3))
        await store.receive(\.highlightExpired) {
            $0.newCredentialID = nil
        }
        await store.receive(\.toastExpired) {
            $0.toast = nil
        }
    }

    @Test
    func aListRefreshFailureAfterEnrollmentStillReportsSuccess() async {
        let clock = TestClock()
        let registered = LockIsolated<[String]>([])
        let store = makeStore(clock: clock)
        stubEnrollment(store, registered: registered)
        store.dependencies.passkeyRepository.credentials = {
            throw URLError(.notConnectedToInternet)
        }

        await store.send(.addTapped) {
            $0.isAddPresented = true
        }
        await store.send(.addContinueTapped) {
            $0.isCreating = true
        }
        await store.receive(\.created) {
            $0.isCreating = false
            $0.addStep = .success
            $0.newCredentialID = Self.attestation.id
        }
        await store.receive(\.credentialsLoadFailed)
        #expect(registered.value == [Self.attestation.id])
        #expect(store.state.createError == nil)

        await clock.advance(by: .milliseconds(1050))
        await store.receive(\.addSuccessCompleted) {
            $0.isAddPresented = false
            $0.addStep = .choose
            $0.toast = String.localized(.passkeysToastCreated)
        }
        await clock.advance(by: .seconds(3))
        await store.receive(\.highlightExpired) {
            $0.newCredentialID = nil
        }
        await store.receive(\.toastExpired) {
            $0.toast = nil
        }
    }

    @Test
    func aLateFailureStaysWithTheKeyItBelongsTo() async {
        let clock = TestClock()
        let store = makeStore(clock: clock)
        store.dependencies.passkeyRepository.delete = { _ in
            try await clock.sleep(for: .seconds(1))
            throw URLError(.notConnectedToInternet)
        }

        await store.send(.rowTapped(Self.phone)) {
            $0.detail = Self.phone
        }
        await store.send(.deleteTapped) {
            $0.pendingDelete = Self.phone
        }
        await store.send(.deleteConfirmed) {
            $0.pendingDelete = nil
            $0.mutatingIDs = [Self.phone.id]
        }
        #expect(store.state.isMutating)

        await store.send(.detailDismissed) {
            $0.detail = nil
        }
        await store.send(.rowTapped(Self.key)) {
            $0.detail = Self.key
        }
        #expect(store.state.isMutating == false)

        await clock.advance(by: .seconds(1))
        await store.receive(\.mutationFailed) {
            $0.mutatingIDs = []
        }
        #expect(store.state.mutationError == nil)
    }

    @Test
    func aFailureOnTheOpenKeyShowsOnItsSheet() async {
        let clock = TestClock()
        let store = makeStore(clock: clock)
        store.dependencies.passkeyRepository.delete = { _ in
            try await clock.sleep(for: .seconds(1))
            throw URLError(.notConnectedToInternet)
        }

        await store.send(.rowTapped(Self.phone)) {
            $0.detail = Self.phone
        }
        await store.send(.deleteTapped) {
            $0.pendingDelete = Self.phone
        }
        await store.send(.deleteConfirmed) {
            $0.pendingDelete = nil
            $0.mutatingIDs = [Self.phone.id]
        }
        await store.send(.detailDismissed) {
            $0.detail = nil
        }
        await store.send(.rowTapped(Self.phone)) {
            $0.detail = Self.phone
        }
        #expect(store.state.isMutating)

        await clock.advance(by: .seconds(1))
        await store.receive(\.mutationFailed) {
            $0.mutatingIDs = []
            $0.mutationError = String.localized(.passkeysMutationErrorMessage)
        }
    }
}

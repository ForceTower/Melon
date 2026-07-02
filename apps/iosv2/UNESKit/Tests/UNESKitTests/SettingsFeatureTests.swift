import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct SettingsFeatureTests {
    @Test
    func taskLoadsTheAccount() async {
        var settings = UserSettings()
        settings.gradeSpoiler = .discreet
        settings.classSubject = true
        let account = SettingsAccount(profile: .preview, settings: settings)

        let store = TestStore(initialState: SettingsFeature.State()) {
            SettingsFeature()
        } withDependencies: {
            $0.sessionStore = .inMemory(initial: .preview)
            $0.settingsRepository.account = { account }
            $0.settingsRepository.credentials = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.userName = "Mariana Souza"
        }
        await store.receive(.accountLoaded(account)) {
            $0.profile = .preview
            $0.settings = settings
        }
    }

    @Test
    func taskLoadsTheCredentials() async {
        let store = TestStore(initialState: SettingsFeature.State()) {
            SettingsFeature()
        } withDependencies: {
            $0.sessionStore = .inMemory(initial: .preview)
            $0.settingsRepository.account = { throw APIError.emptyEnvelope }
            $0.settingsRepository.credentials = { .preview }
        }

        await store.send(.task) {
            $0.userName = "Mariana Souza"
        }
        await store.receive(.credentialsLoaded(.preview)) {
            $0.credentials = .preview
        }
    }

    @Test
    func togglingANotificationPatchesJustThatField() async {
        let patched = LockIsolated<[SettingsChange]>([])

        let store = TestStore(initialState: SettingsFeature.State()) {
            SettingsFeature()
        } withDependencies: {
            $0.settingsRepository.update = { change in
                patched.withValue { $0.append(change) }
                var settings = UserSettings()
                settings.apply(change)
                return settings
            }
        }

        await store.send(.notificationToggled(.gradeDateChanged)) {
            $0.settings.gradeDateChanged = true
        }
        await store.finish()

        #expect(patched.value == [.notification(.gradeDateChanged, isOn: true)])
    }

    @Test
    func failedPatchRollsTheToggleBack() async {
        let store = TestStore(initialState: SettingsFeature.State()) {
            SettingsFeature()
        } withDependencies: {
            $0.settingsRepository.update = { _ in throw APIError.emptyEnvelope }
        }

        await store.send(.notificationToggled(.messageBroadcast)) {
            $0.settings.messageBroadcast = false
        }
        await store.receive(.settingsUpdateFailed(revert: .notification(.messageBroadcast, isOn: true))) {
            $0.settings.messageBroadcast = true
        }
    }

    @Test
    func selectingASpoilerPatchesAndIgnoresReselection() async {
        let patched = LockIsolated<[SettingsChange]>([])

        let store = TestStore(initialState: SettingsFeature.State()) {
            SettingsFeature()
        } withDependencies: {
            $0.settingsRepository.update = { change in
                patched.withValue { $0.append(change) }
                var settings = UserSettings()
                settings.apply(change)
                return settings
            }
        }

        await store.send(.spoilerSelected(.value)) {
            $0.settings.gradeSpoiler = .value
        }
        await store.finish()
        await store.send(.spoilerSelected(.value))

        #expect(patched.value == [.gradeSpoiler(.value)])
    }

    @Test
    func revealAuthenticatesAndExpiresAfterThirtySeconds() async {
        let clock = TestClock()

        let store = TestStore(initialState: SettingsFeature.State(credentials: .preview)) {
            SettingsFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.localAuth.authenticate = { _ in true }
        }

        await store.send(.revealTapped)
        await store.receive(.revealAuthenticated) {
            $0.isPasswordRevealed = true
        }
        await clock.advance(by: .seconds(30))
        await store.receive(.revealExpired) {
            $0.isPasswordRevealed = false
        }
    }

    @Test
    func deniedAuthenticationKeepsThePasswordMasked() async {
        let store = TestStore(initialState: SettingsFeature.State(credentials: .preview)) {
            SettingsFeature()
        } withDependencies: {
            $0.localAuth.authenticate = { _ in false }
        }

        await store.send(.revealTapped)
        await store.finish()
    }

    @Test
    func hidingCancelsTheRevealExpiry() async {
        let clock = TestClock()

        let store = TestStore(initialState: SettingsFeature.State(credentials: .preview)) {
            SettingsFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.localAuth.authenticate = { _ in true }
        }

        await store.send(.revealTapped)
        await store.receive(.revealAuthenticated) {
            $0.isPasswordRevealed = true
        }
        await store.send(.revealTapped) {
            $0.isPasswordRevealed = false
        }
    }

    @Test
    func copyingPutsTheUsernameOnThePasteboard() async {
        let clock = TestClock()
        let copied = LockIsolated<[String]>([])

        let store = TestStore(initialState: SettingsFeature.State(credentials: .preview)) {
            SettingsFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.pasteboard.copy = { text in copied.withValue { $0.append(text) } }
        }

        await store.send(.copyTapped(.username)) {
            $0.copied = .username
        }
        await clock.advance(by: .milliseconds(1400))
        await store.receive(.copyFeedbackExpired) {
            $0.copied = nil
        }

        #expect(copied.value == ["mariana.nogueira"])
    }

    @Test
    func passwordOnlyCopiesWhileRevealed() async {
        let clock = TestClock()
        let copied = LockIsolated<[String]>([])

        var initialState = SettingsFeature.State(credentials: .preview)
        let store = TestStore(initialState: initialState) {
            SettingsFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.pasteboard.copy = { text in copied.withValue { $0.append(text) } }
        }

        await store.send(.copyTapped(.password))
        #expect(copied.value.isEmpty)

        initialState = SettingsFeature.State(credentials: .preview, isPasswordRevealed: true)
        let revealedStore = TestStore(initialState: initialState) {
            SettingsFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.pasteboard.copy = { text in copied.withValue { $0.append(text) } }
        }

        await revealedStore.send(.copyTapped(.password)) {
            $0.copied = .password
        }
        await clock.advance(by: .milliseconds(1400))
        await revealedStore.receive(.copyFeedbackExpired) {
            $0.copied = nil
        }

        #expect(copied.value == ["ma·nogue!ra·2024"])
    }

    @Test
    func selectingAThemeWritesTheSharedValue() async {
        let store = TestStore(initialState: SettingsFeature.State()) {
            SettingsFeature()
        }

        await store.send(.themeSelected(.dark)) {
            $0.$theme.withLock { $0 = .dark }
        }
    }
}

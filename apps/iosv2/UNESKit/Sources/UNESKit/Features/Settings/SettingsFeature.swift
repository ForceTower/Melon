import ComposableArchitecture
import Foundation

@Reducer
struct SettingsFeature {
    @ObservableState
    struct State: Equatable {
        /// Identity seeded from the Eu tab (or the session) so the hero
        /// doesn't flash empty while the account fetch is in flight.
        var profile: Profile?
        var userName: String?
        var credentials: AccountCredentials?
        var settings = UserSettings()
        var isPasswordRevealed = false
        /// Which credential just landed on the pasteboard — drives the
        /// transient "copiado" feedback.
        var copied: CredentialField?
        @Shared(.appStorage("theme")) var theme: AppTheme = .system

        var displayName: String? { profile?.name ?? userName }
    }

    enum CredentialField: Equatable, Sendable {
        case username, password
    }

    enum Action: Equatable {
        case task
        case accountLoaded(SettingsAccount)
        case credentialsLoaded(AccountCredentials?)
        case themeSelected(AppTheme)
        case spoilerSelected(GradeSpoiler)
        case notificationToggled(NotificationToggle)
        case settingsUpdateFailed(revert: SettingsChange)
        case revealTapped
        case revealAuthenticated
        case revealExpired
        case copyTapped(CredentialField)
        case copyFeedbackExpired
    }

    @Dependency(\.settingsRepository) var settingsRepository
    @Dependency(\.sessionStore) var sessionStore
    @Dependency(\.localAuth) var localAuth
    @Dependency(\.pasteboard) var pasteboard
    @Dependency(\.continuousClock) var clock

    /// How long the password stays visible after a Face ID reveal.
    private static let revealWindow: Duration = .seconds(30)

    private enum CancelID: Hashable {
        case revealExpiry
        case copyFeedback
        case save(SettingsChange.Field)
    }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                if state.userName == nil {
                    state.userName = sessionStore.current()?.user.name
                }
                return .merge(loadAccount(), loadCredentials())

            case let .accountLoaded(account):
                state.profile = account.profile
                state.settings = account.settings
                return .none

            case let .credentialsLoaded(credentials):
                state.credentials = credentials
                return .none

            case let .themeSelected(theme):
                state.$theme.withLock { $0 = theme }
                return .none

            case let .spoilerSelected(spoiler):
                guard spoiler != state.settings.gradeSpoiler else { return .none }
                let revert = SettingsChange.gradeSpoiler(state.settings.gradeSpoiler)
                state.settings.gradeSpoiler = spoiler
                return save(.gradeSpoiler(spoiler), revert: revert)

            case let .notificationToggled(toggle):
                let isOn = !state.settings[keyPath: toggle.keyPath]
                state.settings[keyPath: toggle.keyPath] = isOn
                return save(
                    .notification(toggle, isOn: isOn),
                    revert: .notification(toggle, isOn: !isOn)
                )

            case let .settingsUpdateFailed(revert):
                state.settings.apply(revert)
                return .none

            case .revealTapped:
                guard state.credentials != nil else { return .none }
                if state.isPasswordRevealed {
                    state.isPasswordRevealed = false
                    return .cancel(id: CancelID.revealExpiry)
                }
                return .run { send in
                    guard try await localAuth.authenticate("Autentique para ver a senha") else { return }
                    await send(.revealAuthenticated)
                } catch: { _, _ in
                    // Denied or cancelled — the password stays masked.
                }

            case .revealAuthenticated:
                state.isPasswordRevealed = true
                return .run { send in
                    try await clock.sleep(for: Self.revealWindow)
                    await send(.revealExpired)
                }
                .cancellable(id: CancelID.revealExpiry, cancelInFlight: true)

            case .revealExpired:
                state.isPasswordRevealed = false
                return .none

            case let .copyTapped(field):
                guard let credentials = state.credentials else { return .none }
                if field == .password, !state.isPasswordRevealed { return .none }
                state.copied = field
                let value = field == .username ? credentials.username : credentials.password
                return .run { send in
                    await pasteboard.copy(value)
                    try await clock.sleep(for: .milliseconds(1400))
                    await send(.copyFeedbackExpired)
                }
                .cancellable(id: CancelID.copyFeedback, cancelInFlight: true)

            case .copyFeedbackExpired:
                state.copied = nil
                return .none
            }
        }
    }

    /// Both loads fail silently: the screen has no error surface, and the
    /// defaults are the server's own row defaults.
    private func loadAccount() -> Effect<Action> {
        .run { send in
            guard let account = try? await settingsRepository.account() else { return }
            await send(.accountLoaded(account))
        }
    }

    private func loadCredentials() -> Effect<Action> {
        .run { send in
            guard let credentials = try? await settingsRepository.credentials() else { return }
            await send(.credentialsLoaded(credentials))
        }
    }

    /// Fires the optimistic PATCH. The response echoes the value we already
    /// applied, and adopting the full server row could clobber another
    /// in-flight field, so success is ignored and only failure rolls back.
    private func save(_ change: SettingsChange, revert: SettingsChange) -> Effect<Action> {
        .run { _ in
            _ = try await settingsRepository.update(change)
        } catch: { _, send in
            await send(.settingsUpdateFailed(revert: revert))
        }
        .cancellable(id: CancelID.save(change.field), cancelInFlight: true)
    }
}

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
        var appIcon: AppIcon = .aurora
        /// Taps accumulated on the version footer toward the BaseSans unlock.
        var secretIconTaps = 0
        var toast: SettingsToast?
        /// Icons being celebrated by the unlock sheet right now.
        var unlockSheetIcons: [AppIcon]?
        @Shared(.appStorage("theme")) var theme: AppTheme = .system
        @Shared(.unlockedSecretIcons) var unlockedSecretIcons
        @Shared(.announcedSecretIcons) var announcedSecretIcons

        var displayName: String? { profile?.name ?? userName }
    }

    enum CredentialField: Equatable, Sendable {
        case username, password
    }

    /// The transient pill above the tab bar — icon-hunt breadcrumbs.
    enum SettingsToast: Equatable, Sendable {
        case secretHint(remaining: Int)
        case secretsAlreadyUnlocked
        case lockedHint(AppIcon)
    }

    enum Action: Equatable {
        case task
        case accountLoaded(SettingsAccount)
        case credentialsLoaded(AccountCredentials?)
        case themeSelected(AppTheme)
        case spoilerSelected(GradeSpoiler)
        case notificationToggled(NotificationToggle)
        case settingsUpdateFailed(revert: SettingsChange)
        case appIconLoaded(AppIcon)
        case appIconSelected(AppIcon)
        case appIconChangeFailed(revert: AppIcon)
        case lockedIconTapped(AppIcon)
        case versionTapped
        case unlockSheetDismissed
        case toastExpired
        case revealTapped
        case revealAuthenticated
        case revealExpired
        case copyTapped(CredentialField)
        case copyFeedbackExpired
        case securityRowTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            /// The host stack should push the passkeys manager.
            case openPasskeys
        }
    }

    @Dependency(\.settingsRepository) var settingsRepository
    @Dependency(\.appIconClient) var appIconClient
    @Dependency(\.sessionStore) var sessionStore
    @Dependency(\.localAuth) var localAuth
    @Dependency(\.pasteboard) var pasteboard
    @Dependency(\.continuousClock) var clock

    private let log = Log.scoped("SettingsFeature")

    /// How long the password stays visible after a Face ID reveal.
    private static let revealWindow: Duration = .seconds(30)

    /// How long a toast lingers before fading out.
    private static let toastWindow: Duration = .milliseconds(2600)

    /// Version-footer taps needed to unlock the secret icons.
    private static let secretTapsNeeded = 7

    private enum CancelID: Hashable {
        case revealExpiry
        case copyFeedback
        case toastExpiry
        case appIconChange
        case save(SettingsChange.Field)
    }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                if state.userName == nil {
                    state.userName = sessionStore.current()?.user.name
                }
                // Icon first (a local read) so the action order stays deterministic.
                return .concatenate(loadAppIcon(), .merge(loadAccount(), loadCredentials()))

            case let .accountLoaded(account):
                state.profile = account.profile
                state.settings = account.settings
                // Server-known discoveries unlock silently: announced too, so
                // this device doesn't celebrate a find from another one.
                let serverIcons = account.settings.unlockedIcons
                if !serverIcons.isEmpty {
                    state.$unlockedSecretIcons.withLock { $0.formUnion(serverIcons) }
                    state.$announcedSecretIcons.withLock { $0.formUnion(serverIcons) }
                }
                // Local discoveries the server missed (e.g. a failed PATCH).
                let unsynced = state.unlockedSecretIcons.icons.subtracting(serverIcons)
                guard !unsynced.isEmpty else { return .none }
                return pushUnlockedIcons(state.unlockedSecretIcons.icons)

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
                log.info("set grade spoiler \(spoiler)")
                return save(.gradeSpoiler(spoiler), revert: revert)

            case let .notificationToggled(toggle):
                let isOn = !state.settings[keyPath: toggle.keyPath]
                state.settings[keyPath: toggle.keyPath] = isOn
                log.info("set notification toggle \(toggle) isOn=\(isOn)")
                return save(
                    .notification(toggle, isOn: isOn),
                    revert: .notification(toggle, isOn: !isOn)
                )

            case let .settingsUpdateFailed(revert):
                state.settings.apply(revert)
                return .none

            case let .appIconLoaded(icon):
                state.appIcon = icon
                return .none

            case let .appIconSelected(icon):
                guard icon != state.appIcon else { return .none }
                guard !icon.isSecret || state.unlockedSecretIcons.contains(icon) else { return .none }
                let revert = state.appIcon
                state.appIcon = icon
                state.unlockSheetIcons = nil
                log.info("set app icon \(icon.rawValue)")
                return .run { _ in
                    try await appIconClient.set(icon)
                } catch: { [log] error, send in
                    log.warn("app icon change failed", error: error)
                    await send(.appIconChangeFailed(revert: revert))
                }
                .cancellable(id: CancelID.appIconChange, cancelInFlight: true)

            case let .appIconChangeFailed(revert):
                state.appIcon = revert
                return .none

            case let .lockedIconTapped(icon):
                return showToast(.lockedHint(icon), in: &state)

            case .versionTapped:
                guard !state.unlockedSecretIcons.contains(.baseSans) else {
                    return showToast(.secretsAlreadyUnlocked, in: &state)
                }
                state.secretIconTaps += 1
                if state.secretIconTaps >= Self.secretTapsNeeded {
                    state.secretIconTaps = 0
                    state.$unlockedSecretIcons.withLock { $0.insert(.baseSans) }
                    // Announced immediately: this screen owns the celebration,
                    // so the app-level observer must not fire a second sheet.
                    state.$announcedSecretIcons.withLock { $0.insert(.baseSans) }
                    state.toast = nil
                    state.unlockSheetIcons = [.baseSans]
                    log.info("secret app icon unlocked source=versionTap")
                    return .merge(
                        .cancel(id: CancelID.toastExpiry),
                        pushUnlockedIcons(state.unlockedSecretIcons.icons)
                    )
                }
                // The first taps stay silent; breadcrumbs only near the end.
                guard state.secretIconTaps >= 3 else { return .none }
                return showToast(
                    .secretHint(remaining: Self.secretTapsNeeded - state.secretIconTaps),
                    in: &state
                )

            case .unlockSheetDismissed:
                state.unlockSheetIcons = nil
                return .none

            case .toastExpired:
                state.toast = nil
                return .none

            case .revealTapped:
                guard state.credentials != nil else { return .none }
                if state.isPasswordRevealed {
                    state.isPasswordRevealed = false
                    return .cancel(id: CancelID.revealExpiry)
                }
                log.info("reveal password tapped")
                return .run { [log] send in
                    guard try await localAuth.authenticate(String.localized(.settingsRevealAuthReason)) else {
                        log.debug("reveal password declined")
                        return
                    }
                    await send(.revealAuthenticated)
                } catch: { [log] _, _ in
                    // Denied or cancelled — the password stays masked.
                    log.debug("reveal password auth threw")
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
                log.info("copy credential field=\(field)")
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

            case .securityRowTapped:
                log.info("open passkeys")
                return .send(.delegate(.openPasskeys))

            case .delegate:
                return .none
            }
        }
    }

    /// Both loads fail silently: the screen has no error surface, and the
    /// defaults are the server's own row defaults.
    private func loadAccount() -> Effect<Action> {
        .run { [log] send in
            do {
                let account = try await settingsRepository.account()
                await send(.accountLoaded(account))
            } catch {
                log.debug("settings account load failed")
            }
        }
    }

    private func loadAppIcon() -> Effect<Action> {
        .run { send in
            await send(.appIconLoaded(appIconClient.current()))
        }
    }

    /// Best-effort, no rollback: the icon stays unlocked locally either way,
    /// and the next settings load re-pushes anything the server missed.
    private func pushUnlockedIcons(_ icons: Set<AppIcon>) -> Effect<Action> {
        .run { _ in
            _ = try await settingsRepository.update(.unlockedIcons(icons))
        } catch: { [log] error, _ in
            log.warn("unlocked icons sync failed", error: error)
        }
        .cancellable(id: CancelID.save(.unlockedIcons), cancelInFlight: true)
    }

    private func showToast(_ toast: SettingsToast, in state: inout State) -> Effect<Action> {
        state.toast = toast
        return .run { send in
            try await clock.sleep(for: Self.toastWindow)
            await send(.toastExpired)
        }
        .cancellable(id: CancelID.toastExpiry, cancelInFlight: true)
    }

    private func loadCredentials() -> Effect<Action> {
        .run { [log] send in
            do {
                let credentials = try await settingsRepository.credentials()
                await send(.credentialsLoaded(credentials))
            } catch {
                log.debug("settings credentials load failed")
            }
        }
    }

    /// Fires the optimistic PATCH. The response echoes the value we already
    /// applied, and adopting the full server row could clobber another
    /// in-flight field, so success is ignored and only failure rolls back.
    private func save(_ change: SettingsChange, revert: SettingsChange) -> Effect<Action> {
        .run { _ in
            _ = try await settingsRepository.update(change)
        } catch: { [log] error, send in
            log.warn("settings update failed; will reconcile on next profile sync", error: error)
            await send(.settingsUpdateFailed(revert: revert))
        }
        .cancellable(id: CancelID.save(change.field), cancelInFlight: true)
    }
}

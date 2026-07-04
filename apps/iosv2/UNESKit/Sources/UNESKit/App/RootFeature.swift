import ComposableArchitecture

/// Gates the app between onboarding, the connected tab shell, and the
/// post-logout farewell.
@Reducer
struct RootFeature {
    @ObservableState
    enum State: Equatable {
        case onboarding(OnboardingFeature.State)
        case connected(AppFeature.State)
        case farewell(FarewellFeature.State)

        /// Session present → straight to the tab shell; otherwise onboarding.
        static func bootstrap() -> Self {
            @Dependency(\.sessionStore) var sessionStore
            return sessionStore.current() == nil
                ? .onboarding(OnboardingFeature.State())
                : .connected(AppFeature.State())
        }
    }

    enum Action {
        case task
        case legacyMigration(LegacyMigrationOutcome)
        case onboarding(OnboardingFeature.Action)
        case connected(AppFeature.Action)
        case farewell(FarewellFeature.Action)
    }

    @Dependency(\.widgetSync) var widgetSync
    @Dependency(\.spotlightSync) var spotlightSync
    @Dependency(\.legacyMigration) var legacyMigration
    private let log = Log.scoped("RootFeature")

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                // App-lifetime mirror → widget/Spotlight republishing, alive
                // across login/logout so a wipe also clears both.
                let widgets = Effect<Action>.run { _ in await widgetSync.run() }
                let spotlight = Effect<Action>.run { _ in await spotlightSync.run() }
                guard case .onboarding = state else {
                    // Already signed in — sweep anything the legacy app left.
                    return .merge(widgets, spotlight, .run { _ in legacyMigration.removeArtifacts() })
                }
                return .merge(widgets, spotlight, .run { send in
                    await send(.legacyMigration(legacyMigration.attempt()))
                })

            case let .legacyMigration(.migrated(session)):
                guard case .onboarding = state else { return .none }
                log.info("legacy session migrated userId=\(session.user.id) -> connected")
                state = .connected(AppFeature.State())
                return .none

            case let .legacyMigration(.loginRequired(prefillUsername)):
                guard let prefillUsername, case .onboarding = state else { return .none }
                return .send(.onboarding(.legacyUsernameRecovered(prefillUsername)))

            case .legacyMigration(.retry), .legacyMigration(.nothing):
                return .none

            case .onboarding(.delegate(.finished)):
                log.info("onboarding completed -> connected")
                state = .connected(AppFeature.State())
                return .none

            case let .connected(.me(.delegate(.loggedOut(firstName)))):
                log.info("user logged out -> farewell")
                state = .farewell(FarewellFeature.State(firstName: firstName))
                return .none

            case .farewell(.delegate(.signIn)):
                log.info("farewell -> onboarding sign-in")
                var onboarding = OnboardingFeature.State(splash: false)
                onboarding.path.append(.login(LoginFeature.State()))
                state = .onboarding(onboarding)
                return .none

            case .onboarding, .connected, .farewell:
                return .none
            }
        }
        .ifCaseLet(\.onboarding, action: \.onboarding) { OnboardingFeature() }
        .ifCaseLet(\.connected, action: \.connected) { AppFeature() }
        .ifCaseLet(\.farewell, action: \.farewell) { FarewellFeature() }
    }
}

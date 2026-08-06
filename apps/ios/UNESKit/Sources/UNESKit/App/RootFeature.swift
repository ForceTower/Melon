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
    @Dependency(\.watchSync) var watchSync
    @Dependency(\.spotlightSync) var spotlightSync
    @Dependency(\.evaluationReminders) var evaluationReminders
    @Dependency(\.legacyMigration) var legacyMigration
    @Dependency(\.sessionStore) var sessionStore
    @Dependency(\.analytics) var analytics
    private let log = Log.scoped("RootFeature")

    var body: some ReducerOf<Self> {
        // Gated: RootView crossfades between shells, so the departing shell's
        // view tree stays alive for the fade and can still emit view actions
        // (a re-fired `.task`, a navigation write). Those stragglers arrive
        // after the case has flipped and would trip ifCaseLet's mismatch
        // warning, so the gate drops them before the child reducers run.
        ShellGate(base: core)
    }

    private var core: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                // Identifying every launch is safe — the SDK no-ops when the
                // distinct id is unchanged.
                if let session = sessionStore.current() {
                    analytics.identify(session.user.id)
                }
                // App-lifetime mirror → widget/watch/Spotlight republishing,
                // alive across login/logout so a wipe also clears them all.
                let widgets = Effect<Action>.run { _ in await widgetSync.run() }
                let watch = Effect<Action>.run { _ in await watchSync.run() }
                let spotlight = Effect<Action>.run { _ in await spotlightSync.run() }
                let reminders = Effect<Action>.run { _ in await evaluationReminders.run() }
                guard case .onboarding = state else {
                    // Already signed in — sweep anything the legacy app left.
                    return .merge(widgets, watch, spotlight, reminders, .run { _ in legacyMigration.removeArtifacts() })
                }
                return .merge(widgets, watch, spotlight, reminders, .run { send in
                    await send(.legacyMigration(legacyMigration.attempt()))
                })

            case let .legacyMigration(.migrated(session)):
                guard case .onboarding = state else { return .none }
                log.info("legacy session migrated userId=\(session.user.id) -> connected")
                analytics.identify(session.user.id)
                state = .connected(AppFeature.State())
                return .none

            case let .legacyMigration(.loginRequired(prefillUsername)):
                guard let prefillUsername, case .onboarding = state else { return .none }
                return .send(.onboarding(.legacyUsernameRecovered(prefillUsername)))

            case .legacyMigration(.retry), .legacyMigration(.nothing):
                return .none

            case .onboarding(.delegate(.finished)):
                log.info("onboarding completed -> connected")
                if let session = sessionStore.current() {
                    analytics.identify(session.user.id)
                }
                state = .connected(AppFeature.State())
                return .none

            case let .connected(.me(.delegate(.loggedOut(firstName)))):
                log.info("user logged out -> farewell")
                // reset() also drops super properties, so re-stamp the device
                // id that ties analytics rows to the OTel logs.
                analytics.reset()
                analytics.register(properties: ["machine_id": MachineIdentity.id])
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

/// Forwards only actions that belong to the shell currently on screen; shell
/// actions for a case the root has already left are teardown stragglers from
/// the crossfade and are dropped silently.
private struct ShellGate<Base: Reducer<RootFeature.State, RootFeature.Action>>: Reducer {
    let base: Base

    func reduce(
        into state: inout RootFeature.State,
        action: RootFeature.Action
    ) -> Effect<RootFeature.Action> {
        switch (state, action) {
        case (_, .task), (_, .legacyMigration),
             (.onboarding, .onboarding), (.connected, .connected), (.farewell, .farewell):
            // The non-deprecated dispatch entry point — the same one TCA's
            // own combinators (Reduce.init(some Reducer)) call through.
            base._reduce(into: &state, action: action)
        default:
            .none
        }
    }
}

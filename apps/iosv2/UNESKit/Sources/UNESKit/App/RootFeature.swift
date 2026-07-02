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
        case onboarding(OnboardingFeature.Action)
        case connected(AppFeature.Action)
        case farewell(FarewellFeature.Action)
    }

    @Dependency(\.widgetSync) var widgetSync

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                // App-lifetime mirror → widget republishing, alive across
                // login/logout so a wipe also clears the widgets.
                return .run { _ in await widgetSync.run() }

            case .onboarding(.delegate(.finished)):
                state = .connected(AppFeature.State())
                return .none

            case let .connected(.me(.delegate(.loggedOut(firstName, keptData, dataSummary)))):
                state = .farewell(FarewellFeature.State(
                    firstName: firstName,
                    keptData: keptData,
                    dataSummary: dataSummary
                ))
                return .none

            case .farewell(.delegate(.signIn)):
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

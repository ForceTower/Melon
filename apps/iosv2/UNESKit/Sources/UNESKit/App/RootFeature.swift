import ComposableArchitecture

/// Gates the app between onboarding and the connected tab shell.
@Reducer
struct RootFeature {
    @ObservableState
    enum State: Equatable {
        case onboarding(OnboardingFeature.State)
        case connected(AppFeature.State)

        /// Session present → straight to the tab shell; otherwise onboarding.
        static func bootstrap() -> Self {
            @Dependency(\.sessionStore) var sessionStore
            return sessionStore.current() == nil
                ? .onboarding(OnboardingFeature.State())
                : .connected(AppFeature.State())
        }
    }

    enum Action {
        case onboarding(OnboardingFeature.Action)
        case connected(AppFeature.Action)
    }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .onboarding(.delegate(.finished)):
                state = .connected(AppFeature.State())
                return .none

            case .onboarding, .connected:
                return .none
            }
        }
        .ifCaseLet(\.onboarding, action: \.onboarding) { OnboardingFeature() }
        .ifCaseLet(\.connected, action: \.connected) { AppFeature() }
    }
}

import ComposableArchitecture

@Reducer
struct MeFeature {
    @ObservableState
    struct State: Equatable {
        var profile: Profile?
        var isLoading = false
        var errorMessage: String?
    }

    enum Action: Equatable {
        case onAppear
        case profileLoaded(Profile)
        case profileFailed(String)
    }

    @Dependency(\.profileRepository) var profileRepository

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .onAppear:
                guard state.profile == nil, !state.isLoading else { return .none }
                state.isLoading = true
                state.errorMessage = nil
                return .run { send in
                    do {
                        await send(.profileLoaded(try await profileRepository.current()))
                    } catch {
                        await send(.profileFailed(error.localizedDescription))
                    }
                }

            case let .profileLoaded(profile):
                state.isLoading = false
                state.profile = profile
                return .none

            case let .profileFailed(message):
                state.isLoading = false
                state.errorMessage = message
                return .none
            }
        }
    }
}

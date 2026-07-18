import ComposableArchitecture

@Reducer
struct IntroFeature {
    static let slideCount = 4

    @ObservableState
    struct State: Equatable {
        var slide = 0
    }

    enum Action: Equatable {
        case task
        case slideChanged(Int)
        case continueTapped
        case skipTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case login
        }
    }

    @Dependency(\.push) var push
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("IntroFeature")

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(Screens.intro)
                return .none

            case let .slideChanged(index):
                state.slide = index
                return .none

            case .continueTapped:
                guard state.slide < Self.slideCount - 1 else {
                    // The last slide sells notifications — ask for permission
                    // right as it's accepted, before moving on to login.
                    log.info("intro finished, requesting notification permission and advancing to login")
                    return .run { send in
                        await push.requestAuthorization()
                        await send(.delegate(.login))
                    }
                }
                state.slide += 1
                return .none

            case .skipTapped:
                log.info("intro skipped, advancing to login")
                return .send(.delegate(.login))

            case .delegate:
                return .none
            }
        }
    }
}

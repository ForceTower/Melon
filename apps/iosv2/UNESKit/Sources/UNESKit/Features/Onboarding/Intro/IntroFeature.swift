import ComposableArchitecture

@Reducer
struct IntroFeature {
    static let slideCount = 4

    @ObservableState
    struct State: Equatable {
        var slide = 0
    }

    enum Action: Equatable {
        case slideChanged(Int)
        case continueTapped
        case skipTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case login
        }
    }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case let .slideChanged(index):
                state.slide = index
                return .none

            case .continueTapped:
                guard state.slide < Self.slideCount - 1 else {
                    return .send(.delegate(.login))
                }
                state.slide += 1
                return .none

            case .skipTapped:
                return .send(.delegate(.login))

            case .delegate:
                return .none
            }
        }
    }
}

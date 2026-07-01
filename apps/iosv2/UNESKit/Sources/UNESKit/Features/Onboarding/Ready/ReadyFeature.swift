import ComposableArchitecture

@Reducer
struct ReadyFeature {
    @ObservableState
    struct State: Equatable {
        var userName: String
        var overview: ReadyOverview
    }

    enum Action: Equatable {
        case enterTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case enter
        }
    }

    var body: some ReducerOf<Self> {
        Reduce { _, action in
            switch action {
            case .enterTapped:
                return .send(.delegate(.enter))
            case .delegate:
                return .none
            }
        }
    }
}

import ComposableArchitecture

@Reducer
struct OverviewFeature {
    @ObservableState
    struct State: Equatable {}

    enum Action: Equatable {
        case openMessagesTapped
        case openScheduleTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openMessages
            case openSchedule
        }
    }

    var body: some ReducerOf<Self> {
        Reduce { _, action in
            switch action {
            case .openMessagesTapped:
                return .send(.delegate(.openMessages))
            case .openScheduleTapped:
                return .send(.delegate(.openSchedule))
            case .delegate:
                return .none
            }
        }
    }
}

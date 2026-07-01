import ComposableArchitecture

@Reducer
struct MessagesFeature {
    @ObservableState
    struct State: Equatable {}

    enum Action: Equatable {
        case onAppear
    }

    var body: some ReducerOf<Self> {
        Reduce { _, action in
            switch action {
            case .onAppear:
                return .none
            }
        }
    }
}

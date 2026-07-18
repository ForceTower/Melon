import ComposableArchitecture

@Reducer
struct ReadyFeature {
    @ObservableState
    struct State: Equatable {
        var userName: String
        var overview: ReadyOverview
    }

    enum Action: Equatable {
        case task
        case enterTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case enter
        }
    }

    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("ReadyFeature")

    var body: some ReducerOf<Self> {
        Reduce { _, action in
            switch action {
            case .task:
                analytics.screen(Screens.ready)
                return .none
            case .enterTapped:
                analytics.selectContent(contentType: ContentTypes.cta, itemId: "ready_enter")
                return .send(.delegate(.enter))
            case .delegate:
                return .none
            }
        }
    }
}

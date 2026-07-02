import ComposableArchitecture

/// The post-logout beat: a brief "Encerrando sessão…" flash, then the
/// farewell screen with the way back in.
@Reducer
struct FarewellFeature {
    @ObservableState
    struct State: Equatable {
        var firstName: String?
        var keptData = true
        var dataSummary: LocalDataSummary?
        var isFlashing = true
    }

    enum Action: Equatable {
        case task
        case flashFinished
        case signInTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case signIn
        }
    }

    @Dependency(\.continuousClock) var clock

    private let log = Log.scoped("FarewellFeature")

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                guard state.isFlashing else { return .none }
                return .run { send in
                    try await clock.sleep(for: .milliseconds(900))
                    await send(.flashFinished, animation: .easeInOut(duration: 0.35))
                }

            case .flashFinished:
                state.isFlashing = false
                return .none

            case .signInTapped:
                return .send(.delegate(.signIn))

            case .delegate:
                return .none
            }
        }
    }
}

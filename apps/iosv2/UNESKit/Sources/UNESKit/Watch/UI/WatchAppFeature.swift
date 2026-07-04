#if os(watchOS)
import ComposableArchitecture
import Foundation

/// The whole watch app: one snapshot observation feeding every screen, and
/// value-routed navigation on the host NavigationStack.
@Reducer
struct WatchAppFeature {
    @ObservableState
    struct State: Equatable {
        /// nil after load means signed out (or the phone never pushed).
        var snapshot: WatchSnapshot?
        /// The observation replays the store immediately; until then, spinner.
        var hasLoaded = false
        var path: [Route] = []
    }

    enum Route: Equatable, Hashable {
        case week
        case discipline(String)
    }

    enum Action: Equatable, BindableAction {
        case binding(BindingAction<State>)
        case task
        case snapshotUpdated(WatchSnapshot?)
        case weekTapped
        case disciplineTapped(String)
    }

    @Dependency(\.watchRepository) var repository

    private let log = Log.scoped("WatchAppFeature")

    private enum CancelID { case observation }

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .binding:
                return .none

            case .task:
                let repository = repository
                return .run { send in
                    repository.activate()
                    for await snapshot in repository.observe() {
                        await send(.snapshotUpdated(snapshot))
                    }
                }
                .cancellable(id: CancelID.observation)

            case let .snapshotUpdated(snapshot):
                if state.snapshot == nil || snapshot == nil {
                    log.info("snapshot updated hasData=\(snapshot != nil)")
                }
                state.hasLoaded = true
                state.snapshot = snapshot
                // Whatever was on the stack may not exist in the new dataset.
                if snapshot == nil { state.path.removeAll() }
                return .none

            case .weekTapped:
                state.path.append(.week)
                return .none

            case let .disciplineTapped(id):
                state.path.append(.discipline(id))
                return .none
            }
        }
    }
}
#endif

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
        /// A content deeplink that arrived before the store's first emission —
        /// newest wins, resolved on the next `snapshotUpdated`.
        var pendingRoute: IntentRoute?
        /// A tapped message the mirror doesn't hold yet; the inbox upgrades to
        /// its detail if the id lands within the back-channel grace window.
        var awaitingMessageId: String?
    }

    enum Route: Equatable, Hashable {
        case week
        case discipline(String)
        case messages
        case message(String)
        case spaceImpact
    }

    enum Action: Equatable, BindableAction {
        case binding(BindingAction<State>)
        case task
        case snapshotUpdated(WatchSnapshot?)
        case deeplink(IntentRoute)
        case deeplinkGraceExpired
        case weekTapped
        case disciplineTapped(String)
        case messagesTapped
        case messageTapped(String)
        case spaceImpactTapped
    }

    @Dependency(\.watchRepository) var repository
    @Dependency(\.intentRouter) var intentRouter
    @Dependency(\.continuousClock) var clock

    private let log = Log.scoped("WatchAppFeature")

    /// How long the inbox waits for the back-channel-refreshed snapshot to
    /// deliver a tapped message before giving up on the detail upgrade.
    private static let messageGraceWindow: Duration = .seconds(15)

    private enum CancelID { case observation, routes, deeplinkGrace }

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .binding:
                return .none

            case .task:
                let repository = repository
                let intentRouter = intentRouter
                // `.task` re-runs on every foreground cycle — without
                // `cancelInFlight` each pass would stack another subscriber.
                return .merge(
                    .run { send in
                        repository.activate()
                        for await snapshot in repository.observe() {
                            await send(.snapshotUpdated(snapshot))
                        }
                    }
                    .cancellable(id: CancelID.observation, cancelInFlight: true),
                    .run { send in
                        for await route in intentRouter.routes() {
                            await send(.deeplink(route))
                        }
                    }
                    .cancellable(id: CancelID.routes, cancelInFlight: true)
                )

            case let .snapshotUpdated(snapshot):
                if state.snapshot == nil || snapshot == nil {
                    log.info("snapshot updated hasData=\(snapshot != nil)")
                }
                state.hasLoaded = true
                state.snapshot = snapshot
                guard let snapshot else {
                    // Signed out — whatever was on the stack, or waiting to
                    // route, no longer exists.
                    state.path.removeAll()
                    state.pendingRoute = nil
                    state.awaitingMessageId = nil
                    return .cancel(id: CancelID.deeplinkGrace)
                }
                var effects: [Effect<Action>] = []
                if let route = state.pendingRoute {
                    state.pendingRoute = nil
                    effects.append(apply(route, &state))
                }
                if let id = state.awaitingMessageId,
                   snapshot.messages.contains(where: { $0.id == id }) {
                    state.awaitingMessageId = nil
                    log.info("deeplink message upgrade after refresh")
                    state.path = [.messages, .message(id)]
                    effects.append(markReadEffect(id, state))
                    effects.append(.cancel(id: CancelID.deeplinkGrace))
                }
                return effects.isEmpty ? .none : .merge(effects)

            case let .deeplink(route):
                if state.hasLoaded, state.snapshot == nil {
                    // Signed out — same drop `Deeplinks.post` does on the
                    // phone, just at consumption time.
                    log.info("deeplink dropped signed-out kind=\(route.kindLabel)")
                    return .none
                }
                if !state.hasLoaded, WatchDeeplinkResolver.needsSnapshot(route) {
                    state.pendingRoute = route
                    return .none
                }
                return apply(route, &state)

            case .deeplinkGraceExpired:
                // The user is on the inbox; it re-renders reactively if the
                // message still arrives later.
                if state.awaitingMessageId != nil {
                    log.info("deeplink message grace expired")
                    state.awaitingMessageId = nil
                }
                return .none

            case .weekTapped:
                state.path.append(.week)
                return .none

            case let .disciplineTapped(id):
                state.path.append(.discipline(id))
                return .none

            case .messagesTapped:
                state.path.append(.messages)
                return .none

            case .spaceImpactTapped:
                state.path.append(.spaceImpact)
                return .none

            case let .messageTapped(id):
                state.path.append(.message(id))
                return markReadEffect(id, state)
            }
        }
    }

    private func apply(_ route: IntentRoute, _ state: inout State) -> Effect<Action> {
        let destination = WatchDeeplinkResolver.resolve(route, snapshot: state.snapshot)
        log.info("deeplink consumed kind=\(route.kindLabel)")
        switch destination {
        case .root:
            state.path.removeAll()
            return .none
        case .week:
            state.path = [.week]
            return .none
        case .messages:
            state.path = [.messages]
            guard case let .message(id) = route else { return .none }
            // The announced message isn't mirrored yet — the push that
            // mirrored to the watch is the same one the locked phone hasn't
            // synced. Wake the phone for a messages refresh and upgrade to
            // the detail if the id lands within the grace window.
            state.awaitingMessageId = id
            let repository = repository
            let clock = clock
            return .merge(
                .run { _ in repository.requestSnapshot() },
                .run { send in
                    try await clock.sleep(for: Self.messageGraceWindow)
                    await send(.deeplinkGraceExpired)
                }
                .cancellable(id: CancelID.deeplinkGrace, cancelInFlight: true)
            )
        case let .message(id):
            state.path = [.messages, .message(id)]
            return markReadEffect(id, state)
        case let .discipline(id):
            state.path = [.discipline(id)]
            return .none
        }
    }

    /// The read receipt for an opened message; the caller sets the path.
    private func markReadEffect(_ id: String, _ state: State) -> Effect<Action> {
        guard state.snapshot?.messages.first(where: { $0.id == id })?.unread == true else {
            return .none
        }
        log.info("message opened markRead")
        let repository = repository
        return .run { _ in await repository.markMessageRead(id: id) }
    }
}
#endif

import ComposableArchitecture
import Foundation

@Reducer
struct MessagesFeature {
    @ObservableState
    struct State: Equatable {
        var overview: MessagesOverview?
        var filter: MessageFilter = .all
        var isLoading = false
        var errorMessage: String?
        var path = StackState<Path.State>()
    }

    @Reducer
    enum Path {
        case detail(MessageDetailFeature)
    }

    enum Action: Equatable {
        case task
        case refreshPulled
        case overviewUpdated(MessagesOverview)
        case refreshFailed(String)
        case filterSelected(MessageFilter)
        case markAllReadTapped
        case messageTapped(MessageItem)
        case path(StackActionOf<Path>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case unreadChanged(Int)
        }
    }

    @Dependency(\.messagesRepository) var messagesRepository
    @Dependency(\.date.now) var now

    private enum CancelID { case observation, refresh }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                // The observation replays the mirror on subscription, so
                // every appearance rehydrates; the refresh keeps retrying on
                // each appearance until the mirror has data, so a first load
                // cancelled mid-flight (tab switch) can't wedge the spinner.
                guard state.overview == nil else { return observeMirror() }
                state.isLoading = true
                state.errorMessage = nil
                return .merge(observeMirror(), refresh())

            case .refreshPulled:
                guard !state.isLoading else { return .none }
                if state.overview == nil {
                    state.isLoading = true
                    state.errorMessage = nil
                }
                return refresh()

            case let .overviewUpdated(overview):
                state.isLoading = false
                state.errorMessage = nil
                state.overview = overview
                return .send(.delegate(.unreadChanged(overview.unreadCount)))

            case let .refreshFailed(message):
                state.isLoading = false
                // A stale inbox beats an error screen; only surface the
                // failure when there is nothing to show.
                if state.overview == nil {
                    state.errorMessage = message
                }
                return .none

            case let .filterSelected(filter):
                state.filter = filter
                return .none

            case .markAllReadTapped:
                guard var overview = state.overview, overview.unreadCount > 0 else { return .none }
                // Optimistic: the digest settles instantly; the write's own
                // mirror emission then confirms (or corrects) it.
                for index in overview.messages.indices {
                    overview.messages[index].unread = false
                }
                state.overview = overview
                return .merge(
                    .send(.delegate(.unreadChanged(0))),
                    .run { _ in try? await messagesRepository.markAllRead(now: now) }
                )

            case let .messageTapped(message):
                var opened = message
                opened.unread = false
                if let index = state.overview?.messages.firstIndex(where: { $0.id == message.id }) {
                    state.overview?.messages[index] = opened
                }
                state.path.append(.detail(MessageDetailFeature.State(message: opened)))
                guard message.unread else { return .none }
                return .merge(
                    .send(.delegate(.unreadChanged(state.overview?.unreadCount ?? 0))),
                    .run { _ in try? await messagesRepository.markRead(id: message.id, now: now) }
                )

            case let .path(.element(id: _, action: .detail(.delegate(.starredChanged(id, starred))))):
                if let index = state.overview?.messages.firstIndex(where: { $0.id == id }) {
                    state.overview?.messages[index].starred = starred
                }
                return .none

            case .path:
                return .none

            case .delegate:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    /// The reactive backbone: every mirror write (sync refresh from any tab,
    /// read/star overlays) lands here as a fresh inbox.
    private func observeMirror() -> Effect<Action> {
        .run { send in
            for await overview in messagesRepository.observe() {
                await send(.overviewUpdated(overview))
            }
        }
        .cancellable(id: CancelID.observation, cancelInFlight: true)
    }

    /// Rewrites the mirror from upstream; the fresh inbox arrives through
    /// the observation.
    private func refresh() -> Effect<Action> {
        .run { send in
            do {
                try await messagesRepository.refresh(now: now)
            } catch {
                // Offline with a mirror: keep serving the local inbox.
                if let cached = try? await messagesRepository.cached(now: now) {
                    await send(.overviewUpdated(cached))
                } else {
                    await send(.refreshFailed(error.localizedDescription))
                }
            }
        }
        .cancellable(id: CancelID.refresh, cancelInFlight: true)
    }
}

extension MessagesFeature.Path.State: Equatable {}
extension MessagesFeature.Path.Action: Equatable {}

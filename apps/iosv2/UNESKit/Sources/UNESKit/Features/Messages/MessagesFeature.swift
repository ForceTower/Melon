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
        case hydrated(MessagesOverview)
        case overviewLoaded(MessagesOverview)
        case overviewFailed(String)
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

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                guard state.overview == nil, !state.isLoading else { return .none }
                state.isLoading = true
                state.errorMessage = nil
                return hydrateThenRefresh()

            case .refreshPulled:
                guard !state.isLoading else { return .none }
                if state.overview == nil {
                    state.isLoading = true
                    state.errorMessage = nil
                }
                return refresh()

            case let .hydrated(overview), let .overviewLoaded(overview):
                state.isLoading = false
                state.errorMessage = nil
                state.overview = overview
                return .send(.delegate(.unreadChanged(overview.unreadCount)))

            case let .overviewFailed(message):
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
                // Optimistic: the digest settles instantly; a failed write is
                // corrected by the next hydrate.
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

    /// Stale-while-revalidate: hydrate instantly from the mirror (no spinner
    /// when data exists), then refresh over the network.
    private func hydrateThenRefresh() -> Effect<Action> {
        .run { send in
            if let cached = try? await messagesRepository.cached(now: now) {
                await send(.hydrated(cached))
            }
            do {
                await send(.overviewLoaded(try await messagesRepository.refresh(now: now)))
            } catch {
                await send(.overviewFailed(error.localizedDescription))
            }
        }
    }

    private func refresh() -> Effect<Action> {
        .run { send in
            do {
                await send(.overviewLoaded(try await messagesRepository.refresh(now: now)))
            } catch {
                // Offline with a mirror: keep serving the local inbox.
                if let cached = try? await messagesRepository.cached(now: now) {
                    await send(.hydrated(cached))
                } else {
                    await send(.overviewFailed(error.localizedDescription))
                }
            }
        }
    }
}

extension MessagesFeature.Path.State: Equatable {}
extension MessagesFeature.Path.Action: Equatable {}

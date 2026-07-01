import ComposableArchitecture
import Foundation

@Reducer
struct HomeFeature {
    @ObservableState
    struct State: Equatable {
        var overview: HomeOverview?
        var userName: String?
        var isLoading = false
        var errorMessage: String?
        var lastRefreshed: Date?
    }

    enum Action: Equatable {
        case task
        case refreshPulled
        case hydrated(CachedHomeOverview)
        case overviewLoaded(HomeOverview)
        case overviewFailed(String)
        case profileLoaded(Profile)
        case seeScheduleTapped
        case seeAllClassesTapped
        case messagesWidgetTapped
        case avatarTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openSchedule
            case openClasses
            case openMessages
            case openMe
            case unreadMessagesChanged(Int)
        }
    }

    @Dependency(\.homeRepository) var homeRepository
    @Dependency(\.profileRepository) var profileRepository
    @Dependency(\.date.now) var now
    @Dependency(\.continuousClock) var clock

    private enum CancelID { case heroRollover }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                guard state.overview == nil, !state.isLoading else { return .none }
                state.isLoading = true
                state.errorMessage = nil
                return .merge(hydrateThenRefresh(), loadProfile())

            case .refreshPulled:
                guard !state.isLoading else { return .none }
                if state.overview == nil {
                    state.isLoading = true
                    state.errorMessage = nil
                }
                return refresh()

            case let .hydrated(cached):
                state.isLoading = false
                state.errorMessage = nil
                state.overview = cached.overview
                // The mirror's own sync stamp, so "Atualizado há X min"
                // survives relaunches.
                state.lastRefreshed = cached.syncedAt
                return .merge(
                    .send(.delegate(.unreadMessagesChanged(cached.overview.messages?.unreadCount ?? 0))),
                    scheduleHeroRollover(cached.overview)
                )

            case let .overviewLoaded(overview):
                state.isLoading = false
                state.errorMessage = nil
                state.overview = overview
                state.lastRefreshed = now
                return .merge(
                    .send(.delegate(.unreadMessagesChanged(overview.messages?.unreadCount ?? 0))),
                    scheduleHeroRollover(overview)
                )

            case let .overviewFailed(message):
                state.isLoading = false
                // A stale overview beats an error screen; only surface the
                // failure when there is nothing to show.
                if state.overview == nil {
                    state.errorMessage = message
                }
                return .none

            case let .profileLoaded(profile):
                state.userName = profile.name
                return .none

            case .seeScheduleTapped:
                return .send(.delegate(.openSchedule))

            case .seeAllClassesTapped:
                return .send(.delegate(.openClasses))

            case .messagesWidgetTapped:
                return .send(.delegate(.openMessages))

            case .avatarTapped:
                return .send(.delegate(.openMe))

            case .delegate:
                return .none
            }
        }
    }

    /// Stale-while-revalidate: hydrate instantly from the mirror (no spinner
    /// when data exists), then refresh over the network.
    private func hydrateThenRefresh() -> Effect<Action> {
        .run { send in
            if let cached = try? await homeRepository.cached(now: now) {
                await send(.hydrated(cached))
            }
            do {
                await send(.overviewLoaded(try await homeRepository.refresh(now: now)))
            } catch {
                await send(.overviewFailed(error.localizedDescription))
            }
        }
    }

    private func refresh() -> Effect<Action> {
        .run { send in
            do {
                await send(.overviewLoaded(try await homeRepository.refresh(now: now)))
            } catch {
                // Offline with a mirror: recompute from local data so the
                // time-derived pieces (hero, "Seu dia") still advance.
                if let cached = try? await homeRepository.cached(now: now) {
                    await send(.hydrated(cached))
                } else {
                    await send(.overviewFailed(error.localizedDescription))
                }
            }
        }
    }

    /// Once the hero class starts, its countdown is spent — reload so the
    /// hero advances to the next occurrence.
    private func scheduleHeroRollover(_ overview: HomeOverview) -> Effect<Action> {
        guard let startsAt = overview.hero?.startsAt else { return .none }
        let delay = startsAt.timeIntervalSince(now) + 1
        guard delay > 0 else { return .none }
        return .run { send in
            try await clock.sleep(for: .seconds(delay))
            await send(.refreshPulled)
        }
        .cancellable(id: CancelID.heroRollover, cancelInFlight: true)
    }

    private func loadProfile() -> Effect<Action> {
        .run { send in
            // Only feeds the avatar initial — ignore failures.
            guard let profile = try? await profileRepository.current() else { return }
            await send(.profileLoaded(profile))
        }
    }
}

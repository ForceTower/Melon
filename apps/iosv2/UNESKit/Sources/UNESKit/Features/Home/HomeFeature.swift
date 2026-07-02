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
        var path = StackState<Path.State>()
    }

    @Reducer
    enum Path {
        case detail(DisciplineDetailFeature)
    }

    enum Action: Equatable {
        case task
        case refreshPulled
        case mirrorUpdated(CachedHomeOverview)
        case refreshFailed(String)
        case profileLoaded(Profile)
        case disciplineTapped(id: String, name: String)
        case seeScheduleTapped
        case seeAllClassesTapped
        case messagesWidgetTapped
        case avatarTapped
        case path(StackActionOf<Path>)
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

    private enum CancelID { case observation, refresh, heroRollover }

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
                return .merge(observeMirror(), refresh(), loadProfile())

            case .refreshPulled:
                guard !state.isLoading else { return .none }
                if state.overview == nil {
                    state.isLoading = true
                    state.errorMessage = nil
                }
                return refresh()

            case let .mirrorUpdated(cached):
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

            case let .refreshFailed(message):
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

            case let .disciplineTapped(id, name):
                guard let overview = state.overview, let semesterId = overview.semesterId else { return .none }
                let colorIndex = overview.disciplines.first { $0.id == id }?.colorIndex ?? 0
                state.path.append(.detail(DisciplineDetailFeature.State(
                    semesterId: semesterId,
                    disciplineId: id,
                    name: name,
                    colorIndex: colorIndex
                )))
                return .none

            case .seeScheduleTapped:
                return .send(.delegate(.openSchedule))

            case .seeAllClassesTapped:
                return .send(.delegate(.openClasses))

            case .messagesWidgetTapped:
                return .send(.delegate(.openMessages))

            case .avatarTapped:
                return .send(.delegate(.openMe))

            case .path:
                return .none

            case .delegate:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    /// The reactive backbone: every mirror write (sync refresh, message
    /// read/star, semester download — from any tab) lands here as a fresh
    /// overview.
    private func observeMirror() -> Effect<Action> {
        .run { send in
            for await cached in homeRepository.observe() {
                await send(.mirrorUpdated(cached))
            }
        }
        .cancellable(id: CancelID.observation, cancelInFlight: true)
    }

    /// Rewrites the mirror from upstream; the fresh snapshot arrives through
    /// the observation.
    private func refresh() -> Effect<Action> {
        .run { send in
            do {
                try await homeRepository.refresh(now: now)
            } catch {
                // Offline with a mirror: recompute from local data so the
                // time-derived pieces (hero, "Seu dia") still advance.
                if let cached = try? await homeRepository.cached(now: now) {
                    await send(.mirrorUpdated(cached))
                } else {
                    await send(.refreshFailed(error.localizedDescription))
                }
            }
        }
        .cancellable(id: CancelID.refresh, cancelInFlight: true)
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

extension HomeFeature.Path.State: Equatable {}
extension HomeFeature.Path.Action: Equatable {}

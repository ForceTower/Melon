import ComposableArchitecture
import Foundation

@Reducer
struct DisciplinesFeature {
    @ObservableState
    struct State: Equatable {
        var overview: DisciplinesOverview?
        var isLoading = false
        var errorMessage: String?
        var downloadingSemesterIds: Set<String> = []
        /// Semesters pulled through the "Baixar" card this session — their
        /// history group starts expanded.
        var recentlyDownloadedIds: Set<String> = []
        var path = StackState<Path.State>()
        @Presents var alert: AlertState<Never>?
    }

    @Reducer
    enum Path {
        case detail(DisciplineDetailFeature)
    }

    enum Action: Equatable {
        case task
        case refreshPulled
        case overviewUpdated(DisciplinesOverview)
        case overviewFailed(String)
        case downloadSemesterTapped(String)
        case semesterDownloaded(String)
        case semesterDownloadFailed(String, String)
        case disciplineTapped(semesterId: String, discipline: DisciplineSummary)
        case path(StackActionOf<Path>)
        case alert(PresentationAction<Never>)
    }

    @Dependency(\.disciplinesRepository) var disciplinesRepository
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
                return .none

            case let .overviewFailed(message):
                state.isLoading = false
                // A stale overview beats an error screen; only surface the
                // failure when there is nothing to show.
                if state.overview == nil {
                    state.errorMessage = message
                }
                return .none

            case let .downloadSemesterTapped(semesterId):
                guard !state.downloadingSemesterIds.contains(semesterId) else { return .none }
                state.downloadingSemesterIds.insert(semesterId)
                return .run { send in
                    do {
                        try await disciplinesRepository.downloadSemester(semesterId: semesterId, now: now)
                        await send(.semesterDownloaded(semesterId))
                    } catch {
                        await send(.semesterDownloadFailed(semesterId, error.localizedDescription))
                    }
                }

            case let .semesterDownloaded(semesterId):
                // The expanded overview arrives through the observation.
                state.downloadingSemesterIds.remove(semesterId)
                state.recentlyDownloadedIds.insert(semesterId)
                return .none

            case let .semesterDownloadFailed(semesterId, message):
                state.downloadingSemesterIds.remove(semesterId)
                state.alert = AlertState {
                    TextState("Não deu para baixar o semestre")
                } message: {
                    TextState(message)
                }
                return .none

            case let .disciplineTapped(semesterId, discipline):
                state.path.append(
                    .detail(DisciplineDetailFeature.State(summary: discipline, semesterId: semesterId))
                )
                return .none

            case .path, .alert:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
        .ifLet(\.$alert, action: \.alert)
    }

    /// The reactive backbone: every mirror write (sync refresh, semester
    /// download — from any tab) lands here as a fresh overview.
    private func observeMirror() -> Effect<Action> {
        .run { send in
            for await overview in disciplinesRepository.observe() {
                await send(.overviewUpdated(overview))
            }
        }
        .cancellable(id: CancelID.observation, cancelInFlight: true)
    }

    /// Rewrites the mirror from upstream; the fresh overview arrives through
    /// the observation.
    private func refresh() -> Effect<Action> {
        .run { send in
            do {
                try await disciplinesRepository.refresh(now: now)
            } catch {
                // Offline with a mirror: recompute from local data so the
                // time-derived pieces (countdowns, status) still advance.
                if let cached = try? await disciplinesRepository.cached(now: now) {
                    await send(.overviewUpdated(cached))
                } else {
                    await send(.overviewFailed(error.localizedDescription))
                }
            }
        }
        .cancellable(id: CancelID.refresh, cancelInFlight: true)
    }
}

extension DisciplinesFeature.Path.State: Equatable {}
extension DisciplinesFeature.Path.Action: Equatable {}

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
        case hydrated(DisciplinesOverview)
        case overviewLoaded(DisciplinesOverview)
        case overviewFailed(String)
        case downloadSemesterTapped(String)
        case semesterDownloaded(String, DisciplinesOverview)
        case semesterDownloadFailed(String, String)
        case disciplineTapped(semesterId: String, discipline: DisciplineSummary)
        case path(StackActionOf<Path>)
        case alert(PresentationAction<Never>)
    }

    @Dependency(\.disciplinesRepository) var disciplinesRepository
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
                        let overview = try await disciplinesRepository.downloadSemester(
                            semesterId: semesterId,
                            now: now
                        )
                        await send(.semesterDownloaded(semesterId, overview))
                    } catch {
                        await send(.semesterDownloadFailed(semesterId, error.localizedDescription))
                    }
                }

            case let .semesterDownloaded(semesterId, overview):
                state.downloadingSemesterIds.remove(semesterId)
                state.recentlyDownloadedIds.insert(semesterId)
                state.overview = overview
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

    /// Stale-while-revalidate: hydrate instantly from the mirror (no spinner
    /// when data exists), then refresh over the network.
    private func hydrateThenRefresh() -> Effect<Action> {
        .run { send in
            if let cached = try? await disciplinesRepository.cached(now: now) {
                await send(.hydrated(cached))
            }
            do {
                await send(.overviewLoaded(try await disciplinesRepository.refresh(now: now)))
            } catch {
                await send(.overviewFailed(error.localizedDescription))
            }
        }
    }

    private func refresh() -> Effect<Action> {
        .run { send in
            do {
                await send(.overviewLoaded(try await disciplinesRepository.refresh(now: now)))
            } catch {
                // Offline with a mirror: recompute from local data so the
                // time-derived pieces (countdowns, status) still advance.
                if let cached = try? await disciplinesRepository.cached(now: now) {
                    await send(.hydrated(cached))
                } else {
                    await send(.overviewFailed(error.localizedDescription))
                }
            }
        }
    }
}

extension DisciplinesFeature.Path.State: Equatable {}
extension DisciplinesFeature.Path.Action: Equatable {}

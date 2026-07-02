import ComposableArchitecture
import Foundation

@Reducer
struct ScheduleFeature {
    @ObservableState
    struct State: Equatable {
        var overview: ScheduleOverview?
        /// Day pinned by a week-strip tap; nil follows today.
        var selectedIndex: Int?
        var isLoading = false
        var errorMessage: String?
        var path = StackState<Path.State>()
    }

    @Reducer
    enum Path {
        case detail(DisciplineDetailFeature)
    }

    enum Action: Equatable {
        case task
        case refreshPulled
        case hydrated(ScheduleOverview)
        case overviewLoaded(ScheduleOverview)
        case overviewFailed(String)
        case daySelected(Int)
        case todayTapped
        case classTapped(ScheduleClass)
        case path(StackActionOf<Path>)
    }

    @Dependency(\.scheduleRepository) var scheduleRepository
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
                // A stale week beats an error screen; only surface the
                // failure when there is nothing to show.
                if state.overview == nil {
                    state.errorMessage = message
                }
                return .none

            case let .daySelected(index):
                // Picking today's own column just resumes following today.
                state.selectedIndex = index == state.overview?.todayIndex(now: now) ? nil : index
                return .none

            case .todayTapped:
                state.selectedIndex = nil
                return .none

            case let .classTapped(scheduleClass):
                guard let semesterId = state.overview?.semesterId else { return .none }
                state.path.append(
                    .detail(DisciplineDetailFeature.State(
                        semesterId: semesterId,
                        disciplineId: scheduleClass.disciplineId,
                        name: scheduleClass.title,
                        colorIndex: scheduleClass.colorIndex
                    ))
                )
                return .none

            case .path:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    /// Stale-while-revalidate: hydrate instantly from the mirror (no spinner
    /// when data exists), then refresh over the network.
    private func hydrateThenRefresh() -> Effect<Action> {
        .run { send in
            if let cached = try? await scheduleRepository.cached(now: now) {
                await send(.hydrated(cached))
            }
            do {
                await send(.overviewLoaded(try await scheduleRepository.refresh(now: now)))
            } catch {
                await send(.overviewFailed(error.localizedDescription))
            }
        }
    }

    private func refresh() -> Effect<Action> {
        .run { send in
            do {
                await send(.overviewLoaded(try await scheduleRepository.refresh(now: now)))
            } catch {
                // Offline with a mirror: recompute from local data so the
                // week's dates and topics still track the calendar.
                if let cached = try? await scheduleRepository.cached(now: now) {
                    await send(.hydrated(cached))
                } else {
                    await send(.overviewFailed(error.localizedDescription))
                }
            }
        }
    }
}

extension ScheduleFeature.Path.State: Equatable {}
extension ScheduleFeature.Path.Action: Equatable {}

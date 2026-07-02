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
        case overviewUpdated(ScheduleOverview)
        case refreshFailed(String)
        case daySelected(Int)
        case todayTapped
        case classTapped(ScheduleClass)
        case path(StackActionOf<Path>)
    }

    @Dependency(\.scheduleRepository) var scheduleRepository
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

            case let .refreshFailed(message):
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

    /// The reactive backbone: every mirror write (sync refresh, semester
    /// download — from any tab) lands here as a fresh week.
    private func observeMirror() -> Effect<Action> {
        .run { send in
            for await overview in scheduleRepository.observe() {
                await send(.overviewUpdated(overview))
            }
        }
        .cancellable(id: CancelID.observation, cancelInFlight: true)
    }

    /// Rewrites the mirror from upstream; the fresh week arrives through the
    /// observation.
    private func refresh() -> Effect<Action> {
        .run { send in
            do {
                try await scheduleRepository.refresh(now: now)
            } catch {
                // Offline with a mirror: recompute from local data so the
                // week's dates and topics still track the calendar.
                if let cached = try? await scheduleRepository.cached(now: now) {
                    await send(.overviewUpdated(cached))
                } else {
                    await send(.refreshFailed(error.localizedDescription))
                }
            }
        }
        .cancellable(id: CancelID.refresh, cancelInFlight: true)
    }
}

extension ScheduleFeature.Path.State: Equatable {}
extension ScheduleFeature.Path.Action: Equatable {}

import ComposableArchitecture
import Foundation

/// The week-grid variant of Horário: the whole week on one proportional time
/// grid with an agenda list beneath it. Sibling of `ScheduleFeature` — a
/// settings flag swaps the two screens at runtime, so each keeps its own
/// state, reducer, and navigation stack.
@Reducer
struct ScheduleGridFeature {
    @ObservableState
    struct State: Equatable {
        var overview: ScheduleOverview?
        var isLoading = false
        var errorMessage: String?
        /// Class opened in the bottom detail sheet.
        var sheet: SheetItem?
        var path = StackState<Path.State>()
    }

    /// The tapped class plus the weekday it sits on, for the sheet header.
    struct SheetItem: Equatable, Identifiable {
        var scheduleClass: ScheduleClass
        /// Monday-first index into the overview's days.
        var dayIndex: Int

        var id: String { scheduleClass.id }
    }

    @Reducer
    enum Path {
        case detail(DisciplineDetailFeature)
        case materialsList(MaterialsListFeature)
        case materialsDetail(MaterialsDetailFeature)
    }

    enum Action: Equatable {
        case task
        case refreshPulled
        case overviewUpdated(ScheduleOverview)
        case refreshFailed(String)
        case classTapped(ScheduleClass, dayIndex: Int)
        case sheetDismissed
        case sheetDisciplineTapped
        case path(StackActionOf<Path>)
    }

    @Dependency(\.scheduleRepository) var scheduleRepository
    @Dependency(\.date.now) var now
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("ScheduleGridFeature")

    private enum CancelID { case observation, refresh }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                // Same appearance contract as ScheduleFeature: the observation
                // replays the mirror on subscription, and the refresh keeps
                // retrying on each appearance until the mirror has data.
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
                log.warn("schedule grid refresh failed err=\(message)")
                state.isLoading = false
                // A stale week beats an error screen; only surface the
                // failure when there is nothing to show.
                if state.overview == nil {
                    state.errorMessage = message
                }
                return .none

            case let .classTapped(scheduleClass, dayIndex):
                state.sheet = SheetItem(scheduleClass: scheduleClass, dayIndex: dayIndex)
                return .none

            case .sheetDismissed:
                state.sheet = nil
                return .none

            case .sheetDisciplineTapped:
                guard let item = state.sheet, let semesterId = state.overview?.semesterId else { return .none }
                state.sheet = nil
                let scheduleClass = item.scheduleClass
                if let offerId = scheduleClass.offerId {
                    analytics.selectContent(
                        contentType: ContentTypes.discipline,
                        itemId: offerId,
                        properties: ["code": scheduleClass.code]
                    )
                }
                state.path.append(
                    .detail(DisciplineDetailFeature.State(
                        semesterId: semesterId,
                        disciplineId: scheduleClass.disciplineId,
                        name: scheduleClass.title,
                        colorIndex: scheduleClass.colorIndex
                    ))
                )
                return .none

            case let .path(.element(id: _, action: pathAction)):
                return routeMaterials(pathAction, state: &state)

            case .path:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    /// Discipline detail's Materiais entry pushes the shelf (and its
    /// material screens) on this stack.
    private func routeMaterials(_ action: Path.Action, state: inout State) -> Effect<Action> {
        switch action {
        case let .detail(.delegate(.openMaterials(discipline))):
            state.path.append(.materialsList(MaterialsListFeature.State(discipline: discipline)))
        case let .materialsList(.delegate(.openMaterial(material, _))):
            state.path.append(.materialsDetail(MaterialsDetailFeature.State(material: material)))
        default:
            break
        }
        return .none
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
        .run { [log] send in
            do {
                try await scheduleRepository.refresh(now: now)
            } catch {
                // Offline with a mirror: recompute from local data so the
                // week's dates and topics still track the calendar.
                if let cached = try? await scheduleRepository.cached(now: now) {
                    log.warn("refresh failed, using cached overview", error: error)
                    await send(.overviewUpdated(cached))
                } else {
                    await send(.refreshFailed(error.localizedDescription))
                }
            }
        }
        .cancellable(id: CancelID.refresh, cancelInFlight: true)
    }
}

extension ScheduleGridFeature.Path.State: Equatable {}
extension ScheduleGridFeature.Path.Action: Equatable {}

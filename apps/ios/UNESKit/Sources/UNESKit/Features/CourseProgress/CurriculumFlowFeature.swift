import ComposableArchitecture
import Foundation

/// The fluxograma: the curriculum grid through three lenses — one page per
/// período (default), the whole-course map, and the side-by-side grid — plus
/// the discipline sheet and the prerequisite trail highlight. Receives the
/// mirrored payload from the progress screen, so it renders offline; the
/// observation keeps it current while open.
@Reducer
struct CurriculumFlowFeature {
    enum Lens: String, Equatable, Sendable, CaseIterable {
        case periods, map, grid
    }

    /// The prerequisite chain being highlighted: everything not in `codes`
    /// dims, `focus` gets the ring.
    struct Trail: Equatable, Sendable {
        var focus: String
        var codes: Set<String>
    }

    @ObservableState
    struct State: Equatable {
        var progress: CourseProgress
        var lens: Lens = .periods
        var selectedPeriod: Int
        var trail: Trail?
        /// The discipline whose sheet is open, by code.
        var presentedEntryCode: String?

        init(progress: CourseProgress) {
            self.progress = progress
            selectedPeriod = progress.landingPeriod
        }

        var periods: [CurriculumPeriod] { progress.scheduledPeriods }

        var selectedPeriodEntries: CurriculumPeriod? {
            progress.period(selectedPeriod)
        }

        var presentedEntry: CurriculumEntry? {
            presentedEntryCode.flatMap(progress.entry)
        }
    }

    enum Action: Equatable {
        case task
        case progressUpdated(CourseProgress?)
        case lensChanged(Lens)
        case periodSelected(Int)
        case entryTapped(String)
        case entrySheetDismissed
        case trailRequested(String)
        case trailCleared
    }

    @Dependency(\.courseProgressRepository) var courseProgressRepository
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("CurriculumFlowFeature")

    private enum CancelID { case observation }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(Screens.curriculumFlow)
                return .run { send in
                    for await progress in courseProgressRepository.observe() {
                        await send(.progressUpdated(progress))
                    }
                }
                .cancellable(id: CancelID.observation, cancelInFlight: true)

            case let .progressUpdated(progress):
                // A refresh that dropped the curriculum keeps the pushed
                // payload — blanking an open grid helps no one.
                guard let progress, progress.hasCurriculum, progress != state.progress else { return .none }
                state.progress = progress
                if progress.period(state.selectedPeriod) == nil {
                    state.selectedPeriod = progress.landingPeriod
                }
                if let trail = state.trail, progress.entry(trail.focus) == nil {
                    state.trail = nil
                }
                return .none

            case let .lensChanged(lens):
                guard lens != state.lens else { return .none }
                analytics.selectContent(contentType: ContentTypes.tile, itemId: "curriculum_lens", properties: ["lens": lens.rawValue])
                state.lens = lens
                return .none

            case let .periodSelected(period):
                guard state.progress.period(period) != nil else { return .none }
                state.selectedPeriod = period
                // Jumping to a período from the map is a request to read
                // its names.
                if state.lens == .map {
                    state.lens = .periods
                }
                return .none

            case let .entryTapped(code):
                guard state.progress.entry(code) != nil else { return .none }
                analytics.selectContent(contentType: ContentTypes.discipline, itemId: code, properties: ["source": "curriculum_flow"])
                state.presentedEntryCode = code
                return .none

            case .entrySheetDismissed:
                state.presentedEntryCode = nil
                return .none

            case let .trailRequested(code):
                guard state.progress.entry(code) != nil else { return .none }
                log.info("show trail code=\(code)")
                analytics.selectContent(contentType: ContentTypes.tile, itemId: "curriculum_trail", properties: ["code": code])
                state.trail = Trail(focus: code, codes: state.progress.trail(through: code))
                state.presentedEntryCode = nil
                state.lens = .map
                return .none

            case .trailCleared:
                state.trail = nil
                return .none
            }
        }
    }
}

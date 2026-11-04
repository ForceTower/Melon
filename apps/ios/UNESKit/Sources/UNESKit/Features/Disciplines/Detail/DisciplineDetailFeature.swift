import ComposableArchitecture
import Foundation

@Reducer
struct DisciplineDetailFeature {
    @ObservableState
    struct State: Equatable {
        let semesterId: String
        let disciplineId: String
        /// Seeded from the tapped card so the toolbar title and tint are
        /// right before the mirror read lands.
        var name: String
        var colorIndex: Int
        var detail: DisciplineDetail?
        /// Selected group code; nil renders every group ("Tudo").
        var selectedGroup: String?
        /// The discipline's Materiais shelf entry — nil (card hidden) until
        /// the count lands, and forever for past-semester disciplines, which
        /// the materials overview doesn't cover.
        var materials: MaterialsDiscipline?
        /// The student's own entries tagged with this discipline, pending
        /// first.
        var tasks: [PersonalEvent] = []
        /// Resolved against the mirrored overview on `task`. Only the running
        /// semester takes new tasks — the composer's class picker offers
        /// nothing else.
        var isCurrentSemester = false
        var composerOptions: [PersonalEventDisciplineOption] = []
        /// Local midnight the due labels count from.
        var today = Date.distantPast
        @Presents var composer: CalendarPersonalEventFeature.State?
        @Shared(.appStorage(FeatureFlags.materialsEnabledKey)) var isMaterialsEnabled = false

        /// A past discipline keeps the section only for what it already has.
        var showsTasks: Bool { isCurrentSemester || !tasks.isEmpty }

        /// Debug builds skip the flag so the flow stays reachable.
        var showsMaterials: Bool {
            #if DEBUG
            true
            #else
            isMaterialsEnabled
            #endif
        }

        init(semesterId: String, disciplineId: String, name: String, colorIndex: Int) {
            self.semesterId = semesterId
            self.disciplineId = disciplineId
            self.name = name
            self.colorIndex = colorIndex
        }

        init(summary: DisciplineSummary, semesterId: String) {
            self.init(
                semesterId: semesterId,
                disciplineId: summary.id,
                name: summary.name,
                colorIndex: summary.colorIndex
            )
        }
    }

    enum Action: Equatable {
        case task
        case detailUpdated(DisciplineDetail)
        case groupSelected(String?)
        case countdownTapped
        case materialsLoaded(MaterialsDiscipline)
        case materialsTapped
        case personalEventsUpdated([PersonalEvent])
        case semesterResolved(isCurrent: Bool, options: [PersonalEventDisciplineOption])
        case taskToggled(PersonalEvent)
        case taskTapped(PersonalEvent)
        case addTaskTapped
        case composer(PresentationAction<CalendarPersonalEventFeature.Action>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            /// The parent stack pushes the calculator seeded from this
            /// discipline.
            case openCountdown
            /// The parent stack pushes the discipline's materials shelf.
            case openMaterials(MaterialsDiscipline)
        }
    }

    @Dependency(\.disciplinesRepository) var disciplinesRepository
    @Dependency(\.materialsRepository) var materialsRepository
    @Dependency(\.personalEventsRepository) var personalEventsRepository
    @Dependency(\.analytics) var analytics
    @Dependency(\.calendar) var calendar
    @Dependency(\.date.now) var now

    private let log = Log.scoped("DisciplineDetailFeature")

    private enum CancelID { case observation, materials, personalEvents }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                // The iOS detail merges a discipline's offers, so it carries a
                // discipline id — label it honestly ("discipline_id", the key
                // materials_discipline already uses) instead of polluting the
                // offer_id namespace.
                analytics.screen(name: Screens.disciplineDetail, properties: ["discipline_id": state.disciplineId])
                state.today = calendar.startOfDay(for: now)
                // The observation replays the mirror on subscription, so
                // every appearance rehydrates and later writes (sync
                // refreshes from any tab) land live.
                let observation: Effect<Action> = .run { [semesterId = state.semesterId, disciplineId = state.disciplineId] send in
                    for await detail in disciplinesRepository.observeDetail(
                        semesterId: semesterId,
                        disciplineId: disciplineId
                    ) {
                        await send(.detailUpdated(detail))
                    }
                }
                .cancellable(id: CancelID.observation, cancelInFlight: true)
                let tasks: Effect<Action> = .merge(
                    .run { send in
                        for await events in personalEventsRepository.observe() {
                            await send(.personalEventsUpdated(events))
                        }
                    }
                    .cancellable(id: CancelID.personalEvents, cancelInFlight: true),
                    .run { [semesterId = state.semesterId, now] send in
                        let overview = try? await disciplinesRepository.cached(now)
                        await send(.semesterResolved(
                            isCurrent: overview?.current?.id == semesterId,
                            options: overview?.composerOptions ?? []
                        ))
                    }
                )
                guard state.showsMaterials, state.materials == nil else { return .merge(observation, tasks) }
                return .merge(observation, tasks, loadMaterials(disciplineId: state.disciplineId))

            case let .detailUpdated(detail):
                state.detail = detail
                state.name = detail.name
                state.colorIndex = detail.colorIndex
                return .none

            case let .groupSelected(code):
                state.selectedGroup = code
                return .none

            case .countdownTapped:
                return .send(.delegate(.openCountdown))

            case var .materialsLoaded(materials):
                // The shelf inherits this screen's tint, not the hub's
                // position-based one.
                materials.colorIndex = state.colorIndex
                state.materials = materials
                return .none

            case .materialsTapped:
                guard let materials = state.materials else { return .none }
                analytics.selectContent(contentType: ContentTypes.material, itemId: state.disciplineId)
                log.info("open materials id=\(materials.id)")
                return .send(.delegate(.openMaterials(materials)))

            case let .personalEventsUpdated(events):
                state.tasks = DisciplineTasks.tasks(for: state.disciplineId, in: events)
                return .none

            case let .semesterResolved(isCurrent, options):
                state.isCurrentSemester = isCurrent
                state.composerOptions = options
                return .none

            case let .taskToggled(event):
                let completed = !event.isCompleted
                analytics.selectContent(
                    contentType: ContentTypes.calendarEvent,
                    itemId: event.id,
                    properties: ["action": completed ? "complete" : "reopen"]
                )
                return .run { [log] _ in
                    do {
                        try await personalEventsRepository.setCompleted(event.id, completed)
                    } catch {
                        log.error("completion failed id=\(event.id)", error: error)
                    }
                }

            case let .taskTapped(event):
                state.composer = CalendarPersonalEventFeature.State(
                    editing: event,
                    disciplines: state.composerOptions,
                    calendar: calendar
                )
                return .none

            case .addTaskTapped:
                guard state.isCurrentSemester else { return .none }
                let tag = state.composerOptions.first { $0.tag.id == state.disciplineId }?.tag
                    ?? PersonalEvent.DisciplineTag(
                        id: state.disciplineId,
                        code: state.detail?.code ?? "",
                        name: state.name
                    )
                state.composer = CalendarPersonalEventFeature.State(
                    day: now,
                    disciplines: state.composerOptions,
                    discipline: tag,
                    calendar: calendar
                )
                return .none

            case .composer, .delegate:
                return .none
            }
        }
        .ifLet(\.$composer, action: \.composer) {
            CalendarPersonalEventFeature()
        }
    }

    /// One-shot count for the entry card, straight from the discipline's
    /// shelf endpoint so past-semester disciplines get the card too (access
    /// is "ever enrolled"). Failures — offline, not enrolled — just keep the
    /// card hidden.
    private func loadMaterials(disciplineId: String) -> Effect<Action> {
        .run { [log] send in
            do {
                let details = try await materialsRepository.discipline(disciplineId)
                await send(.materialsLoaded(details.discipline))
            } catch {
                log.debug("materials entry load failed; card hidden")
            }
        }
        .cancellable(id: CancelID.materials, cancelInFlight: true)
    }
}

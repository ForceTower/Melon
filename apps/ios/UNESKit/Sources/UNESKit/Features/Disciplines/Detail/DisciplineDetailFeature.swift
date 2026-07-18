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
        @Shared(.appStorage(FeatureFlags.materialsEnabledKey)) var isMaterialsEnabled = false

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
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("DisciplineDetailFeature")

    private enum CancelID { case observation, materials }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                // The iOS detail merges a discipline's offers, so it carries a
                // discipline id — label it honestly ("discipline_id", the key
                // materials_discipline already uses) instead of polluting the
                // offer_id namespace.
                analytics.screen(name: Screens.disciplineDetail, properties: ["discipline_id": state.disciplineId])
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
                guard state.showsMaterials, state.materials == nil else { return observation }
                return .merge(observation, loadMaterials(disciplineId: state.disciplineId))

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

            case .delegate:
                return .none
            }
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

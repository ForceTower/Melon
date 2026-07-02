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
    }

    @Dependency(\.disciplinesRepository) var disciplinesRepository

    private enum CancelID { case observation }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                // The observation replays the mirror on subscription, so
                // every appearance rehydrates and later writes (sync
                // refreshes from any tab) land live.
                return .run { [semesterId = state.semesterId, disciplineId = state.disciplineId] send in
                    for await detail in disciplinesRepository.observeDetail(
                        semesterId: semesterId,
                        disciplineId: disciplineId
                    ) {
                        await send(.detailUpdated(detail))
                    }
                }
                .cancellable(id: CancelID.observation, cancelInFlight: true)

            case let .detailUpdated(detail):
                state.detail = detail
                state.name = detail.name
                state.colorIndex = detail.colorIndex
                return .none

            case let .groupSelected(code):
                state.selectedGroup = code
                return .none
            }
        }
    }
}

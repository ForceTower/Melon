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

        init(summary: DisciplineSummary, semesterId: String) {
            self.semesterId = semesterId
            self.disciplineId = summary.id
            self.name = summary.name
            self.colorIndex = summary.colorIndex
        }
    }

    enum Action: Equatable {
        case task
        case detailLoaded(DisciplineDetail?)
        case groupSelected(String?)
    }

    @Dependency(\.disciplinesRepository) var disciplinesRepository
    @Dependency(\.date.now) var now

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                guard state.detail == nil else { return .none }
                return .run { [semesterId = state.semesterId, disciplineId = state.disciplineId] send in
                    let detail = try? await disciplinesRepository.detail(
                        semesterId: semesterId,
                        disciplineId: disciplineId,
                        now: now
                    )
                    await send(.detailLoaded(detail))
                }

            case let .detailLoaded(detail):
                state.detail = detail
                if let detail {
                    state.name = detail.name
                    state.colorIndex = detail.colorIndex
                }
                return .none

            case let .groupSelected(code):
                state.selectedGroup = code
                return .none
            }
        }
    }
}

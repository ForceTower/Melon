import ComposableArchitecture
import Foundation

@Reducer
struct ParadoxoDisciplineFeature {
    @ObservableState
    struct State: Equatable {
        let disciplineId: String
        /// Seed from the row that pushed here — the bar title while loading.
        var name: String?
        var details: ParadoxoDisciplineDetails?
        var isLoading = false
        var loadFailed = false
        var expandedTeacherId: String?
        @Shared(.appStorage("paradoxo_chart_style")) var chartStyle: ParadoxoChartStyle = .line
    }

    enum Action: Equatable {
        case task
        case retryTapped
        case detailsLoaded(ParadoxoDisciplineDetails)
        case detailsFailed
        case chartStyleChanged(ParadoxoChartStyle)
        case teacherExpansionToggled(String)
        case teacherPageTapped(ParadoxoDisciplineTeacher)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openTeacher(id: String, name: String?)
        }
    }

    @Dependency(\.paradoxoRepository) var paradoxoRepository

    private let log = Log.scoped("ParadoxoDisciplineFeature")

    private enum CancelID { case load }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                guard state.details == nil else { return .none }
                state.isLoading = true
                state.loadFailed = false
                return load(state.disciplineId)

            case .retryTapped:
                log.info("retry discipline id=\(state.disciplineId)")
                state.isLoading = true
                state.loadFailed = false
                return load(state.disciplineId)

            case let .detailsLoaded(details):
                state.details = details
                state.name = details.name
                state.isLoading = false
                return .none

            case .detailsFailed:
                state.isLoading = false
                state.loadFailed = true
                return .none

            case let .chartStyleChanged(style):
                state.$chartStyle.withLock { $0 = style }
                return .none

            case let .teacherExpansionToggled(teacherId):
                state.expandedTeacherId = state.expandedTeacherId == teacherId ? nil : teacherId
                return .none

            case let .teacherPageTapped(teacher):
                log.info("open teacher from discipline id=\(teacher.id)")
                return .send(.delegate(.openTeacher(id: teacher.id, name: teacher.name)))

            case .delegate:
                return .none
            }
        }
    }

    private func load(_ id: String) -> Effect<Action> {
        .run { send in
            do {
                let details = try await paradoxoRepository.discipline(id)
                await send(.detailsLoaded(details))
            } catch {
                await send(.detailsFailed)
            }
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }
}

import ComposableArchitecture
import Foundation

@Reducer
struct ParadoxoTeacherFeature {
    @ObservableState
    struct State: Equatable {
        let teacherId: String
        /// Seed from the row that pushed here — the bar title while loading.
        var name: String?
        var details: ParadoxoTeacherDetails?
        var isLoading = false
        var loadFailed = false
    }

    enum Action: Equatable {
        case task
        case retryTapped
        case detailsLoaded(ParadoxoTeacherDetails)
        case detailsFailed
        case disciplineTapped(ParadoxoTeacherDiscipline)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openDiscipline(id: String, name: String?)
        }
    }

    @Dependency(\.paradoxoRepository) var paradoxoRepository
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("ParadoxoTeacherFeature")

    private enum CancelID { case load }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(name: Screens.paradoxoTeacher, properties: ["entity_id": state.teacherId])
                guard state.details == nil else { return .none }
                state.isLoading = true
                state.loadFailed = false
                return load(state.teacherId)

            case .retryTapped:
                log.info("retry teacher id=\(state.teacherId)")
                state.isLoading = true
                state.loadFailed = false
                return load(state.teacherId)

            case let .detailsLoaded(details):
                state.details = details
                state.name = details.name
                state.isLoading = false
                return .none

            case .detailsFailed:
                state.isLoading = false
                state.loadFailed = true
                return .none

            case let .disciplineTapped(discipline):
                log.info("open discipline from teacher id=\(discipline.id)")
                analytics.selectContent(
                    contentType: ContentTypes.paradoxoEntity,
                    itemId: discipline.id,
                    properties: ["kind": ParadoxoEntityKind.discipline.rawValue]
                )
                return .send(.delegate(.openDiscipline(id: discipline.id, name: discipline.name)))

            case .delegate:
                return .none
            }
        }
    }

    private func load(_ id: String) -> Effect<Action> {
        .run { send in
            do {
                let details = try await paradoxoRepository.teacher(id)
                await send(.detailsLoaded(details))
            } catch {
                await send(.detailsFailed)
            }
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }
}

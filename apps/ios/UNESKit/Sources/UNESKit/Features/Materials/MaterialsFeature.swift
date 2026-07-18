import ComposableArchitecture
import Foundation

/// The Materiais hub: current disciplines with their material tallies, the
/// contribution hero, and the entry to the saved shelf.
@Reducer
struct MaterialsFeature {
    @ObservableState
    struct State: Equatable {
        var overview: MaterialsOverview?
        var isLoading = false
        var loadFailed = false
        @Presents var upload: MaterialsUploadFeature.State?
    }

    enum Action: Equatable {
        case task
        case retryTapped
        case refreshPulled
        case overviewLoaded(MaterialsOverview)
        case overviewFailed
        case disciplineTapped(MaterialsDiscipline)
        case contributeTapped
        case savedTapped
        case upload(PresentationAction<MaterialsUploadFeature.Action>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openDiscipline(MaterialsDiscipline)
            case openSaved
        }
    }

    @Dependency(\.materialsRepository) var materialsRepository
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("MaterialsFeature")

    private enum CancelID { case load }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(Screens.materials)
                guard state.overview == nil else { return .none }
                state.isLoading = true
                state.loadFailed = false
                return load()

            case .retryTapped:
                log.info("retry load")
                state.isLoading = true
                state.loadFailed = false
                return load()

            case .refreshPulled:
                return load()

            case let .overviewLoaded(overview):
                state.overview = overview
                state.isLoading = false
                state.loadFailed = false
                return .none

            case .overviewFailed:
                state.isLoading = false
                // A stale screen beats an error screen.
                state.loadFailed = state.overview == nil
                return .none

            case let .disciplineTapped(discipline):
                log.info("open discipline id=\(discipline.id)")
                analytics.selectContent(contentType: ContentTypes.discipline, itemId: discipline.id)
                return .send(.delegate(.openDiscipline(discipline)))

            case .contributeTapped:
                guard let overview = state.overview else { return .none }
                log.info("open upload from hub")
                state.upload = MaterialsUploadFeature.State(
                    disciplines: overview.disciplines,
                    semester: overview.semester
                )
                return .none

            case .savedTapped:
                log.info("open saved shelf")
                analytics.selectContent(contentType: ContentTypes.hub, itemId: "materials_saved")
                return .send(.delegate(.openSaved))

            case let .upload(.presented(.delegate(delegateAction))):
                switch delegateAction {
                case let .finished(material, track):
                    state.upload = nil
                    guard track, let overview = state.overview else { return load() }
                    let discipline = overview.disciplines.first { $0.id == material.discipline.id }
                    return .merge(
                        load(),
                        discipline.map { .send(.delegate(.openDiscipline($0))) } ?? .none
                    )
                }

            case .upload, .delegate:
                return .none
            }
        }
        .ifLet(\.$upload, action: \.upload) {
            MaterialsUploadFeature()
        }
    }

    private func load() -> Effect<Action> {
        .run { send in
            do {
                let overview = try await materialsRepository.overview()
                await send(.overviewLoaded(overview))
            } catch {
                await send(.overviewFailed)
            }
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }
}

import ComposableArchitecture
import Foundation

/// The student's bookmarked materials, across every discipline.
@Reducer
struct MaterialsSavedFeature {
    @ObservableState
    struct State: Equatable {
        var materials: [Material]?
        var isLoading = false
        var loadFailed = false

        /// Grouped by discipline, following the server order.
        var groups: [(discipline: MaterialDisciplineRef, materials: [Material])] {
            var order: [String] = []
            var byDiscipline: [String: [Material]] = [:]
            for material in materials ?? [] {
                if byDiscipline[material.discipline.id] == nil {
                    order.append(material.discipline.id)
                }
                byDiscipline[material.discipline.id, default: []].append(material)
            }
            return order.compactMap { id in
                byDiscipline[id].flatMap { group in
                    group.first.map { (discipline: $0.discipline, materials: group) }
                }
            }
        }
    }

    enum Action: Equatable {
        case task
        case retryTapped
        case refreshPulled
        case materialsLoaded([Material])
        case loadFailed
        case materialTapped(Material)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openMaterial(Material)
        }
    }

    @Dependency(\.materialsRepository) var materialsRepository
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("MaterialsSavedFeature")

    private enum CancelID { case load }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(Screens.materialsSaved)
                guard state.materials == nil else { return .none }
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

            case let .materialsLoaded(materials):
                state.materials = materials
                state.isLoading = false
                state.loadFailed = false
                return .none

            case .loadFailed:
                state.isLoading = false
                state.loadFailed = state.materials == nil
                return .none

            case let .materialTapped(material):
                log.info("open saved material id=\(material.id)")
                analytics.selectContent(contentType: ContentTypes.material, itemId: material.id)
                return .send(.delegate(.openMaterial(material)))

            case .delegate:
                return .none
            }
        }
    }

    private func load() -> Effect<Action> {
        .run { send in
            do {
                let materials = try await materialsRepository.saved()
                await send(.materialsLoaded(materials))
            } catch {
                await send(.loadFailed)
            }
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }
}

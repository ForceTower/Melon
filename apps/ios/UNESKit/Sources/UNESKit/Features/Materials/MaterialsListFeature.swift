import ComposableArchitecture
import Foundation

/// One discipline's shelf: search, type and semester filters, the student's
/// own submissions, and the contribution-selling empty state. Reusable from
/// anywhere that knows a discipline — the hub today, discipline detail later.
@Reducer
struct MaterialsListFeature {
    @ObservableState
    struct State: Equatable {
        /// Header meta, seeded by the pushing screen so the title renders
        /// before the fetch lands, then refreshed by it.
        var discipline: MaterialsDiscipline
        var details: MaterialsDisciplineDetails?
        var isLoading = false
        var loadFailed = false
        var searchQuery = ""
        /// nil browses every type.
        var typeFilter: MaterialType?
        @Presents var upload: MaterialsUploadFeature.State?

        init(discipline: MaterialsDiscipline) {
            self.discipline = discipline
        }

        var published: [Material] { details?.published ?? [] }
        var mine: [Material] { details?.mine ?? [] }
        var isEmpty: Bool { details != nil && published.isEmpty && mine.isEmpty }

        /// The shelf, newest semester first — semesters accumulate forever,
        /// so recency ordering replaces a per-semester filter.
        var filtered: [Material] {
            published
                .filter { material in
                    if let typeFilter, material.type != typeFilter { return false }
                    let query = searchQuery.trimmingCharacters(in: .whitespaces)
                    guard !query.isEmpty else { return true }
                    let folded = Self.fold(query)
                    return Self.fold(material.title).contains(folded)
                        || Self.fold(material.teacherName ?? "").contains(folded)
                }
                .sorted { $0.semester > $1.semester }
        }

        static func fold(_ text: String) -> String {
            text.folding(
                options: [.diacriticInsensitive, .caseInsensitive],
                locale: Locale(identifier: "pt-BR")
            )
        }
    }

    enum Action: Equatable, BindableAction {
        case task
        case retryTapped
        case refreshPulled
        case detailsLoaded(MaterialsDisciplineDetails)
        case detailsFailed
        case materialTapped(Material)
        case contributeTapped
        case upload(PresentationAction<MaterialsUploadFeature.Action>)
        case binding(BindingAction<State>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openMaterial(Material, MaterialsDiscipline)
        }
    }

    @Dependency(\.materialsRepository) var materialsRepository

    private let log = Log.scoped("MaterialsListFeature")

    private enum CancelID { case load }

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .task:
                guard state.details == nil else { return .none }
                state.isLoading = true
                state.loadFailed = false
                return load(state.discipline.id)

            case .retryTapped:
                log.info("retry load id=\(state.discipline.id)")
                state.isLoading = true
                state.loadFailed = false
                return load(state.discipline.id)

            case .refreshPulled:
                return load(state.discipline.id)

            case let .detailsLoaded(details):
                state.details = details
                state.discipline = details.discipline
                state.isLoading = false
                state.loadFailed = false
                // A filter pointing at a type that vanished upstream resets
                // to "all" instead of showing a hollow shelf.
                if let type = state.typeFilter, !details.published.contains(where: { $0.type == type }) {
                    state.typeFilter = nil
                }
                return .none

            case .detailsFailed:
                state.isLoading = false
                state.loadFailed = state.details == nil
                return .none

            case let .materialTapped(material):
                log.info("open material id=\(material.id)")
                return .send(.delegate(.openMaterial(material, state.discipline)))

            case .contributeTapped:
                log.info("open upload from list id=\(state.discipline.id)")
                state.upload = MaterialsUploadFeature.State(
                    disciplines: [state.discipline],
                    semester: nil,
                    locked: state.discipline
                )
                return .none

            case .upload(.presented(.delegate(.finished(_, _)))):
                state.upload = nil
                // The new upload lands in "Meus envios" — refetch to show it.
                return load(state.discipline.id)

            case .upload, .binding, .delegate:
                return .none
            }
        }
        .ifLet(\.$upload, action: \.upload) {
            MaterialsUploadFeature()
        }
    }

    private func load(_ id: String) -> Effect<Action> {
        .run { send in
            do {
                let details = try await materialsRepository.discipline(id)
                await send(.detailsLoaded(details))
            } catch {
                await send(.detailsFailed)
            }
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }
}

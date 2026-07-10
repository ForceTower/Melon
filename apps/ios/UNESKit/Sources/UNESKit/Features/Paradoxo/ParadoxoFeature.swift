import ComposableArchitecture
import Foundation

@Reducer
struct ParadoxoFeature {
    @ObservableState
    struct State: Equatable {
        var overview: ParadoxoOverview?
        var index: [ParadoxoIndexEntry] = []
        var isLoading = false
        var loadFailed = false
        var searchQuery = ""

        var isSearching: Bool {
            !searchQuery.trimmingCharacters(in: .whitespaces).isEmpty
        }

        /// Disciplines first, then teachers — both capped so a two-letter
        /// query doesn't dump the whole index.
        var searchResults: (disciplines: [ParadoxoIndexEntry], teachers: [ParadoxoIndexEntry]) {
            let folded = ParadoxoIndexEntry.fold(searchQuery.trimmingCharacters(in: .whitespaces))
            guard !folded.isEmpty else { return ([], []) }
            let matches = index.filter { $0.matches(folded) }
            return (
                disciplines: Array(matches.filter { $0.ref.kind == .discipline }.prefix(10)),
                teachers: Array(matches.filter { $0.ref.kind == .teacher }.prefix(12))
            )
        }
    }

    enum Action: Equatable, BindableAction {
        case task
        case retryTapped
        case refreshPulled
        case overviewLoaded(ParadoxoOverview)
        case overviewFailed
        case indexLoaded([ParadoxoIndexEntry])
        case pulseFactTapped(ParadoxoPulseFact)
        case exploreTapped(ParadoxoExploreKind)
        case myDisciplineTapped(ParadoxoDisciplineSummary)
        case searchResultTapped(ParadoxoIndexEntry)
        case binding(BindingAction<State>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openDiscipline(id: String, name: String?)
            case openTeacher(id: String, name: String?)
            case openExplore(ParadoxoRanking)
        }
    }

    @Dependency(\.paradoxoRepository) var paradoxoRepository

    private let log = Log.scoped("ParadoxoFeature")

    private enum CancelID { case load }

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .task:
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

            case let .indexLoaded(index):
                state.index = index
                return .none

            case let .pulseFactTapped(fact):
                log.info("open pulse fact id=\(fact.id) kind=\(fact.kind.rawValue)")
                return open(fact.ref, name: fact.title)

            case let .exploreTapped(kind):
                guard let ranking = state.overview?.ranking(kind), !ranking.entries.isEmpty else {
                    return .none
                }
                log.info("open explore kind=\(kind.rawValue)")
                return .send(.delegate(.openExplore(ranking)))

            case let .myDisciplineTapped(summary):
                log.info("open my discipline id=\(summary.id)")
                return .send(.delegate(.openDiscipline(id: summary.id, name: summary.name)))

            case let .searchResultTapped(entry):
                log.info("open search result kind=\(entry.ref.kind.rawValue) id=\(entry.ref.id)")
                return open(entry.ref, name: entry.name)

            case .binding, .delegate:
                return .none
            }
        }
    }

    private func open(_ ref: ParadoxoEntityRef, name: String?) -> Effect<Action> {
        switch ref.kind {
        case .discipline: .send(.delegate(.openDiscipline(id: ref.id, name: name)))
        case .teacher: .send(.delegate(.openTeacher(id: ref.id, name: name)))
        }
    }

    private func load() -> Effect<Action> {
        .run { [log] send in
            async let index = paradoxoRepository.index()
            do {
                let overview = try await paradoxoRepository.overview()
                await send(.overviewLoaded(overview))
            } catch {
                await send(.overviewFailed)
            }
            // The index only powers search; missing it degrades quietly.
            do {
                try await send(.indexLoaded(index))
            } catch {
                log.debug("index load failed; search disabled")
            }
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }
}

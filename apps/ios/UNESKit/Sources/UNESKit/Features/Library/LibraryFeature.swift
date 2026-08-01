import ComposableArchitecture
import Foundation

/// The Biblioteca entry: search box with field scopes, the advanced-search
/// sheet, recent searches, and the "novas no acervo" shelf. The catalogue
/// answers questions; borrowing still happens at the counter.
@Reducer
struct LibraryFeature {
    @ObservableState
    struct State: Equatable {
        var query = ""
        var searchScope: LibrarySearchScope = .all
        var overview: LibraryOverview?
        var isLoading = false
        var loadFailed = false
        var isAdvancedPresented = false
        /// Optimistic hide — the server delete rides behind it.
        var recentsCleared = false

        var recents: [LibraryRecentSearch] {
            recentsCleared ? [] : overview?.recents ?? []
        }
    }

    enum Action: Equatable, BindableAction {
        case task
        case retryTapped
        case overviewLoaded(LibraryOverview)
        case overviewFailed
        case queryChanged(String)
        case scopeTapped(LibrarySearchScope)
        case submitTapped
        case recentTapped(LibraryRecentSearch)
        case clearRecentsTapped
        case newAcquisitionTapped(LibraryWork)
        case advancedTapped
        case advancedSubmitted(query: String, scope: LibrarySearchScope, facets: LibraryFacetSelection)
        case binding(BindingAction<State>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openResults(query: String, scope: LibrarySearchScope, facets: LibraryFacetSelection)
            case openWork(LibraryWork)
        }
    }

    @Dependency(\.libraryRepository) var libraryRepository
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("LibraryFeature")

    private enum CancelID { case load }

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(Screens.library)
                guard state.overview == nil else { return .none }
                state.isLoading = true
                state.loadFailed = false
                return load()

            case .retryTapped:
                state.isLoading = true
                state.loadFailed = false
                return load()

            case let .overviewLoaded(overview):
                state.overview = overview
                state.isLoading = false
                state.loadFailed = false
                return .none

            case .overviewFailed:
                state.isLoading = false
                state.loadFailed = state.overview == nil
                return .none

            case let .queryChanged(query):
                state.query = query
                return .none

            case let .scopeTapped(scope):
                state.searchScope = scope
                return .none

            case .submitTapped:
                let query = state.query.trimmingCharacters(in: .whitespaces)
                guard !query.isEmpty else { return .none }
                log.info("search scope=\(state.searchScope.rawValue)")
                return .send(.delegate(.openResults(query: query, scope: state.searchScope, facets: [:])))

            case let .recentTapped(recent):
                log.info("recent search scope=\(recent.scope.rawValue)")
                return .send(.delegate(.openResults(query: recent.query, scope: recent.scope, facets: [:])))

            case .clearRecentsTapped:
                state.recentsCleared = true
                return .run { [log] _ in
                    do {
                        try await libraryRepository.clearRecents()
                    } catch {
                        // The hub already hid them; the worst case is the row
                        // resurfacing on the next visit.
                        log.warn("clear recents failed", error: error)
                    }
                }

            case let .newAcquisitionTapped(work):
                analytics.selectContent(contentType: ContentTypes.libraryWork, itemId: work.id)
                return .send(.delegate(.openWork(work)))

            case .advancedTapped:
                state.isAdvancedPresented = true
                return .none

            case let .advancedSubmitted(query, scope, facets):
                state.isAdvancedPresented = false
                log.info("advanced search terms scope=\(scope.rawValue)")
                return .send(.delegate(.openResults(query: query, scope: scope, facets: facets)))

            case .binding, .delegate:
                return .none
            }
        }
    }

    private func load() -> Effect<Action> {
        .run { send in
            do {
                let overview = try await libraryRepository.overview()
                await send(.overviewLoaded(overview))
            } catch {
                await send(.overviewFailed)
            }
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }
}

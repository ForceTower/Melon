import ComposableArchitecture
import Foundation

/// Search results over the catalogue. The server sorts, facets and paginates;
/// the screen accumulates pages as the student scrolls. Each row's
/// availability is a second, per-title consultation that fires when the row
/// becomes visible and fills in (or degrades) afterwards.
@Reducer
struct LibraryResultsFeature {
    static let pageSize = 25

    @ObservableState
    struct State: Equatable {
        /// The boolean terms this screen answers; a plain search is one term.
        var terms: [LibrarySearchTerm]
        var facets: LibraryFacetSelection
        /// The pages loaded so far, in server order.
        var works: [LibraryWork] = []
        /// Result count after facet filtering — the server's, not the loaded rows'.
        var total = 0
        /// Refine options with counts over the whole result set.
        var serverFacets: [LibraryFacetGroup: [LibraryFacetValue]] = [:]
        var readings: [String: LibraryReading] = [:]
        /// Work ids with an availability consultation in flight.
        var checking: Set<String> = []
        var isLoading = true
        var isLoadingMore = false
        var sort: LibrarySort = .relevance
        var groupByType = false
        var onlyAvailable = false
        var isRefinePresented = false
        /// Anchor for "due in N days" math, refreshed with the readings.
        var now = Date.distantPast

        init(terms: [LibrarySearchTerm], facets: LibraryFacetSelection = [:]) {
            self.terms = terms
            self.facets = facets
        }

        init(query: String, scope: LibrarySearchScope, facets: LibraryFacetSelection = [:]) {
            self.init(terms: [LibrarySearchTerm(query: query, scope: scope)], facets: facets)
        }

        /// One-line rendering for the title and the empty state.
        var query: String { terms.display }

        var searchScope: LibrarySearchScope { terms.first?.scope ?? .all }

        /// The "search every field" suggestion only makes sense for a plain
        /// single-term search that was scoped down.
        var canBroadenScope: Bool {
            terms.count == 1 && searchScope != .all
        }

        /// Pergamum gives up on queries this short — mirror that upstream
        /// limitation instead of pretending the whole catalogue is scannable.
        /// A multi-term search is a narrowing by construction.
        var isTooBroad: Bool {
            terms.count == 1
                && (terms.first?.query.trimmingCharacters(in: .whitespaces).count ?? 0) <= 2
        }

        /// The full-screen "nothing for this query" state. Zero rows under
        /// active facets is the refine dead-end instead, cleared in place.
        var isEmpty: Bool {
            !isLoading && !isTooBroad && works.isEmpty && activeFacetCount == 0
        }

        var activeFacetCount: Int {
            facets.values.reduce(0) { $0 + $1.count }
        }

        /// The loaded rows after the one filter still applied on-device:
        /// availability is consulted lazily, so the server cannot narrow by it.
        var filtered: [LibraryWork] {
            guard onlyAvailable else { return works }
            return works.filter { $0.availability(now: now).available > 0 }
        }

        /// The count the header and end line narrate — server truth unless
        /// the local availability toggle is narrowing the loaded rows.
        var displayCount: Int {
            onlyAvailable ? filtered.count : total
        }

        var hasMore: Bool { works.count < total }

        var groups: [(type: LibraryWorkType?, works: [LibraryWork])] {
            guard groupByType else { return [(nil, filtered)] }
            return LibraryWorkType.allCases.compactMap { type in
                let items = filtered.filter { $0.type == type }
                return items.isEmpty ? nil : (type, items)
            }
        }

        /// Refine options for one group — the server's counts with the keys
        /// the client knows how to say resolved to display labels.
        func facetValues(for group: LibraryFacetGroup) -> [LibraryFacetValue] {
            (serverFacets[group] ?? []).map { value in
                LibraryFacetValue(key: value.key, label: displayLabel(group, value), count: value.count)
            }
        }

        private func displayLabel(_ group: LibraryFacetGroup, _ value: LibraryFacetValue) -> String {
            switch group {
            case .type:
                LibraryWorkType(rawValue: value.key).map { .localized($0.pluralLabel) } ?? value.label
            case .year:
                LibraryYearBucket(rawValue: value.key).map { .localized($0.label) } ?? value.label
            case .branch:
                LibraryBranch.known.first { $0.id == value.key }?.shortName ?? value.label
            case .subject, .author, .language:
                value.label
            }
        }

        /// The screen-level reading over the rows consulted so far: down or
        /// stale as soon as any checked row is, fresh once one answered live.
        var aggregatedReading: LibraryReading? {
            let displayed = filtered.compactMap { readings[$0.id] }
            if displayed.contains(.unavailable) { return .unavailable }
            for reading in displayed {
                if case .stale = reading { return reading }
            }
            return displayed.first
        }
    }

    enum Action: Equatable, BindableAction {
        case task
        case pageLoaded(LibrarySearchPage, reset: Bool)
        case searchFailed(reset: Bool)
        case loadMoreReached
        case rowAppeared(String)
        case readingLoaded(id: String, LibraryAvailabilitySnapshot)
        case refreshTapped
        case facetToggled(LibraryFacetGroup, String)
        case clearFacetsTapped
        case workTapped(LibraryWork)
        case broadenScopeTapped
        case editQueryTapped
        case binding(BindingAction<State>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openWork(LibraryWork)
        }
    }

    @Dependency(\.libraryRepository) var libraryRepository
    @Dependency(\.analytics) var analytics
    @Dependency(\.date) var date
    @Dependency(\.dismiss) var dismiss

    private let log = Log.scoped("LibraryResultsFeature")

    private enum CancelID { case search, readings }

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(name: Screens.libraryResults, properties: ["scope": state.searchScope.rawValue])
                state.now = date.now
                guard !state.isTooBroad else {
                    state.isLoading = false
                    return .none
                }
                guard state.works.isEmpty else { return .none }
                return search(state, reset: true)

            case let .pageLoaded(page, reset):
                state.isLoading = false
                state.isLoadingMore = false
                state.total = page.total
                state.serverFacets = page.facets
                if reset {
                    state.works = page.works
                } else {
                    // The set can shift between page fetches (cache refresh);
                    // an id that comes again must not render twice.
                    let known = Set(state.works.map(\.id))
                    state.works += page.works.filter { !known.contains($0.id) }
                }
                return .none

            case let .searchFailed(reset):
                state.isLoading = false
                state.isLoadingMore = false
                if reset {
                    state.works = []
                    state.total = 0
                    state.serverFacets = [:]
                }
                return .none

            case .loadMoreReached:
                guard !state.isLoading, !state.isLoadingMore, state.hasMore else { return .none }
                state.isLoadingMore = true
                return search(state, reset: false)

            case let .rowAppeared(id):
                guard state.readings[id] == nil, !state.checking.contains(id) else { return .none }
                state.checking.insert(id)
                return .run { send in
                    let snapshot = await libraryRepository.checkAvailability(id)
                    await send(.readingLoaded(id: id, snapshot))
                }

            case let .readingLoaded(id, snapshot):
                state.checking.remove(id)
                state.readings[id] = snapshot.reading
                // A live reading also carries the copies — fold them into the
                // work so counts and the detail push reflect what was read.
                if snapshot.reading != .unavailable, !snapshot.copies.isEmpty,
                   let index = state.works.firstIndex(where: { $0.id == id }) {
                    state.works[index].copies = snapshot.copies
                }
                return .none

            case .refreshTapped:
                log.info("refresh availability")
                state.now = date.now
                // Only rows the student has actually seen hold readings —
                // re-consult those; the rest re-checks as it scrolls in.
                let ids = state.filtered.map(\.id).filter { state.readings[$0] != nil }
                state.readings = [:]
                state.checking.formUnion(ids)
                return .run { send in
                    for id in ids {
                        let snapshot = await libraryRepository.checkAvailability(id)
                        await send(.readingLoaded(id: id, snapshot))
                    }
                }
                .cancellable(id: CancelID.readings, cancelInFlight: true)

            case let .facetToggled(group, key):
                var keys = state.facets[group] ?? []
                if keys.contains(key) { keys.remove(key) } else { keys.insert(key) }
                if keys.isEmpty {
                    state.facets[group] = nil
                } else {
                    state.facets[group] = keys
                }
                return search(state, reset: true)

            case .clearFacetsTapped:
                guard state.activeFacetCount > 0 else { return .none }
                state.facets = [:]
                return search(state, reset: true)

            case let .workTapped(work):
                analytics.selectContent(contentType: ContentTypes.libraryWork, itemId: work.id)
                return .send(.delegate(.openWork(work)))

            case .broadenScopeTapped:
                guard !state.terms.isEmpty else { return .none }
                state.terms[0].scope = .all
                state.isLoading = true
                state.works = []
                state.total = 0
                return search(state, reset: true)

            case .editQueryTapped:
                return .run { _ in await dismiss() }

            case .binding(\.sort):
                return search(state, reset: true)

            case .binding, .delegate:
                return .none
            }
        }
    }

    private func search(_ state: State, reset: Bool) -> Effect<Action> {
        let request = LibrarySearchRequest(
            terms: state.terms,
            sort: state.sort,
            facets: state.facets,
            offset: reset ? 0 : state.works.count,
            limit: Self.pageSize
        )
        return .run { send in
            do {
                let page = try await libraryRepository.search(request)
                await send(.pageLoaded(page, reset: reset))
            } catch {
                await send(.searchFailed(reset: reset))
            }
        }
        .cancellable(id: CancelID.search, cancelInFlight: true)
    }
}

extension LibraryYearBucket {
    var label: LocalizedStringResource {
        switch self {
        case .from2020: .libraryYearFrom2020
        case .decade2010: .libraryYearDecade2010
        case .decade2000: .libraryYearDecade2000
        case .decade1990: .libraryYearDecade1990
        case .before1990: .libraryYearBefore1990
        case .unknown: .libraryYearUnknown
        }
    }
}

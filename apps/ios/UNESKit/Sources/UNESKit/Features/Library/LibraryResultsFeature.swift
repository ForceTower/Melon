import ComposableArchitecture
import Foundation

/// Search results over the catalogue. The catalogue rows render as soon as
/// the search answers; each row's availability is a second, per-title
/// consultation that fills in (or degrades) afterwards.
@Reducer
struct LibraryResultsFeature {
    @ObservableState
    struct State: Equatable {
        /// The boolean terms this screen answers; a plain search is one term.
        var terms: [LibrarySearchTerm]
        var facets: LibraryFacetSelection
        var works: [LibraryWork] = []
        var readings: [String: LibraryReading] = [:]
        var isLoading = true
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

        var isEmpty: Bool {
            !isLoading && !isTooBroad && works.isEmpty
        }

        var activeFacetCount: Int {
            facets.values.reduce(0) { $0 + $1.count }
        }

        /// The rows after refine filters and sorting.
        var filtered: [LibraryWork] {
            var rows = works.filter { $0.matches(facets) }
            if onlyAvailable {
                rows = rows.filter { $0.availability(now: now).available > 0 }
            }
            switch sort {
            case .relevance:
                break
            case .newest, .oldest:
                rows.sort { lhs, rhs in
                    let l = lhs.sortYear, r = rhs.sortYear
                    return sort == .newest ? l > r : l < r
                }
            case .titleAZ:
                rows.sort {
                    $0.parsedTitle.title.localizedStandardCompare($1.parsedTitle.title) == .orderedAscending
                }
            }
            return rows
        }

        var groups: [(type: LibraryWorkType?, works: [LibraryWork])] {
            guard groupByType else { return [(nil, filtered)] }
            return LibraryWorkType.allCases.compactMap { type in
                let items = filtered.filter { $0.type == type }
                return items.isEmpty ? nil : (type, items)
            }
        }

        /// Refine counts over the unfiltered result set — a work can carry
        /// several subjects and live in more than one branch, so the counts
        /// sum to more than the total.
        func facetValues(for group: LibraryFacetGroup) -> [LibraryFacetValue] {
            var counts: [String: Int] = [:]
            for work in works {
                for key in work.facetKeys(for: group) {
                    counts[key, default: 0] += 1
                }
            }
            switch group {
            case .type:
                return LibraryWorkType.allCases.compactMap { type in
                    counts[type.rawValue].map {
                        LibraryFacetValue(key: type.rawValue, label: .localized(type.pluralLabel), count: $0)
                    }
                }
            case .branch:
                // The registry order for the known campus libraries, then any
                // unmapped branch the backend surfaced, by name.
                var byId: [String: LibraryBranch] = [:]
                for work in works {
                    for branch in work.branches where byId[branch.id] == nil {
                        byId[branch.id] = branch
                    }
                }
                let knownIds = LibraryBranch.known.map(\.id)
                return byId.values
                    .sorted { lhs, rhs in
                        let li = knownIds.firstIndex(of: lhs.id) ?? knownIds.count
                        let ri = knownIds.firstIndex(of: rhs.id) ?? knownIds.count
                        return li != ri ? li < ri : lhs.name < rhs.name
                    }
                    .compactMap { branch in
                        counts[branch.id].map {
                            LibraryFacetValue(key: branch.id, label: branch.shortName, count: $0)
                        }
                    }
            case .year:
                return LibraryYearBucket.allCases.compactMap { bucket in
                    counts[bucket.rawValue].map {
                        LibraryFacetValue(key: bucket.rawValue, label: .localized(bucket.label), count: $0)
                    }
                }
            case .subject, .author, .language:
                return counts
                    .map { LibraryFacetValue(key: $0.key, label: $0.key, count: $0.value) }
                    .sorted { lhs, rhs in
                        lhs.count != rhs.count ? lhs.count > rhs.count : lhs.label < rhs.label
                    }
            }
        }

        /// The screen-level reading: down or stale as soon as any row is,
        /// fresh only once every row resolved.
        var aggregatedReading: LibraryReading? {
            let displayed = filtered.compactMap { readings[$0.id] }
            if displayed.contains(.unavailable) { return .unavailable }
            for reading in displayed {
                if case .stale = reading { return reading }
            }
            guard displayed.count == filtered.count, let first = displayed.first else { return nil }
            return first
        }
    }

    enum Action: Equatable, BindableAction {
        case task
        case worksLoaded([LibraryWork])
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
                return search(state)

            case let .worksLoaded(works):
                state.works = works
                state.isLoading = false
                state.readings = [:]
                return checkReadings(for: works)

            case let .readingLoaded(id, snapshot):
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
                state.readings = [:]
                return checkReadings(for: state.works)

            case let .facetToggled(group, key):
                var keys = state.facets[group] ?? []
                if keys.contains(key) { keys.remove(key) } else { keys.insert(key) }
                if keys.isEmpty {
                    state.facets[group] = nil
                } else {
                    state.facets[group] = keys
                }
                return .none

            case .clearFacetsTapped:
                state.facets = [:]
                return .none

            case let .workTapped(work):
                analytics.selectContent(contentType: ContentTypes.libraryWork, itemId: work.id)
                return .send(.delegate(.openWork(work)))

            case .broadenScopeTapped:
                guard !state.terms.isEmpty else { return .none }
                state.terms[0].scope = .all
                state.isLoading = true
                return search(state)

            case .editQueryTapped:
                return .run { _ in await dismiss() }

            case .binding, .delegate:
                return .none
            }
        }
    }

    private func search(_ state: State) -> Effect<Action> {
        .run { [terms = state.terms] send in
            do {
                let works = try await libraryRepository.search(terms)
                await send(.worksLoaded(works))
            } catch {
                await send(.worksLoaded([]))
            }
        }
        .cancellable(id: CancelID.search, cancelInFlight: true)
    }

    /// Consults circulation title by title so rows resolve as answers land.
    private func checkReadings(for works: [LibraryWork]) -> Effect<Action> {
        .run { send in
            for work in works {
                let snapshot = await libraryRepository.checkAvailability(work.id)
                await send(.readingLoaded(id: work.id, snapshot))
            }
        }
        .cancellable(id: CancelID.readings, cancelInFlight: true)
    }
}

extension LibraryWork {
    /// Numeric year for sorting; unparseable years sink to the bottom.
    fileprivate var sortYear: Int {
        if case let .year(text) = year, let value = Int(text) { return value }
        return 0
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

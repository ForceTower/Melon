import ComposableArchitecture
import Foundation

/// Search results over the catalogue. The catalogue rows render as soon as
/// the search answers; each row's availability is a second, per-title
/// consultation that fills in (or degrades) afterwards.
@Reducer
struct LibraryResultsFeature {
    @ObservableState
    struct State: Equatable {
        var query: String
        var searchScope: LibrarySearchScope
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

        init(query: String, scope: LibrarySearchScope, facets: LibraryFacetSelection = [:]) {
            self.query = query
            self.searchScope = scope
            self.facets = facets
        }

        /// Pergamum gives up on queries this short — mirror that upstream
        /// limitation instead of pretending the whole catalogue is scannable.
        var isTooBroad: Bool {
            query.trimmingCharacters(in: .whitespaces).count <= 2
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
                return LibraryBranch.allCases.compactMap { branch in
                    counts[branch.rawValue].map {
                        LibraryFacetValue(key: branch.rawValue, label: branch.shortName, count: $0)
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
        case readingLoaded(id: String, LibraryReading)
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

            case let .readingLoaded(id, reading):
                state.readings[id] = reading
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
                state.searchScope = .all
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
        .run { [query = state.query, scope = state.searchScope] send in
            do {
                let works = try await libraryRepository.search(query, scope)
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
                let reading = await libraryRepository.checkAvailability(work.id)
                await send(.readingLoaded(id: work.id, reading))
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

extension LibraryBranch {
    /// Compact label for refine rows.
    var shortName: String {
        switch self {
        case .central: "Central Julieta Carteado"
        case .health: "Setorial de Saúde"
        case .lencois: "Campus de Lençóis"
        case .santoAntonio: "Campus de Sto. Antônio de Jesus"
        }
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

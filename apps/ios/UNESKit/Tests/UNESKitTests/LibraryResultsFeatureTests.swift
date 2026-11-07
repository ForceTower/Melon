import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct LibraryResultsFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)

    private nonisolated static func work(_ id: String) -> LibraryWork {
        LibraryWork(
            id: id,
            rawTitle: "Obra \(id)",
            callNumber: "000 A\(id)",
            type: .book,
            authors: [],
            subjects: [],
            branches: [],
            record: [],
            copies: []
        )
    }

    private nonisolated static func works(_ prefix: String, count: Int) -> [LibraryWork] {
        (0..<count).map { work("\(prefix)\($0)") }
    }

    private nonisolated static func page(_ works: [LibraryWork], for request: LibrarySearchRequest) -> LibrarySearchPage {
        LibrarySearchPage(
            works: Array(works.dropFirst(request.offset).prefix(request.limit)),
            total: works.count,
            offset: request.offset,
            facets: [:]
        )
    }

    private nonisolated static func loaded(_ works: [LibraryWork], total: Int) -> LibraryResultsFeature.State {
        var state = LibraryResultsFeature.State(query: "cálculo", scope: .all)
        state.works = works
        state.total = total
        state.phase = .loaded
        return state
    }

    // MARK: Reset searches vs. pagination

    @Test
    func sortChangeHoldsPaginationUntilTheNewFirstPageLands() async {
        let clock = TestClock()
        let requests = LockIsolated<[LibrarySearchRequest]>([])
        let relevance = Self.works("r", count: 25)
        let newest = Self.works("n", count: 60)
        let store = TestStore(initialState: Self.loaded(relevance, total: 60)) {
            LibraryResultsFeature()
        } withDependencies: {
            $0.libraryRepository.search = { request in
                requests.withValue { $0.append(request) }
                try await clock.sleep(for: .seconds(1))
                return Self.page(newest, for: request)
            }
        }

        await store.send(.binding(.set(\.sort, .newest))) {
            $0.sort = .newest
            $0.phase = .loading
        }
        await store.send(.loadMoreReached)
        await clock.advance(by: .seconds(1))
        await store.receive(\.pageLoaded) {
            $0.phase = .loaded
            $0.works = Array(newest.prefix(25))
        }

        #expect(requests.value.map(\.offset) == [0])
        #expect(requests.value.map(\.sort) == [.newest])
    }

    @Test
    func facetToggleHoldsPaginationUntilTheNewFirstPageLands() async {
        let clock = TestClock()
        let requests = LockIsolated<[LibrarySearchRequest]>([])
        let books = Self.works("b", count: 40)
        let store = TestStore(initialState: Self.loaded(Self.works("r", count: 25), total: 60)) {
            LibraryResultsFeature()
        } withDependencies: {
            $0.libraryRepository.search = { request in
                requests.withValue { $0.append(request) }
                try await clock.sleep(for: .seconds(1))
                return Self.page(books, for: request)
            }
        }

        await store.send(.facetToggled(.type, "book")) {
            $0.facets = [.type: ["book"]]
            $0.phase = .loading
        }
        await store.send(.loadMoreReached)
        await clock.advance(by: .seconds(1))
        await store.receive(\.pageLoaded) {
            $0.phase = .loaded
            $0.works = Array(books.prefix(25))
            $0.total = 40
        }

        #expect(requests.value.map(\.offset) == [0])
    }

    // MARK: Availability refresh

    @Test
    func refreshIgnoresTapsWhileItsSweepIsRunning() async {
        let clock = TestClock()
        let now = Self.referenceDate
        let fresh = LibraryReading.fresh(checkedAt: now)
        var initial = Self.loaded([Self.work("a"), Self.work("b"), Self.work("c")], total: 3)
        initial.readings = ["a": fresh, "b": fresh, "c": fresh]
        let store = TestStore(initialState: initial) {
            LibraryResultsFeature()
        } withDependencies: {
            $0.date = .constant(now)
            $0.libraryRepository.checkAvailability = { _ in
                try? await clock.sleep(for: .seconds(1))
                return LibraryAvailabilitySnapshot(reading: fresh, copies: [])
            }
        }

        await store.send(.refreshTapped) {
            $0.now = now
            $0.readings = [:]
            $0.checking = ["a", "b", "c"]
            $0.isRefreshingReadings = true
        }
        await clock.advance(by: .seconds(1))
        await store.receive(\.readingLoaded) {
            $0.checking = ["b", "c"]
            $0.readings = ["a": fresh]
        }
        await store.send(.refreshTapped)
        await clock.advance(by: .seconds(1))
        await store.receive(\.readingLoaded) {
            $0.checking = ["c"]
            $0.readings["b"] = fresh
        }
        await clock.advance(by: .seconds(1))
        await store.receive(\.readingLoaded) {
            $0.checking = []
            $0.readings["c"] = fresh
        }
        await store.receive(\.readingsRefreshed) {
            $0.isRefreshingReadings = false
        }
    }

    // MARK: Search failures

    @Test
    func failedSortChangeOffersRetryInsteadOfNoMatches() async {
        let online = LockIsolated(false)
        let relevance = Self.works("r", count: 25)
        let newest = Self.works("n", count: 60)
        let store = TestStore(initialState: Self.loaded(relevance, total: 60)) {
            LibraryResultsFeature()
        } withDependencies: {
            $0.libraryRepository.search = { request in
                guard online.value else { throw URLError(.notConnectedToInternet) }
                return Self.page(newest, for: request)
            }
        }

        await store.send(.binding(.set(\.sort, .newest))) {
            $0.sort = .newest
            $0.phase = .loading
        }
        await store.receive(\.searchFailed) {
            $0.phase = .failed
        }
        #expect(!store.state.isEmpty)

        await store.send(.loadMoreReached)
        online.setValue(true)
        await store.send(.retryTapped) {
            $0.phase = .loading
        }
        await store.receive(\.pageLoaded) {
            $0.phase = .loaded
            $0.works = Array(newest.prefix(25))
        }
    }

    @Test
    func failedFirstPageIsAnErrorNotAnEmptyResult() async {
        let store = TestStore(initialState: LibraryResultsFeature.State(query: "cálculo", scope: .all)) {
            LibraryResultsFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.libraryRepository.search = { _ in throw URLError(.notConnectedToInternet) }
        }

        await store.send(.task) {
            $0.now = Self.referenceDate
        }
        await store.receive(\.searchFailed) {
            $0.phase = .failed
        }

        #expect(!store.state.isEmpty)
    }

    @Test
    func failedPageAppendKeepsTheLoadedRows() async {
        let relevance = Self.works("r", count: 25)
        let store = TestStore(initialState: Self.loaded(relevance, total: 60)) {
            LibraryResultsFeature()
        } withDependencies: {
            $0.libraryRepository.search = { _ in throw URLError(.notConnectedToInternet) }
        }

        await store.send(.loadMoreReached) {
            $0.phase = .loadingMore
        }
        await store.receive(\.searchFailed) {
            $0.phase = .loaded
        }
    }
}

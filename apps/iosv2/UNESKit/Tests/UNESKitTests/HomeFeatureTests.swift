import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct HomeFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)

    private nonisolated static func heroless() -> HomeOverview {
        var overview = HomeOverview.preview(now: referenceDate)
        overview.hero = nil
        return overview
    }

    @Test
    func taskLoadsTheOverviewWhenTheMirrorIsEmpty() async {
        let overview = Self.heroless()
        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.homeRepository.cached = { _ in nil }
            $0.homeRepository.refresh = { _ in overview }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.overviewLoaded(overview)) {
            $0.isLoading = false
            $0.overview = overview
            $0.lastRefreshed = Self.referenceDate
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))
    }

    @Test
    func taskHydratesFromTheMirrorBeforeRefreshing() async {
        var stale = Self.heroless()
        stale.semesterCode = "2025.2"
        let syncedAt = Self.referenceDate.addingTimeInterval(-45 * 60)
        let cached = CachedHomeOverview(overview: stale, syncedAt: syncedAt)
        let fresh = Self.heroless()

        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.homeRepository.cached = { _ in cached }
            $0.homeRepository.refresh = { _ in fresh }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.hydrated(cached)) {
            $0.isLoading = false
            $0.overview = stale
            $0.lastRefreshed = syncedAt
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))
        await store.receive(.overviewLoaded(fresh)) {
            $0.overview = fresh
            $0.lastRefreshed = Self.referenceDate
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))
    }

    @Test
    func failureSurfacesOnlyWithoutData() async {
        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.homeRepository.cached = { _ in nil }
            $0.homeRepository.refresh = { _ in throw APIError.emptyEnvelope }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.overviewFailed(APIError.emptyEnvelope.localizedDescription)) {
            $0.isLoading = false
            $0.errorMessage = APIError.emptyEnvelope.localizedDescription
        }
    }

    @Test
    func offlineTaskKeepsShowingTheMirrorData() async {
        let cached = CachedHomeOverview(overview: Self.heroless(), syncedAt: Self.referenceDate)

        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.homeRepository.cached = { _ in cached }
            $0.homeRepository.refresh = { _ in throw APIError.emptyEnvelope }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.hydrated(cached)) {
            $0.isLoading = false
            $0.overview = cached.overview
            $0.lastRefreshed = Self.referenceDate
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))
        // Stale beats an error screen: the failure leaves the data untouched.
        await store.receive(.overviewFailed(APIError.emptyEnvelope.localizedDescription))
    }

    @Test
    func heroStartRefreshesTheOverview() async {
        var mutableOverview = Self.heroless()
        mutableOverview.hero = HomeHeroClass(
            disciplineName: "Cálculo II",
            startsAt: Self.referenceDate.addingTimeInterval(30 * 60),
            startTime: "10:20"
        )
        let withHero = mutableOverview
        let responses = LockIsolated([withHero, Self.heroless()])
        let clock = TestClock()

        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.continuousClock = clock
            $0.homeRepository.cached = { _ in nil }
            $0.homeRepository.refresh = { _ in responses.withValue { $0.removeFirst() } }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.overviewLoaded(withHero)) {
            $0.isLoading = false
            $0.overview = withHero
            $0.lastRefreshed = Self.referenceDate
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))

        await clock.advance(by: .seconds(30 * 60 + 1))

        await store.receive(.refreshPulled)
        await store.receive(.overviewLoaded(Self.heroless())) {
            $0.overview = Self.heroless()
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))
    }

    @Test
    func heroRolloverFallsBackToTheMirrorWhenOffline() async {
        var mutableOverview = Self.heroless()
        mutableOverview.hero = HomeHeroClass(
            disciplineName: "Cálculo II",
            startsAt: Self.referenceDate.addingTimeInterval(30 * 60),
            startTime: "10:20"
        )
        let withHero = mutableOverview
        let rolled = CachedHomeOverview(overview: Self.heroless(), syncedAt: Self.referenceDate)
        // nil marks the offline second refresh.
        let refreshes = LockIsolated<[HomeOverview?]>([withHero, nil])
        let cachedReads = LockIsolated<[CachedHomeOverview?]>([nil, rolled])
        let clock = TestClock()

        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.continuousClock = clock
            $0.homeRepository.cached = { _ in cachedReads.withValue { $0.removeFirst() } }
            $0.homeRepository.refresh = { _ in
                guard let next = refreshes.withValue({ $0.removeFirst() }) else {
                    throw APIError.emptyEnvelope
                }
                return next
            }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.overviewLoaded(withHero)) {
            $0.isLoading = false
            $0.overview = withHero
            $0.lastRefreshed = Self.referenceDate
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))

        await clock.advance(by: .seconds(30 * 60 + 1))

        await store.receive(.refreshPulled)
        // Offline, but the mirror recomputes "today" so the hero advances.
        await store.receive(.hydrated(rolled)) {
            $0.overview = rolled.overview
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))
    }

    @Test
    func shortcutsForwardToTheDelegate() async {
        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        }

        await store.send(.seeScheduleTapped)
        await store.receive(.delegate(.openSchedule))

        await store.send(.messagesWidgetTapped)
        await store.receive(.delegate(.openMessages))
    }
}

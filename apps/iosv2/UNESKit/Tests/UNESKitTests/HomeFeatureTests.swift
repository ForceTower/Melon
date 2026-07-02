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
        let (updates, mirror) = AsyncStream.makeStream(of: CachedHomeOverview.self)

        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.homeRepository.observe = { updates }
            $0.homeRepository.refresh = { now in
                mirror.yield(CachedHomeOverview(overview: overview, syncedAt: now))
                mirror.finish()
            }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.mirrorUpdated(CachedHomeOverview(overview: overview, syncedAt: Self.referenceDate))) {
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
        let (updates, mirror) = AsyncStream.makeStream(of: CachedHomeOverview.self)
        // The observation replays the stale mirror before the refresh lands.
        mirror.yield(cached)

        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.homeRepository.observe = { updates }
            $0.homeRepository.refresh = { now in
                mirror.yield(CachedHomeOverview(overview: fresh, syncedAt: now))
                mirror.finish()
            }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.mirrorUpdated(cached)) {
            $0.isLoading = false
            $0.overview = stale
            $0.lastRefreshed = syncedAt
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))
        await store.receive(.mirrorUpdated(CachedHomeOverview(overview: fresh, syncedAt: Self.referenceDate))) {
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
            $0.homeRepository.observe = { .finished }
            $0.homeRepository.cached = { _ in nil }
            $0.homeRepository.refresh = { _ in throw APIError.emptyEnvelope }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.refreshFailed(APIError.emptyEnvelope.localizedDescription)) {
            $0.isLoading = false
            $0.errorMessage = APIError.emptyEnvelope.localizedDescription
        }
    }

    @Test
    func offlineTaskKeepsShowingTheMirrorData() async {
        let cached = CachedHomeOverview(overview: Self.heroless(), syncedAt: Self.referenceDate)
        let (updates, mirror) = AsyncStream.makeStream(of: CachedHomeOverview.self)
        mirror.yield(cached)
        mirror.finish()

        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.homeRepository.observe = { updates }
            $0.homeRepository.cached = { _ in cached }
            $0.homeRepository.refresh = { _ in throw APIError.emptyEnvelope }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.mirrorUpdated(cached)) {
            $0.isLoading = false
            $0.overview = cached.overview
            $0.lastRefreshed = Self.referenceDate
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))
        // Stale beats an error screen: the failed refresh falls back to the
        // mirror instead of surfacing.
        await store.receive(.mirrorUpdated(cached))
        await store.receive(.delegate(.unreadMessagesChanged(2)))
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
        let (updates, mirror) = AsyncStream.makeStream(of: CachedHomeOverview.self)

        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.continuousClock = clock
            $0.homeRepository.observe = { updates }
            $0.homeRepository.refresh = { now in
                let overview = responses.withValue { $0.removeFirst() }
                mirror.yield(CachedHomeOverview(overview: overview, syncedAt: now))
                if responses.isEmpty { mirror.finish() }
            }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.mirrorUpdated(CachedHomeOverview(overview: withHero, syncedAt: Self.referenceDate))) {
            $0.isLoading = false
            $0.overview = withHero
            $0.lastRefreshed = Self.referenceDate
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))

        await clock.advance(by: .seconds(30 * 60 + 1))

        await store.receive(.refreshPulled)
        await store.receive(.mirrorUpdated(CachedHomeOverview(overview: Self.heroless(), syncedAt: Self.referenceDate))) {
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
        // The first refresh lands in the mirror; the second is offline.
        let isFirstRefresh = LockIsolated(true)
        let clock = TestClock()
        let (updates, mirror) = AsyncStream.makeStream(of: CachedHomeOverview.self)

        let store = TestStore(initialState: HomeFeature.State()) {
            HomeFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.continuousClock = clock
            $0.homeRepository.observe = { updates }
            $0.homeRepository.cached = { _ in rolled }
            $0.homeRepository.refresh = { now in
                guard isFirstRefresh.withValue({ first in
                    defer { first = false }
                    return first
                }) else {
                    mirror.finish()
                    throw APIError.emptyEnvelope
                }
                mirror.yield(CachedHomeOverview(overview: withHero, syncedAt: now))
            }
            $0.profileRepository.current = { throw APIError.emptyEnvelope }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.mirrorUpdated(CachedHomeOverview(overview: withHero, syncedAt: Self.referenceDate))) {
            $0.isLoading = false
            $0.overview = withHero
            $0.lastRefreshed = Self.referenceDate
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))

        await clock.advance(by: .seconds(30 * 60 + 1))

        await store.receive(.refreshPulled)
        // Offline, but the mirror recomputes "today" so the hero advances.
        await store.receive(.mirrorUpdated(rolled)) {
            $0.overview = rolled.overview
        }
        await store.receive(.delegate(.unreadMessagesChanged(2)))
    }

    @Test
    func disciplineTapPushesTheDetail() async {
        let overview = Self.heroless()
        let store = TestStore(
            initialState: HomeFeature.State(overview: overview)
        ) {
            HomeFeature()
        }

        await store.send(.disciplineTapped(id: "d2", name: "Cálculo II")) {
            $0.path.append(.detail(DisciplineDetailFeature.State(
                semesterId: "sem1",
                disciplineId: "d2",
                name: "Cálculo II",
                colorIndex: 1
            )))
        }
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

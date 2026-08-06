import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct ScheduleGridFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)

    private struct Boom: Error, LocalizedError {
        var errorDescription: String? { "boom" }
    }

    @Test
    func taskHydratesFromTheMirrorBeforeRefreshing() async {
        let stale = ScheduleOverview.preview(now: Self.referenceDate)
        let fresh = ScheduleOverview.preview(now: Self.referenceDate.addingTimeInterval(7 * 86_400))
        let (updates, mirror) = AsyncStream.makeStream(of: ScheduleOverview.self)
        // The observation replays the stale mirror before the refresh lands.
        mirror.yield(stale)

        let store = TestStore(initialState: ScheduleGridFeature.State()) {
            ScheduleGridFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.scheduleRepository.observe = { updates }
            $0.scheduleRepository.refresh = { _ in
                mirror.yield(fresh)
                mirror.finish()
            }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.overviewUpdated(stale)) {
            $0.isLoading = false
            $0.overview = stale
        }
        await store.receive(.overviewUpdated(fresh)) {
            $0.overview = fresh
        }
    }

    @Test
    func taskOnlyResubscribesWhenAlreadyHydrated() async {
        let stale = ScheduleOverview.preview(now: Self.referenceDate)
        let fresh = ScheduleOverview.preview(now: Self.referenceDate.addingTimeInterval(7 * 86_400))
        let (updates, mirror) = AsyncStream.makeStream(of: ScheduleOverview.self)
        mirror.yield(fresh)
        mirror.finish()

        var seeded = ScheduleGridFeature.State()
        seeded.overview = stale

        // `refresh` stays unimplemented on purpose: a hydrated re-appearance
        // must only restart the observation, never hit upstream.
        let store = TestStore(initialState: seeded) {
            ScheduleGridFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.scheduleRepository.observe = { updates }
        }

        await store.send(.task)
        await store.receive(.overviewUpdated(fresh)) {
            $0.overview = fresh
        }
    }

    @Test
    func refreshFailureFallsBackToTheCachedWeek() async {
        let cached = ScheduleOverview.preview(now: Self.referenceDate)

        let store = TestStore(initialState: ScheduleGridFeature.State()) {
            ScheduleGridFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.scheduleRepository.observe = { .finished }
            $0.scheduleRepository.refresh = { _ in throw Boom() }
            $0.scheduleRepository.cached = { _ in cached }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        // Offline with a mirror: the week is recomputed locally instead of
        // surfacing the failure.
        await store.receive(.overviewUpdated(cached)) {
            $0.isLoading = false
            $0.overview = cached
        }
    }

    @Test
    func refreshFailureWithoutAMirrorSurfacesTheError() async {
        let store = TestStore(initialState: ScheduleGridFeature.State()) {
            ScheduleGridFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.scheduleRepository.observe = { .finished }
            $0.scheduleRepository.refresh = { _ in throw Boom() }
            $0.scheduleRepository.cached = { _ in nil }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.refreshFailed("boom")) {
            $0.isLoading = false
            $0.errorMessage = "boom"
        }
    }

    @Test
    func pulledRefreshFailureKeepsTheStaleWeek() async {
        var seeded = ScheduleGridFeature.State()
        seeded.overview = .preview(now: Self.referenceDate)

        let store = TestStore(initialState: seeded) {
            ScheduleGridFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.scheduleRepository.refresh = { _ in throw Boom() }
            $0.scheduleRepository.cached = { _ in nil }
        }

        await store.send(.refreshPulled)
        // A stale week beats an error screen: with data on screen the
        // failure changes nothing.
        await store.receive(.refreshFailed("boom"))
    }

    @Test
    func classTapOpensTheSheetAndDismissClosesIt() async {
        let overview = ScheduleOverview.preview(now: Self.referenceDate)
        let scheduleClass = overview.days[0].classes[0]

        var seeded = ScheduleGridFeature.State()
        seeded.overview = overview

        let store = TestStore(initialState: seeded) {
            ScheduleGridFeature()
        }

        await store.send(.classTapped(scheduleClass, dayIndex: 0)) {
            $0.sheet = ScheduleGridFeature.SheetItem(scheduleClass: scheduleClass, dayIndex: 0)
        }
        await store.send(.sheetDismissed) {
            $0.sheet = nil
        }
    }

    @Test
    func sheetDisciplineTapPushesTheDetailAndLogsTheTap() async {
        let overview = ScheduleOverview.preview(now: Self.referenceDate)
        var scheduleClass = overview.days[0].classes[0]
        scheduleClass.offerId = "offer-1"
        let taps = LockIsolated<[String?]>([])

        var seeded = ScheduleGridFeature.State()
        seeded.overview = overview
        seeded.sheet = ScheduleGridFeature.SheetItem(scheduleClass: scheduleClass, dayIndex: 0)

        let store = TestStore(initialState: seeded) {
            ScheduleGridFeature()
        } withDependencies: {
            $0.analytics.selectContent = { _, itemId, _ in
                taps.withValue { $0.append(itemId) }
            }
        }

        await store.send(.sheetDisciplineTapped) {
            $0.sheet = nil
            $0.path[id: 0] = .detail(DisciplineDetailFeature.State(
                semesterId: "sem-2026-1",
                disciplineId: scheduleClass.disciplineId,
                name: scheduleClass.title,
                colorIndex: scheduleClass.colorIndex
            ))
        }
        // The discipline tap carries the cross-platform analytics identity.
        #expect(taps.value == ["offer-1"])
    }

    @Test
    func detailMaterialsDelegateRoutesOntoTheGridStack() async {
        let discipline = MaterialsOverview.preview().disciplines[0]
        let material = Material.preview()[0]

        var seeded = ScheduleGridFeature.State()
        seeded.overview = .preview(now: Self.referenceDate)
        seeded.path.append(.detail(DisciplineDetailFeature.State(
            semesterId: "sem-2026-1",
            disciplineId: discipline.id,
            name: discipline.name,
            colorIndex: discipline.colorIndex
        )))

        let store = TestStore(initialState: seeded) {
            ScheduleGridFeature()
        }

        await store.send(.path(.element(id: 0, action: .detail(.delegate(.openMaterials(discipline)))))) {
            $0.path[id: 1] = .materialsList(MaterialsListFeature.State(discipline: discipline))
        }
        await store.send(.path(.element(id: 1, action: .materialsList(.delegate(.openMaterial(material, discipline)))))) {
            $0.path[id: 2] = .materialsDetail(MaterialsDetailFeature.State(material: material))
        }
    }
}

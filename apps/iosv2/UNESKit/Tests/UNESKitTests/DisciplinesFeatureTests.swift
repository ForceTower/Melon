import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct DisciplinesFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)

    private struct Boom: Error, LocalizedError {
        var errorDescription: String? { "boom" }
    }

    @Test
    func taskHydratesFromTheMirrorBeforeRefreshing() async {
        var staleTemplate = DisciplinesOverview.preview(now: Self.referenceDate)
        staleTemplate.pending = []
        let stale = staleTemplate
        let fresh = DisciplinesOverview.preview(now: Self.referenceDate)
        let (updates, mirror) = AsyncStream.makeStream(of: DisciplinesOverview.self)
        // The observation replays the stale mirror before the refresh lands.
        mirror.yield(stale)

        let store = TestStore(initialState: DisciplinesFeature.State()) {
            DisciplinesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.observe = { updates }
            $0.disciplinesRepository.refresh = { _ in
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
    func refreshFailureKeepsTheStaleOverview() async {
        let cached = DisciplinesOverview.preview(now: Self.referenceDate)
        let (updates, mirror) = AsyncStream.makeStream(of: DisciplinesOverview.self)
        mirror.yield(cached)
        mirror.finish()

        let store = TestStore(initialState: DisciplinesFeature.State()) {
            DisciplinesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.observe = { updates }
            $0.disciplinesRepository.cached = { _ in cached }
            $0.disciplinesRepository.refresh = { _ in throw Boom() }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.overviewUpdated(cached)) {
            $0.isLoading = false
            $0.overview = cached
        }
        // Stale beats an error screen: the failed refresh falls back to the
        // mirror instead of surfacing.
        await store.receive(.overviewUpdated(cached))
    }

    @Test
    func downloadedSemesterLandsThroughTheObservation() async {
        var downloadedTemplate = DisciplinesOverview.preview(now: Self.referenceDate)
        downloadedTemplate.pending.removeAll { $0.id == "sem-2024-2" }
        let downloaded = downloadedTemplate
        let (updates, mirror) = AsyncStream.makeStream(of: DisciplinesOverview.self)

        var seeded = DisciplinesFeature.State()
        seeded.overview = .preview(now: Self.referenceDate)

        let store = TestStore(initialState: seeded) {
            DisciplinesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.observe = { updates }
            $0.disciplinesRepository.downloadSemester = { semesterId, _ in
                #expect(semesterId == "sem-2024-2")
            }
        }

        // The mirror already has data, so this only (re)subscribes.
        await store.send(.task)
        await store.send(.downloadSemesterTapped("sem-2024-2")) {
            $0.downloadingSemesterIds = ["sem-2024-2"]
        }
        await store.receive(.semesterDownloaded("sem-2024-2")) {
            $0.downloadingSemesterIds = []
            $0.recentlyDownloadedIds = ["sem-2024-2"]
        }
        // The download rewrote the mirror; the expanded overview arrives
        // through the observation, not the download effect.
        mirror.yield(downloaded)
        mirror.finish()
        await store.receive(.overviewUpdated(downloaded)) {
            $0.overview = downloaded
        }
        await store.finish()
    }

    @Test
    func failedDownloadSurfacesAnAlert() async {
        let store = TestStore(initialState: DisciplinesFeature.State()) {
            DisciplinesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.downloadSemester = { _, _ in throw Boom() }
        }

        await store.send(.downloadSemesterTapped("sem-2024-2")) {
            $0.downloadingSemesterIds = ["sem-2024-2"]
        }
        await store.receive(.semesterDownloadFailed("sem-2024-2", "boom")) {
            $0.downloadingSemesterIds = []
            $0.alert = AlertState {
                TextState("Não deu para baixar o semestre")
            } message: {
                TextState("boom")
            }
        }
    }

    @Test
    func tappingADisciplinePushesItsDetail() async {
        let discipline = DisciplinesOverview.preview(now: Self.referenceDate).current!.disciplines[0]

        let store = TestStore(initialState: DisciplinesFeature.State()) {
            DisciplinesFeature()
        }

        await store.send(.disciplineTapped(semesterId: "sem-2026-1", discipline: discipline)) {
            $0.path.append(
                .detail(DisciplineDetailFeature.State(summary: discipline, semesterId: "sem-2026-1"))
            )
        }
    }
}

@MainActor
struct DisciplineDetailFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)

    @Test
    func taskStreamsTheDetailFromTheMirror() async {
        let summary = DisciplinesOverview.preview(now: Self.referenceDate).current!.disciplines[0]
        let detail = DisciplineDetail.preview(now: Self.referenceDate)
        let (updates, mirror) = AsyncStream.makeStream(of: DisciplineDetail.self)
        mirror.yield(detail)
        mirror.finish()

        let store = TestStore(
            initialState: DisciplineDetailFeature.State(summary: summary, semesterId: "sem-2026-1")
        ) {
            DisciplineDetailFeature()
        } withDependencies: {
            $0.disciplinesRepository.observeDetail = { semesterId, disciplineId in
                #expect(semesterId == "sem-2026-1")
                #expect(disciplineId == summary.id)
                return updates
            }
        }

        await store.send(.task)
        await store.receive(.detailUpdated(detail)) {
            $0.detail = detail
            $0.name = detail.name
            $0.colorIndex = detail.colorIndex
        }
    }

    @Test
    func groupFilterSticksInState() async {
        let summary = DisciplinesOverview.preview(now: Self.referenceDate).current!.disciplines[2]

        let store = TestStore(
            initialState: DisciplineDetailFeature.State(summary: summary, semesterId: "sem-2026-1")
        ) {
            DisciplineDetailFeature()
        }

        await store.send(.groupSelected("T01P01")) {
            $0.selectedGroup = "T01P01"
        }
        await store.send(.groupSelected(nil)) {
            $0.selectedGroup = nil
        }
    }
}

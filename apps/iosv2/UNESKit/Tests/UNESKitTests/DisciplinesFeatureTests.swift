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

        let store = TestStore(initialState: DisciplinesFeature.State()) {
            DisciplinesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.cached = { _ in stale }
            $0.disciplinesRepository.refresh = { _ in fresh }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.hydrated(stale)) {
            $0.isLoading = false
            $0.overview = stale
        }
        await store.receive(.overviewLoaded(fresh)) {
            $0.overview = fresh
        }
    }

    @Test
    func refreshFailureKeepsTheStaleOverview() async {
        let cached = DisciplinesOverview.preview(now: Self.referenceDate)

        let store = TestStore(initialState: DisciplinesFeature.State()) {
            DisciplinesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.cached = { _ in cached }
            $0.disciplinesRepository.refresh = { _ in throw Boom() }
        }

        await store.send(.task) {
            $0.isLoading = true
        }
        await store.receive(.hydrated(cached)) {
            $0.isLoading = false
            $0.overview = cached
        }
        await store.receive(.overviewFailed("boom"))
    }

    @Test
    func downloadingASemesterFoldsItIntoTheOverview() async {
        var downloadedTemplate = DisciplinesOverview.preview(now: Self.referenceDate)
        downloadedTemplate.pending.removeAll { $0.id == "sem-2024-2" }
        let downloaded = downloadedTemplate

        let store = TestStore(initialState: DisciplinesFeature.State()) {
            DisciplinesFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.downloadSemester = { semesterId, _ in
                #expect(semesterId == "sem-2024-2")
                return downloaded
            }
        }

        await store.send(.downloadSemesterTapped("sem-2024-2")) {
            $0.downloadingSemesterIds = ["sem-2024-2"]
        }
        await store.receive(.semesterDownloaded("sem-2024-2", downloaded)) {
            $0.downloadingSemesterIds = []
            $0.recentlyDownloadedIds = ["sem-2024-2"]
            $0.overview = downloaded
        }
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
    func taskLoadsTheDetailFromTheMirror() async {
        let summary = DisciplinesOverview.preview(now: Self.referenceDate).current!.disciplines[0]
        let detail = DisciplineDetail.preview(now: Self.referenceDate)

        let store = TestStore(
            initialState: DisciplineDetailFeature.State(summary: summary, semesterId: "sem-2026-1")
        ) {
            DisciplineDetailFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.detail = { semesterId, disciplineId, _ in
                #expect(semesterId == "sem-2026-1")
                #expect(disciplineId == summary.id)
                return detail
            }
        }

        await store.send(.task)
        await store.receive(.detailLoaded(detail)) {
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

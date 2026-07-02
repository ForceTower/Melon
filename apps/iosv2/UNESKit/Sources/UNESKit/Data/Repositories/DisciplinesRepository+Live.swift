import ComposableArchitecture
import Foundation

extension DisciplinesRepository: DependencyKey {
    static let liveValue = DisciplinesRepository(
        cached: { now in
            @Dependency(\.database) var wrappedDatabase
            let mirror = MirrorStore(writer: wrappedDatabase)
            return try await mirror.cachedDisciplinesOverview(now: now)
        },
        refresh: { now in
            @Dependency(\.apiClient) var wrappedClient
            @Dependency(\.database) var wrappedDatabase
            let apiClient = wrappedClient
            let mirror = MirrorStore(writer: wrappedDatabase)

            let list: SemesterListDTO = try await apiClient.get(from: "api/sync/semesters")
            var snapshot: SemesterSnapshot?
            if let active = list.semesters.map(\.domain).active(today: now.dayStamp) {
                let payload: SemesterPayloadDTO = try await apiClient.get(from: "api/sync/semesters/\(active.id)")
                snapshot = payload.snapshot
            }
            try await mirror.apply(semesters: list.semesters.map(\.record), snapshot: snapshot, syncedAt: now)
        },
        downloadSemester: { semesterId, now in
            @Dependency(\.apiClient) var wrappedClient
            @Dependency(\.database) var wrappedDatabase
            let apiClient = wrappedClient
            let mirror = MirrorStore(writer: wrappedDatabase)

            let payload: SemesterPayloadDTO = try await apiClient.get(from: "api/sync/semesters/\(semesterId)")
            try await mirror.apply(semesters: [], snapshot: payload.snapshot, syncedAt: now)
        },
        observe: {
            @Dependency(\.database) var wrappedDatabase
            @Dependency(\.date) var wrappedDate
            let date = wrappedDate
            let mirror = MirrorStore(writer: wrappedDatabase)
            return AsyncStream { continuation in
                let task = Task {
                    // Observation only fails if the database itself is gone;
                    // ending the stream is all there is to do.
                    do {
                        for try await overview in mirror.disciplinesOverviewUpdates(now: { date.now }) {
                            if let overview {
                                continuation.yield(overview)
                            }
                        }
                    } catch {}
                    continuation.finish()
                }
                continuation.onTermination = { _ in task.cancel() }
            }
        },
        observeDetail: { semesterId, disciplineId in
            @Dependency(\.database) var wrappedDatabase
            @Dependency(\.date) var wrappedDate
            let date = wrappedDate
            let mirror = MirrorStore(writer: wrappedDatabase)
            return AsyncStream { continuation in
                let task = Task {
                    do {
                        let updates = mirror.disciplineDetailUpdates(
                            semesterId: semesterId,
                            disciplineId: disciplineId,
                            now: { date.now }
                        )
                        for try await detail in updates {
                            if let detail {
                                continuation.yield(detail)
                            }
                        }
                    } catch {}
                    continuation.finish()
                }
                continuation.onTermination = { _ in task.cancel() }
            }
        }
    )
}

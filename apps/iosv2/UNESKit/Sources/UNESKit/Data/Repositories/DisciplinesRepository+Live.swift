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

            return try await mirror.disciplinesOverview(now: now)
        },
        downloadSemester: { semesterId, now in
            @Dependency(\.apiClient) var wrappedClient
            @Dependency(\.database) var wrappedDatabase
            let apiClient = wrappedClient
            let mirror = MirrorStore(writer: wrappedDatabase)

            let payload: SemesterPayloadDTO = try await apiClient.get(from: "api/sync/semesters/\(semesterId)")
            try await mirror.apply(semesters: [], snapshot: payload.snapshot, syncedAt: now)

            return try await mirror.disciplinesOverview(now: now)
        }
    )
}

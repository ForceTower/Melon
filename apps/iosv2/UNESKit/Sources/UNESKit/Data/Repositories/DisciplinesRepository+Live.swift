import ComposableArchitecture
import Foundation

private let log = Log.scoped("DisciplinesRepository")

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

            log.debug("refresh start")
            do {
                let list: SemesterListDTO = try await apiClient.get(from: "api/sync/semesters")
                var snapshot: SemesterSnapshot?
                if let active = list.semesters.map(\.domain).active(today: now.dayStamp) {
                    let payload: SemesterPayloadDTO = try await apiClient.get(from: "api/sync/semesters/\(active.id)")
                    snapshot = payload.snapshot
                }
                try await mirror.apply(semesters: list.semesters.map(\.record), snapshot: snapshot, syncedAt: now)
                log.info("refresh ok semesters=\(list.semesters.count) activeSnapshot=\(snapshot != nil)")
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("refresh unauthorized")
                case let APIError.server(status, message):
                    log.warn("refresh server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("refresh 2xx envelope had null data")
                case is URLError:
                    log.warn("refresh transport failure", error: error)
                default:
                    log.error("refresh failed", error: error)
                }
                throw error
            }
        },
        downloadSemester: { semesterId, now in
            @Dependency(\.apiClient) var wrappedClient
            @Dependency(\.database) var wrappedDatabase
            let apiClient = wrappedClient
            let mirror = MirrorStore(writer: wrappedDatabase)

            log.debug("downloadSemester start id=\(semesterId)")
            do {
                let payload: SemesterPayloadDTO = try await apiClient.get(from: "api/sync/semesters/\(semesterId)")
                try await mirror.apply(semesters: [], snapshot: payload.snapshot, syncedAt: now)
                log.info("downloadSemester ok id=\(semesterId)")
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("downloadSemester unauthorized")
                case let APIError.server(status, message):
                    log.warn("downloadSemester server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("downloadSemester 2xx envelope had null data")
                case is URLError:
                    log.warn("downloadSemester transport failure", error: error)
                default:
                    log.error("downloadSemester failed", error: error)
                }
                throw error
            }
        },
        observe: {
            @Dependency(\.database) var wrappedDatabase
            @Dependency(\.date) var wrappedDate
            let date = wrappedDate
            let mirror = MirrorStore(writer: wrappedDatabase)
            log.debug("observe subscribed")
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
                    } catch {
                        log.error("observe failed", error: error)
                    }
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
            log.debug("observeDetail subscribed semesterId=\(semesterId) disciplineId=\(disciplineId)")
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
                    } catch {
                        log.error("observeDetail failed", error: error)
                    }
                    continuation.finish()
                }
                continuation.onTermination = { _ in task.cancel() }
            }
        },
        detailCached: { semesterId, disciplineId, now in
            @Dependency(\.database) var wrappedDatabase
            let mirror = MirrorStore(writer: wrappedDatabase)
            return try await mirror.disciplineDetail(semesterId: semesterId, disciplineId: disciplineId, now: now)
        }
    )
}

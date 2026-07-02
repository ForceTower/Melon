import ComposableArchitecture
import Foundation

private let log = Log.scoped("HomeRepository")

extension HomeRepository: DependencyKey {
    static let liveValue = HomeRepository(
        cached: { now in
            @Dependency(\.database) var wrappedDatabase
            let mirror = MirrorStore(writer: wrappedDatabase)
            return try await mirror.cachedOverview(now: now)
        },
        refresh: { now in
            @Dependency(\.apiClient) var wrappedClient
            @Dependency(\.database) var wrappedDatabase
            let apiClient = wrappedClient
            let mirror = MirrorStore(writer: wrappedDatabase)

            log.debug("refresh start")
            async let inbox: MessageListDTO = apiClient.get(from: "api/sync/messages")

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

            // The inbox is a garnish here — a failure should not sink Home.
            if let inbox = try? await inbox {
                try? await mirror.upsertMessages(inbox.page, syncedAt: now)
                log.debug("refresh inbox merged count=\(inbox.page.messages.count)")
            } else {
                log.warn("refresh inbox fetch failed")
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
                        for try await cached in mirror.overviewUpdates(now: { date.now }) {
                            if let cached {
                                continuation.yield(cached)
                            }
                        }
                    } catch {
                        log.error("observe failed", error: error)
                    }
                    continuation.finish()
                }
                continuation.onTermination = { _ in task.cancel() }
            }
        }
    )
}

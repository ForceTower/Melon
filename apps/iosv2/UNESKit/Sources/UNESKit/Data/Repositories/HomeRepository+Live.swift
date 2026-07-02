import ComposableArchitecture
import Foundation

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

            async let inbox: MessageListDTO = apiClient.get(from: "api/sync/messages")

            let list: SemesterListDTO = try await apiClient.get(from: "api/sync/semesters")
            var snapshot: SemesterSnapshot?
            if let active = list.semesters.map(\.domain).active(today: now.dayStamp) {
                let payload: SemesterPayloadDTO = try await apiClient.get(from: "api/sync/semesters/\(active.id)")
                snapshot = payload.snapshot
            }
            try await mirror.apply(semesters: list.semesters.map(\.record), snapshot: snapshot, syncedAt: now)

            // The inbox is a garnish here — a failure should not sink Home.
            if let inbox = try? await inbox {
                try? await mirror.upsertMessages(inbox.page, syncedAt: now)
            }
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
                        for try await cached in mirror.overviewUpdates(now: { date.now }) {
                            if let cached {
                                continuation.yield(cached)
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

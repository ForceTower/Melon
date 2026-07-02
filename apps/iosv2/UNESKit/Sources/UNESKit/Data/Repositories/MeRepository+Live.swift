import ComposableArchitecture
import Foundation

extension MeRepository: DependencyKey {
    static let liveValue = MeRepository(
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
                        for try await cached in mirror.meOverviewUpdates(now: { date.now }) {
                            if let cached {
                                continuation.yield(cached)
                            }
                        }
                    } catch {}
                    continuation.finish()
                }
                continuation.onTermination = { _ in task.cancel() }
            }
        },
        localData: {
            @Dependency(\.database) var wrappedDatabase
            return try await MirrorStore(writer: wrappedDatabase).localDataSummary()
        },
        wipeLocalData: {
            @Dependency(\.database) var wrappedDatabase
            try await MirrorStore(writer: wrappedDatabase).wipe()
        }
    )
}

import ComposableArchitecture
import Foundation

private let log = Log.scoped("MeRepository")

extension MeRepository: DependencyKey {
    static let liveValue = MeRepository(
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
                        for try await cached in mirror.meOverviewUpdates(now: { date.now }) {
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
        },
        localData: {
            @Dependency(\.database) var wrappedDatabase
            do {
                let summary = try await MirrorStore(writer: wrappedDatabase).localDataSummary()
                log.info("localData ok semesters=\(summary.semesters) messages=\(summary.messages)")
                return summary
            } catch {
                log.error("localData failed", error: error)
                throw error
            }
        },
        wipeLocalData: {
            @Dependency(\.database) var wrappedDatabase
            do {
                try await MirrorStore(writer: wrappedDatabase).wipe()
                log.info("wipeLocalData ok")
            } catch {
                log.error("wipeLocalData failed", error: error)
                throw error
            }
        }
    )
}

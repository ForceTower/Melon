import ComposableArchitecture
import Foundation

private let log = Log.scoped("Spotlight")

/// Keeps the on-device Spotlight index fed: observes the mirror's Spotlight
/// projection and applies coalesced diffs through the app target's writer.
@DependencyClient
struct SpotlightSyncClient: Sendable {
    /// Runs for the app's whole lifetime; cancelling the task stops it.
    var run: @Sendable () async -> Void
}

extension SpotlightSyncClient: DependencyKey {
    /// The real indexer needs the app target's entity types, so the app
    /// installs it at launch via `SpotlightSupport.installIndexer`; this
    /// default makes a missed installation loud in the logs, not invisible.
    static let liveValue = SpotlightSyncClient(
        run: { log.warn("indexer not installed — Spotlight index will not update") }
    )
}

extension SpotlightSyncClient: TestDependencyKey {
    static let testValue = SpotlightSyncClient()
    static let previewValue = SpotlightSyncClient(run: {})
}

extension DependencyValues {
    var spotlightSync: SpotlightSyncClient {
        get { self[SpotlightSyncClient.self] }
        set { self[SpotlightSyncClient.self] = newValue }
    }
}

/// The `CSSearchableIndex` boundary, implemented by the app target (the
/// `AppEntity` types live there next to the app's catalog); UNESKit owns
/// everything up to it — observation, coalescing, diffing, the ledger.
public protocol SpotlightIndexWriter: Sendable {
    func index(disciplines: [SpotlightDiscipline], messages: [SpotlightMessage]) async throws
    func delete(disciplineIds: [String], messageIds: [String]) async throws
    func deleteAll() async throws
}

/// Everything the app target needs from the package for Spotlight: the
/// indexer installation and the entity-query lookups.
public enum SpotlightSupport {
    /// Called once from `AppDelegate.didFinishLaunching`, before any scene
    /// task reads dependencies.
    public static func installIndexer(_ writer: some SpotlightIndexWriter) {
        prepareDependencies {
            $0.spotlightSync = SpotlightSyncClient(run: { await run(writer: writer) })
        }
    }

    // MARK: Entity-query lookups

    public static func disciplines(for identifiers: [String]) async -> [SpotlightDiscipline] {
        @Dependency(\.database) var database
        @Dependency(\.date) var date
        return (try? await MirrorStore(writer: database)
            .spotlightDisciplines(ids: identifiers, now: date.now)) ?? []
    }

    /// Active semester's disciplines in Turmas order.
    public static func suggestedDisciplines() async -> [SpotlightDiscipline] {
        @Dependency(\.database) var database
        @Dependency(\.date) var date
        return (try? await MirrorStore(writer: database)
            .spotlightSuggestedDisciplines(now: date.now)) ?? []
    }

    public static func disciplines(matching string: String) async -> [SpotlightDiscipline] {
        @Dependency(\.database) var database
        @Dependency(\.date) var date
        return (try? await MirrorStore(writer: database)
            .spotlightDisciplines(matching: string, now: date.now)) ?? []
    }

    public static func messages(for identifiers: [String]) async -> [SpotlightMessage] {
        @Dependency(\.database) var database
        return (try? await MirrorStore(writer: database).spotlightMessages(ids: identifiers)) ?? []
    }

    public static func suggestedMessages() async -> [SpotlightMessage] {
        @Dependency(\.database) var database
        return (try? await MirrorStore(writer: database).spotlightRecentMessages(limit: 5)) ?? []
    }

    // MARK: Indexer loop

    /// One `indexAppEntities` call per chunk bounds the first full pass
    /// (the mirrored inbox can be thousands of messages).
    private static let chunkSize = 200

    static func run(writer: some SpotlightIndexWriter) async {
        log.debug("indexer subscribed")
        var ledger: SpotlightIndexLedger
        if let loaded = SpotlightIndexLedger.load() {
            ledger = loaded
        } else {
            // The persisted ledger predates the current schema: the indexed
            // items' identifier formats may have changed, so re-indexing
            // over them would leave duplicates — clean slate instead.
            do {
                try await writer.deleteAll()
                log.info("index wiped reason=schema")
            } catch {
                log.error("schema wipe failed", error: error)
            }
            ledger = SpotlightIndexLedger()
            ledger.save()
        }
        for await snapshot in updates() {
            let diff = SpotlightDiff.compute(ledger: ledger, snapshot: snapshot)
            guard !diff.isEmpty else { continue }
            ledger = await apply(diff, ledger: ledger, writer: writer)
            ledger.save()
        }
    }

    static func apply(
        _ diff: SpotlightDiff,
        ledger: SpotlightIndexLedger,
        writer: some SpotlightIndexWriter
    ) async -> SpotlightIndexLedger {
        if diff.wipeAll {
            do {
                try await writer.deleteAll()
                log.info("index wiped")
                return SpotlightIndexLedger()
            } catch {
                log.error("index wipe failed", error: error)
                return ledger
            }
        }

        // A failed write leaves that kind's ledger untouched, so the next
        // emission retries the same delta (index upserts are idempotent).
        var next = ledger
        do {
            for chunk in diff.disciplinesToIndex.chunked(into: chunkSize) {
                try await writer.index(disciplines: chunk, messages: [])
            }
            if !diff.disciplineIdsToDelete.isEmpty {
                try await writer.delete(disciplineIds: diff.disciplineIdsToDelete, messageIds: [])
            }
            next.applyDisciplines(diff)
        } catch {
            log.error("discipline index apply failed", error: error)
        }
        do {
            for chunk in diff.messagesToIndex.chunked(into: chunkSize) {
                try await writer.index(disciplines: [], messages: chunk)
            }
            if !diff.messageIdsToDelete.isEmpty {
                try await writer.delete(disciplineIds: [], messageIds: diff.messageIdsToDelete)
            }
            next.applyMessages(diff)
        } catch {
            log.error("message index apply failed", error: error)
        }
        log.info(
            "indexed disciplines=+\(diff.disciplinesToIndex.count)/-\(diff.disciplineIdsToDelete.count)"
                + " messages=+\(diff.messagesToIndex.count)/-\(diff.messageIdsToDelete.count)"
        )
        return next
    }

    /// The mirror's projection stream, coalesced: emissions settle for the
    /// quiet period before delivery, so a backfill burst (semester by
    /// semester, message pages in a loop) applies once, not per write.
    static func updates(quiet: Duration = .seconds(2)) -> AsyncStream<SpotlightSnapshot?> {
        AsyncStream { continuation in
            let task = Task {
                @Dependency(\.database) var database
                @Dependency(\.date) var date
                @Dependency(\.continuousClock) var clock
                let mirror = MirrorStore(writer: database)
                var pending: Task<Void, Never>?
                // Observation only fails if the database itself is gone;
                // there is nothing left to index then.
                do {
                    for try await snapshot in mirror.spotlightUpdates(now: { date.now }) {
                        pending?.cancel()
                        pending = Task { @Sendable [clock] in
                            try? await clock.sleep(for: quiet)
                            guard !Task.isCancelled else { return }
                            continuation.yield(snapshot)
                        }
                    }
                } catch {
                    log.error("projection observation failed", error: error)
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}

extension Array {
    fileprivate func chunked(into size: Int) -> [[Element]] {
        stride(from: 0, to: count, by: size).map { Array(self[$0..<Swift.min($0 + size, count)]) }
    }
}

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

/// One kind's worth of items to write. The loop sends one kind per call so
/// a failed write only holds back that kind's ledger update.
public struct SpotlightIndexBatch: Sendable {
    public var disciplines: [SpotlightDiscipline] = []
    public var messages: [SpotlightMessage] = []
    public var evaluations: [SpotlightEvaluation] = []
    public var lectures: [SpotlightLecture] = []
    public var sessions: [SpotlightSession] = []
    public var personalEvents: [SpotlightPersonalEvent] = []
}

/// Entity identifiers to remove, per kind.
public struct SpotlightDeleteBatch: Sendable {
    public var disciplineIds: [String] = []
    public var messageIds: [String] = []
    public var evaluationIds: [String] = []
    public var lectureIds: [String] = []
    public var sessionIds: [String] = []
    public var personalEventIds: [String] = []
}

/// The `CSSearchableIndex` boundary, implemented by the app target (the
/// `AppEntity` types live there next to the app's catalog); UNESKit owns
/// everything up to it — observation, coalescing, diffing, the ledger.
public protocol SpotlightIndexWriter: Sendable {
    func index(_ batch: SpotlightIndexBatch) async throws
    func delete(_ batch: SpotlightDeleteBatch) async throws
    func deleteAll() async throws
    /// The discipline set changed (or the index was wiped) — the app target
    /// re-registers its App Shortcut parameters so Siri's phrase-embedded
    /// discipline names track the mirror.
    func disciplinesDidChange() async
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

    public static func evaluations(for identifiers: [String]) async -> [SpotlightEvaluation] {
        @Dependency(\.database) var database
        @Dependency(\.date) var date
        return (try? await MirrorStore(writer: database)
            .spotlightEvaluations(ids: identifiers, now: date.now)) ?? []
    }

    /// Scheduled pending evaluations, soonest first.
    public static func suggestedEvaluations() async -> [SpotlightEvaluation] {
        @Dependency(\.database) var database
        @Dependency(\.date) var date
        return (try? await MirrorStore(writer: database)
            .spotlightSuggestedEvaluations(now: date.now)) ?? []
    }

    public static func lectures(for identifiers: [String]) async -> [SpotlightLecture] {
        @Dependency(\.database) var database
        @Dependency(\.date) var date
        return (try? await MirrorStore(writer: database)
            .spotlightLectures(ids: identifiers, now: date.now)) ?? []
    }

    /// The most recently dated lectures.
    public static func suggestedLectures() async -> [SpotlightLecture] {
        @Dependency(\.database) var database
        @Dependency(\.date) var date
        return (try? await MirrorStore(writer: database)
            .spotlightSuggestedLectures(now: date.now, limit: 5)) ?? []
    }

    public static func sessions(for identifiers: [String]) async -> [SpotlightSession] {
        @Dependency(\.database) var database
        @Dependency(\.date) var date
        return (try? await MirrorStore(writer: database)
            .spotlightSessions(ids: identifiers, now: date.now)) ?? []
    }

    /// Every weekly session of the active semester, Sunday first.
    public static func suggestedSessions() async -> [SpotlightSession] {
        @Dependency(\.database) var database
        @Dependency(\.date) var date
        return (try? await MirrorStore(writer: database)
            .spotlightSuggestedSessions(now: date.now)) ?? []
    }

    /// Which calendar-shaped kind an entity identifier names; nil for the
    /// other kinds.
    public static func eventKind(of identifier: String) -> SpotlightEventKind? {
        SpotlightEntityID.eventKind(of: identifier)
    }

    public static func personalEvents(for identifiers: [String]) async -> [SpotlightPersonalEvent] {
        @Dependency(\.database) var database
        return (try? await MirrorStore(writer: database).spotlightPersonalEvents(ids: identifiers)) ?? []
    }

    /// The student's own entries, earliest first.
    public static func suggestedPersonalEvents() async -> [SpotlightPersonalEvent] {
        @Dependency(\.database) var database
        return (try? await MirrorStore(writer: database).spotlightSuggestedPersonalEvents()) ?? []
    }

    // MARK: Indexer loop

    /// One index call per chunk bounds the first full pass (the mirrored
    /// inbox can be thousands of messages).
    private static let chunkSize = 200

    static func run(writer: some SpotlightIndexWriter) async {
        log.debug("indexer subscribed")
        @Dependency(\.database) var database
        let mirror = MirrorStore(writer: database)

        var ledger: SpotlightIndexLedger
        // A failed read counts as no usable ledger — same treatment as a
        // version mismatch.
        if let loaded = (try? await mirror.spotlightLedger()) ?? nil {
            ledger = loaded
        } else {
            // No usable ledger (another schema version or OS major, the
            // legacy JSON file, or a DEBUG schema erase): the indexed
            // items' identifier formats or entity types may have changed,
            // so re-indexing over them would leave duplicates — clean slate
            // instead. The fresh ledger and the legacy-file deletion land
            // only after the wipe succeeds; a failed wipe keeps its signal
            // and retries next launch.
            do {
                try await writer.deleteAll()
                log.info("index wiped reason=schema")
            } catch {
                log.error("schema wipe failed", error: error)
                return
            }
            MirrorStore.deleteLegacySpotlightLedgerFile()
            ledger = SpotlightIndexLedger()
            await save(ledger, to: mirror)
        }
        for await snapshot in updates() {
            let diff = SpotlightDiff.compute(ledger: ledger, snapshot: snapshot)
            guard !diff.isEmpty else { continue }
            ledger = await apply(diff, ledger: ledger, writer: writer)
            await save(ledger, to: mirror)
        }
    }

    /// A failed save only costs a retry: the next emission diffs against
    /// the stale ledger and re-applies the same idempotent delta.
    private static func save(_ ledger: SpotlightIndexLedger, to mirror: MirrorStore) async {
        do {
            try await mirror.saveSpotlightLedger(ledger)
        } catch {
            log.warn("ledger write failed", error: error)
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
                await writer.disciplinesDidChange()
                return SpotlightIndexLedger()
            } catch {
                log.error("index wipe failed", error: error)
                return ledger
            }
        }

        // A failed write leaves that kind's ledger untouched, so the next
        // emission retries the same delta (index upserts are idempotent).
        var next = ledger
        if await applyKind(
            "discipline", diff.disciplinesToIndex, diff.disciplineIdsToDelete, writer: writer,
            index: { SpotlightIndexBatch(disciplines: $0) }, delete: { SpotlightDeleteBatch(disciplineIds: $0) }
        ) {
            next.applyDisciplines(diff)
            if !diff.disciplinesToIndex.isEmpty || !diff.disciplineIdsToDelete.isEmpty {
                // Ride the existing choke point: the Prova Final shortcut
                // embeds discipline names in its phrases, so a discipline
                // change re-registers the shortcut parameters.
                await writer.disciplinesDidChange()
            }
        }
        if await applyKind(
            "message", diff.messagesToIndex, diff.messageIdsToDelete, writer: writer,
            index: { SpotlightIndexBatch(messages: $0) }, delete: { SpotlightDeleteBatch(messageIds: $0) }
        ) {
            next.applyMessages(diff)
        }
        if await applyKind(
            "evaluation", diff.evaluationsToIndex, diff.evaluationIdsToDelete, writer: writer,
            index: { SpotlightIndexBatch(evaluations: $0) }, delete: { SpotlightDeleteBatch(evaluationIds: $0) }
        ) {
            next.applyEvaluations(diff)
        }
        if await applyKind(
            "lecture", diff.lecturesToIndex, diff.lectureIdsToDelete, writer: writer,
            index: { SpotlightIndexBatch(lectures: $0) }, delete: { SpotlightDeleteBatch(lectureIds: $0) }
        ) {
            next.applyLectures(diff)
        }
        if await applyKind(
            "session", diff.sessionsToIndex, diff.sessionIdsToDelete, writer: writer,
            index: { SpotlightIndexBatch(sessions: $0) }, delete: { SpotlightDeleteBatch(sessionIds: $0) }
        ) {
            next.applySessions(diff)
        }
        if await applyKind(
            "personalEvent", diff.personalEventsToIndex, diff.personalEventIdsToDelete, writer: writer,
            index: { SpotlightIndexBatch(personalEvents: $0) }, delete: { SpotlightDeleteBatch(personalEventIds: $0) }
        ) {
            next.applyPersonalEvents(diff)
        }
        log.info(
            "indexed disciplines=+\(diff.disciplinesToIndex.count)/-\(diff.disciplineIdsToDelete.count)"
                + " messages=+\(diff.messagesToIndex.count)/-\(diff.messageIdsToDelete.count)"
                + " evaluations=+\(diff.evaluationsToIndex.count)/-\(diff.evaluationIdsToDelete.count)"
                + " lectures=+\(diff.lecturesToIndex.count)/-\(diff.lectureIdsToDelete.count)"
                + " sessions=+\(diff.sessionsToIndex.count)/-\(diff.sessionIdsToDelete.count)"
                + " personalEvents=+\(diff.personalEventsToIndex.count)/-\(diff.personalEventIdsToDelete.count)"
        )
        return next
    }

    /// Writes one kind's upserts (chunked) and deletes; false when any
    /// write failed, so the caller leaves that kind's ledger untouched.
    private static func applyKind<Item>(
        _ kind: String,
        _ items: [Item],
        _ deletions: [String],
        writer: some SpotlightIndexWriter,
        index: ([Item]) -> SpotlightIndexBatch,
        delete: ([String]) -> SpotlightDeleteBatch
    ) async -> Bool {
        do {
            for chunk in items.chunked(into: chunkSize) {
                try await writer.index(index(chunk))
            }
            if !deletions.isEmpty {
                try await writer.delete(delete(deletions))
            }
            return true
        } catch {
            log.error("\(kind) index apply failed", error: error)
            return false
        }
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

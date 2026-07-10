import ComposableArchitecture
import Foundation

private let log = Log.scoped("SyncRepository")

extension SyncRepository: DependencyKey {
    static let liveValue = SyncRepository(
        ping: {
            @Dependency(\.apiClient) var apiClient
            log.debug("ping start")
            do {
                try await apiClient.post(to: "api/me/ping")
                log.info("ping ok")
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("ping unauthorized")
                case let APIError.server(status, message):
                    log.warn("ping server \(status) message=\(message ?? "<none>")")
                case is URLError:
                    log.warn("ping transport failure", error: error)
                default:
                    log.error("ping failed", error: error)
                }
                throw error
            }
        },
        onboardingStatus: {
            @Dependency(\.apiClient) var apiClient
            log.debug("onboardingStatus start")
            do {
                let dto: OnboardingStatusDTO = try await apiClient.get(from: "api/sync/onboarding-status")
                let status = dto.domain
                log.info(
                    "onboardingStatus ok courseLinked=\(status.courseLinked) semesters=\(status.semesters.state) messages=\(status.messages.state)"
                )
                return status
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("onboardingStatus unauthorized")
                case let APIError.server(status, message):
                    log.warn("onboardingStatus server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("onboardingStatus 2xx envelope had null data")
                case is URLError:
                    log.warn("onboardingStatus transport failure", error: error)
                default:
                    log.error("onboardingStatus failed", error: error)
                }
                throw error
            }
        },
        semesters: {
            @Dependency(\.apiClient) var apiClient
            log.debug("semesters start")
            do {
                let dto: SemesterListDTO = try await apiClient.get(from: "api/sync/semesters")
                log.info("semesters ok count=\(dto.semesters.count)")
                return dto.semesters.map(\.domain)
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("semesters unauthorized")
                case let APIError.server(status, message):
                    log.warn("semesters server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("semesters 2xx envelope had null data")
                case is URLError:
                    log.warn("semesters transport failure", error: error)
                default:
                    log.error("semesters failed", error: error)
                }
                throw error
            }
        },
        readyOverview: { semester, now in
            @Dependency(\.apiClient) var apiClient
            log.debug("readyOverview start semesterId=\(semester.id)")
            do {
                let dto: SemesterPayloadDTO = try await apiClient.get(from: "api/sync/semesters/\(semester.id)")
                log.info("readyOverview ok semesterId=\(semester.id)")
                return dto.snapshot.readyOverview(now: now)
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("readyOverview unauthorized")
                case let APIError.server(status, message):
                    log.warn("readyOverview server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("readyOverview 2xx envelope had null data")
                case is URLError:
                    log.warn("readyOverview transport failure", error: error)
                default:
                    log.error("readyOverview failed", error: error)
                }
                throw error
            }
        },
        fetchFirstMessagesPage: {
            @Dependency(\.apiClient) var apiClient
            @Dependency(\.database) var database
            @Dependency(\.date.now) var now
            log.debug("fetchFirstMessagesPage start")
            do {
                let inbox: MessageListDTO = try await apiClient.get(from: "api/sync/messages")
                try await MirrorStore(writer: database).upsertMessages(inbox.page, syncedAt: now)
                log.info("fetchFirstMessagesPage ok count=\(inbox.page.messages.count)")
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("fetchFirstMessagesPage unauthorized")
                case let APIError.server(status, message):
                    log.warn("fetchFirstMessagesPage server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("fetchFirstMessagesPage 2xx envelope had null data")
                case is URLError:
                    log.warn("fetchFirstMessagesPage transport failure", error: error)
                default:
                    log.error("fetchFirstMessagesPage failed", error: error)
                }
                throw error
            }
        },
        backfillMirror: {
            @Dependency(\.apiClient) var apiClient
            @Dependency(\.database) var database
            @Dependency(\.date.now) var now
            let mirror = MirrorStore(writer: database)

            if try await mirror.isBackfillMirrorComplete() {
                log.debug("backfill skipped: already complete")
                return
            }
            log.info("backfill start")
            do {
                // Mirroring before the server's Phase 2 settles would persist a
                // partial archive that the completion flag then locks in.
                try await awaitTerminalOnboardingStatus()

                let list: SemesterListDTO = try await apiClient.get(from: "api/sync/semesters")
                try await mirror.apply(semesters: list.semesters.map(\.record), snapshot: nil, syncedAt: now)
                let semesters = list.semesters.map(\.domain)
                log.info("backfill mirroring semesters count=\(semesters.count)")
                for semester in semesters {
                    let payload: SemesterPayloadDTO = try await apiClient.get(from: "api/sync/semesters/\(semester.id)")
                    try await mirror.apply(semesters: [], snapshot: payload.snapshot, syncedAt: now)
                }

                var cursor: String?
                var pages = 0
                while pages < backfillMaxMessagePages {
                    let query = cursor.map { [URLQueryItem(name: "cursor", value: $0)] } ?? []
                    let inbox: MessageListDTO = try await apiClient.get(from: "api/sync/messages", query: query)
                    try await mirror.upsertMessages(inbox.page, syncedAt: now)
                    pages += 1
                    guard let next = inbox.nextCursor else { break }
                    cursor = next
                }
                if pages == backfillMaxMessagePages {
                    log.warn("backfill messages pagination hit page cap=\(backfillMaxMessagePages)")
                }

                try await mirror.setBackfillMirrorComplete()
                log.info("backfill complete pages=\(pages)")
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("backfill unauthorized")
                case let APIError.server(status, message):
                    log.warn("backfill server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("backfill 2xx envelope had null data")
                case is URLError:
                    log.warn("backfill transport failure", error: error)
                default:
                    log.error("backfill failed", error: error)
                }
                throw error
            }
        }
    )
}

private let backfillPollInterval: Duration = .seconds(5)
private let backfillMaxPollIterations = 60
private let backfillMaxMessagePages = 200

private func awaitTerminalOnboardingStatus() async throws {
    @Dependency(\.apiClient) var apiClient
    @Dependency(\.continuousClock) var clock
    for iteration in 0 ..< backfillMaxPollIterations {
        let dto: OnboardingStatusDTO = try await apiClient.get(from: "api/sync/onboarding-status")
        if isBackfillTerminal(dto.domain) { return }
        if iteration == backfillMaxPollIterations - 1 {
            log.warn("backfill polling hit cap without terminal status")
            return
        }
        try await clock.sleep(for: backfillPollInterval)
    }
}

// Phase 1 failure short-circuits Phase 2 server-side, so a failed initial phase
// is terminal for everything; otherwise both Phase 2 streams must settle.
private func isBackfillTerminal(_ status: OnboardingStatus) -> Bool {
    if status.initial.state == .failed { return true }
    guard status.initial.state == .done else { return false }
    return status.semesters.state.isTerminal && status.messages.state.isTerminal
}

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
        }
    )
}

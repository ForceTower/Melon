import ComposableArchitecture
import Foundation

private let log = Log.scoped("MessagesRepository")

extension MessagesRepository: DependencyKey {
    static let liveValue = MessagesRepository(
        cached: { now in
            @Dependency(\.database) var database
            return try await MirrorStore(writer: database).cachedMessagesOverview(now: now)
        },
        refresh: { now in
            @Dependency(\.apiClient) var apiClient
            @Dependency(\.database) var database
            let mirror = MirrorStore(writer: database)

            log.debug("refresh start")
            do {
                let inbox: MessageListDTO = try await apiClient.get(from: "api/sync/messages")
                try await mirror.upsertMessages(inbox.page, syncedAt: now)
                log.info("refresh ok count=\(inbox.page.messages.count)")
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
        observe: {
            @Dependency(\.database) var database
            @Dependency(\.date) var wrappedDate
            let date = wrappedDate
            let mirror = MirrorStore(writer: database)
            log.debug("observe subscribed")
            return AsyncStream { continuation in
                let task = Task {
                    // Observation only fails if the database itself is gone;
                    // ending the stream is all there is to do.
                    do {
                        for try await overview in mirror.messagesOverviewUpdates(now: { date.now }) {
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
        markRead: { id, now in
            @Dependency(\.database) var database
            try await MirrorStore(writer: database).markMessageRead(id: id, now: now)
            log.info("markRead ok id=\(id)")
        },
        markAllRead: { now in
            @Dependency(\.database) var database
            try await MirrorStore(writer: database).markAllMessagesRead(now: now)
            log.info("markAllRead ok")
        },
        setStarred: { id, starred in
            @Dependency(\.database) var database
            try await MirrorStore(writer: database).setMessageStarred(id: id, starred: starred)
            log.info("setStarred ok id=\(id) starred=\(starred)")
        }
    )
}

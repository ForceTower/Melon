import ComposableArchitecture
import Foundation

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

            let inbox: MessageListDTO = try await apiClient.get(from: "api/sync/messages")
            try await mirror.upsertMessages(inbox.page, syncedAt: now)
            return try await mirror.messagesOverview(now: now)
        },
        markRead: { id, now in
            @Dependency(\.database) var database
            try await MirrorStore(writer: database).markMessageRead(id: id, now: now)
        },
        markAllRead: { now in
            @Dependency(\.database) var database
            try await MirrorStore(writer: database).markAllMessagesRead(now: now)
        },
        setStarred: { id, starred in
            @Dependency(\.database) var database
            try await MirrorStore(writer: database).setMessageStarred(id: id, starred: starred)
        }
    )
}

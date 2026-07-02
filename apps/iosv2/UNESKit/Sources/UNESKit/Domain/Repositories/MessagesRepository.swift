import ComposableArchitecture
import Foundation

/// The Mensagens inbox, stale-while-revalidate like Home: `cached` is the
/// fast local read from the mirror, `refresh` pulls the first inbox page and
/// rewrites the mirror. Read/star mutations are local-only — the backend has
/// no ack endpoint yet, so the overlay lives beside the mirrored rows.
@DependencyClient
struct MessagesRepository: Sendable {
    /// The inbox as mirrored on disk; nil until the first successful sync.
    var cached: @Sendable (_ now: Date) async throws -> MessagesOverview?
    var refresh: @Sendable (_ now: Date) async throws -> MessagesOverview
    var markRead: @Sendable (_ id: String, _ now: Date) async throws -> Void
    var markAllRead: @Sendable (_ now: Date) async throws -> Void
    var setStarred: @Sendable (_ id: String, _ starred: Bool) async throws -> Void
}

extension MessagesRepository: TestDependencyKey {
    static let testValue = MessagesRepository()

    static let previewValue = MessagesRepository(
        cached: { now in .preview(now: now) },
        refresh: { now in .preview(now: now) },
        markRead: { _, _ in },
        markAllRead: { _ in },
        setStarred: { _, _ in }
    )
}

extension DependencyValues {
    var messagesRepository: MessagesRepository {
        get { self[MessagesRepository.self] }
        set { self[MessagesRepository.self] = newValue }
    }
}

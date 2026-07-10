import ComposableArchitecture
import Foundation

/// The Mensagens inbox. The mirror is the source of truth: `observe` streams
/// the local inbox on subscription and after every write that changes it,
/// `refresh` pulls the first inbox page into the mirror (landing through
/// `observe`), and `cached` is a one-shot local read for the offline fallback.
/// Read and star mutations write the local overlay first, then best-effort
/// ack the backend so other devices see the state.
@DependencyClient
struct MessagesRepository: Sendable {
    /// The inbox as mirrored on disk; nil until the first successful sync.
    var cached: @Sendable (_ now: Date) async throws -> MessagesOverview?
    var refresh: @Sendable (_ now: Date) async throws -> Void
    var observe: @Sendable () -> AsyncStream<MessagesOverview> = { .finished }
    var markRead: @Sendable (_ id: String, _ now: Date) async throws -> Void
    var markAllRead: @Sendable (_ now: Date) async throws -> Void
    var setStarred: @Sendable (_ id: String, _ starred: Bool) async throws -> Void
}

extension MessagesRepository: TestDependencyKey {
    static let testValue = MessagesRepository()

    static let previewValue = MessagesRepository(
        cached: { now in .preview(now: now) },
        refresh: { _ in },
        observe: {
            AsyncStream { continuation in
                continuation.yield(.preview(now: .now))
            }
        },
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

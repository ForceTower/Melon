import ComposableArchitecture
import Foundation

/// The student's own calendar entries. Purely local: there is no endpoint
/// behind any of this, so `observe` streams the device's own writes back and
/// every mutation lands in the mirror synchronously.
@DependencyClient
struct PersonalEventsRepository: Sendable {
    var observe: @Sendable () -> AsyncStream<[PersonalEvent]> = { .finished }
    /// Inserts or replaces one entry and re-aligns its local notification.
    var save: @Sendable (_ event: PersonalEvent) async throws -> Void
    var delete: @Sendable (_ id: String) async throws -> Void
    /// Re-aligns the pending notifications with what's stored — for the days
    /// nothing was written but the clock moved past a scheduled reminder.
    var reconcileReminders: @Sendable () async -> Void
}

extension PersonalEventsRepository: TestDependencyKey {
    static let testValue = PersonalEventsRepository()

    static let previewValue = PersonalEventsRepository(
        observe: {
            AsyncStream { continuation in
                continuation.yield(.preview(now: .now))
            }
        },
        save: { _ in },
        delete: { _ in },
        reconcileReminders: {}
    )
}

extension DependencyValues {
    var personalEventsRepository: PersonalEventsRepository {
        get { self[PersonalEventsRepository.self] }
        set { self[PersonalEventsRepository.self] = newValue }
    }
}

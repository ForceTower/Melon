import ComposableArchitecture
import Foundation

/// The Horário screen surface. The mirror is the source of truth: `observe`
/// streams the week on subscription and after every write that changes it,
/// `refresh` resolves the active semester upstream and rewrites the mirror
/// (landing through `observe`), and `cached` is a one-shot local read for
/// recomputing the week's dates while offline.
@DependencyClient
struct ScheduleRepository: Sendable {
    /// The week as mirrored on disk; nil until the first successful refresh.
    var cached: @Sendable (_ now: Date) async throws -> ScheduleOverview?
    var refresh: @Sendable (_ now: Date) async throws -> Void
    var observe: @Sendable () -> AsyncStream<ScheduleOverview> = { .finished }
}

extension ScheduleRepository: TestDependencyKey {
    static let testValue = ScheduleRepository()

    static let previewValue = ScheduleRepository(
        cached: { now in .preview(now: now) },
        refresh: { _ in },
        observe: {
            AsyncStream { continuation in
                continuation.yield(.preview(now: .now))
            }
        }
    )
}

extension DependencyValues {
    var scheduleRepository: ScheduleRepository {
        get { self[ScheduleRepository.self] }
        set { self[ScheduleRepository.self] = newValue }
    }
}

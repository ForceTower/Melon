import ComposableArchitecture
import Foundation

/// The Horário screen surface, stale-while-revalidate like Home: `cached` is
/// the fast local read from the mirror, `refresh` resolves the active
/// semester upstream, rewrites the mirror, and returns the fresh week.
@DependencyClient
struct ScheduleRepository: Sendable {
    /// The week as mirrored on disk; nil until the first successful refresh.
    var cached: @Sendable (_ now: Date) async throws -> ScheduleOverview?
    var refresh: @Sendable (_ now: Date) async throws -> ScheduleOverview
}

extension ScheduleRepository: TestDependencyKey {
    static let testValue = ScheduleRepository()

    static let previewValue = ScheduleRepository(
        cached: { now in .preview(now: now) },
        refresh: { now in .preview(now: now) }
    )
}

extension DependencyValues {
    var scheduleRepository: ScheduleRepository {
        get { self[ScheduleRepository.self] }
        set { self[ScheduleRepository.self] = newValue }
    }
}

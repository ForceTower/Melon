import ComposableArchitecture
import Foundation

/// The Turmas screen surface. The mirror is the source of truth: `observe`
/// streams the local snapshot on subscription and after every write that
/// changes it, `refresh` and `downloadSemester` rewrite the mirror from
/// upstream (landing through `observe`), and `cached` is a one-shot local
/// read for recomputing time-derived pieces while offline.
@DependencyClient
struct DisciplinesRepository: Sendable {
    /// The overview as mirrored on disk; nil until the first successful
    /// refresh lands.
    var cached: @Sendable (_ now: Date) async throws -> DisciplinesOverview?
    var refresh: @Sendable (_ now: Date) async throws -> Void
    var downloadSemester: @Sendable (_ semesterId: String, _ now: Date) async throws -> Void
    var observe: @Sendable () -> AsyncStream<DisciplinesOverview> = { .finished }
    /// The detail feed for one mirrored discipline — a pure local read.
    var observeDetail: @Sendable (_ semesterId: String, _ disciplineId: String) -> AsyncStream<DisciplineDetail> = { _, _ in .finished }
}

extension DisciplinesRepository: TestDependencyKey {
    static let testValue = DisciplinesRepository()

    static let previewValue = DisciplinesRepository(
        cached: { now in .preview(now: now) },
        refresh: { _ in },
        downloadSemester: { _, _ in },
        observe: {
            AsyncStream { continuation in
                continuation.yield(.preview(now: .now))
            }
        },
        observeDetail: { _, _ in
            AsyncStream { continuation in
                continuation.yield(.preview(now: .now))
            }
        }
    )
}

extension DependencyValues {
    var disciplinesRepository: DisciplinesRepository {
        get { self[DisciplinesRepository.self] }
        set { self[DisciplinesRepository.self] = newValue }
    }
}

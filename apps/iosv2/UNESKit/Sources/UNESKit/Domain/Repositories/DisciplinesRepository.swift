import ComposableArchitecture
import Foundation

/// The Turmas screen surface, stale-while-revalidate like Home: `cached` is
/// the fast local read from the mirror, `refresh` re-pulls the semester list
/// plus the active semester's payload, and `downloadSemester` pulls one past
/// semester on demand. Every call resolves to the full overview so the
/// screen always renders from a single, mirror-backed source.
@DependencyClient
struct DisciplinesRepository: Sendable {
    var cached: @Sendable (_ now: Date) async throws -> DisciplinesOverview?
    var refresh: @Sendable (_ now: Date) async throws -> DisciplinesOverview
    var downloadSemester: @Sendable (_ semesterId: String, _ now: Date) async throws -> DisciplinesOverview
}

extension DisciplinesRepository: TestDependencyKey {
    static let testValue = DisciplinesRepository()

    static let previewValue = DisciplinesRepository(
        cached: { now in .preview(now: now) },
        refresh: { now in .preview(now: now) },
        downloadSemester: { _, now in .preview(now: now) }
    )
}

extension DependencyValues {
    var disciplinesRepository: DisciplinesRepository {
        get { self[DisciplinesRepository.self] }
        set { self[DisciplinesRepository.self] = newValue }
    }
}

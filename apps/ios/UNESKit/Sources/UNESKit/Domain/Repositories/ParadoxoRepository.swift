import ComposableArchitecture
import Foundation

/// University-wide grade statistics. Everything is fetched live per screen —
/// the data changes once a semester, so a session-scoped fetch beats a
/// mirror table. `index` feeds the local search.
@DependencyClient
struct ParadoxoRepository: Sendable {
    var overview: @Sendable () async throws -> ParadoxoOverview
    var index: @Sendable () async throws -> [ParadoxoIndexEntry]
    var discipline: @Sendable (_ id: String) async throws -> ParadoxoDisciplineDetails
    var teacher: @Sendable (_ id: String) async throws -> ParadoxoTeacherDetails
}

extension ParadoxoRepository: TestDependencyKey {
    static let testValue = ParadoxoRepository()

    static let previewValue = ParadoxoRepository(
        overview: { .preview() },
        index: { ParadoxoIndexEntry.preview() },
        discipline: { _ in .preview() },
        teacher: { _ in .preview() }
    )
}

extension DependencyValues {
    var paradoxoRepository: ParadoxoRepository {
        get { self[ParadoxoRepository.self] }
        set { self[ParadoxoRepository.self] = newValue }
    }
}

import ComposableArchitecture

/// The one live slice of the Retrospectiva: turma percentiles need the other
/// students' rows, so they come from the API; every other card stat is
/// computed from the mirror.
@DependencyClient
struct RetrospectiveRepository: Sendable {
    var percentiles: @Sendable (_ semesterCode: String) async throws -> [RetrospectivePercentile]
}

extension RetrospectiveRepository: TestDependencyKey {
    static let testValue = RetrospectiveRepository()

    static let previewValue = RetrospectiveRepository(
        percentiles: { _ in [] }
    )
}

extension DependencyValues {
    var retrospectiveRepository: RetrospectiveRepository {
        get { self[RetrospectiveRepository.self] }
        set { self[RetrospectiveRepository.self] = newValue }
    }
}

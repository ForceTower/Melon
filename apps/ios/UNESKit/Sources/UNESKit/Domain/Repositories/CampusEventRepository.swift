import ComposableArchitecture
import Foundation

/// Featured course events (integration weeks and other extraordinary
/// happenings). The local mirror is the source of truth: `observe` streams
/// the mirrored event on subscription and after every write that changes it,
/// `refresh` rewrites the mirror from upstream (landing through `observe`),
/// and `cached` is a one-shot local read. nil means nothing is featured.
@DependencyClient
struct CampusEventRepository: Sendable {
    var cached: @Sendable () async throws -> CampusEvent?
    var refresh: @Sendable () async throws -> Void
    var observe: @Sendable () -> AsyncStream<CampusEvent?> = { .finished }
}

extension CampusEventRepository: TestDependencyKey {
    static let testValue = CampusEventRepository()

    static let previewValue = CampusEventRepository(
        cached: { .preview() },
        refresh: {},
        observe: {
            AsyncStream { continuation in
                continuation.yield(.preview())
            }
        }
    )
}

extension DependencyValues {
    var campusEventRepository: CampusEventRepository {
        get { self[CampusEventRepository.self] }
        set { self[CampusEventRepository.self] = newValue }
    }
}

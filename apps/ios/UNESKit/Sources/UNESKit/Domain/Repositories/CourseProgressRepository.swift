import ComposableArchitecture
import Foundation

/// Course progress ("quanto falta pra formar"). The local mirror is the
/// source of truth: `observe` streams the mirrored payload on subscription
/// and after every write that changes it, `refresh` rewrites the mirror from
/// `api/curriculum` (landing through `observe`), and `cached` is a one-shot
/// local read. nil means nothing was fetched yet.
@DependencyClient
struct CourseProgressRepository: Sendable {
    var cached: @Sendable () async throws -> CourseProgress?
    var refresh: @Sendable () async throws -> Void
    var observe: @Sendable () -> AsyncStream<CourseProgress?> = { .finished }
}

extension CourseProgressRepository: TestDependencyKey {
    static let testValue = CourseProgressRepository()

    static let previewValue = CourseProgressRepository(
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
    var courseProgressRepository: CourseProgressRepository {
        get { self[CourseProgressRepository.self] }
        set { self[CourseProgressRepository.self] = newValue }
    }
}

import ComposableArchitecture
import Foundation

/// The Home ("Hoje") screen surface. The mirror is the source of truth:
/// `observe` streams the local snapshot on subscription and after every write
/// that changes it, `refresh` rewrites the mirror from upstream (landing
/// through `observe`), and `cached` is a one-shot local read for recomputing
/// time-derived pieces while offline.
@DependencyClient
struct HomeRepository: Sendable {
    /// The overview as mirrored on disk; nil until the first successful
    /// refresh lands.
    var cached: @Sendable (_ now: Date) async throws -> CachedHomeOverview?
    var refresh: @Sendable (_ now: Date) async throws -> Void
    var observe: @Sendable () -> AsyncStream<CachedHomeOverview> = { .finished }
}

extension HomeRepository: TestDependencyKey {
    static let testValue = HomeRepository()

    static let previewValue = HomeRepository(
        cached: { now in CachedHomeOverview(overview: .preview(now: now), syncedAt: now) },
        refresh: { _ in },
        observe: {
            AsyncStream { continuation in
                continuation.yield(CachedHomeOverview(overview: .preview(now: .now), syncedAt: .now))
            }
        }
    )
}

extension DependencyValues {
    var homeRepository: HomeRepository {
        get { self[HomeRepository.self] }
        set { self[HomeRepository.self] = newValue }
    }
}

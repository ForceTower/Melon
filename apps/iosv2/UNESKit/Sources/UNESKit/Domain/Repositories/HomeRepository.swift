import ComposableArchitecture
import Foundation

/// The Home ("Hoje") screen surface, stale-while-revalidate: `cached` is the
/// fast local read from the mirror, `refresh` resolves the active semester
/// upstream, rewrites the mirror, and returns the fresh snapshot.
@DependencyClient
struct HomeRepository: Sendable {
    /// The overview as mirrored on disk; nil until the first successful
    /// refresh lands.
    var cached: @Sendable (_ now: Date) async throws -> CachedHomeOverview?
    var refresh: @Sendable (_ now: Date) async throws -> HomeOverview
}

extension HomeRepository: TestDependencyKey {
    static let testValue = HomeRepository()

    static let previewValue = HomeRepository(
        cached: { now in CachedHomeOverview(overview: .preview(now: now), syncedAt: now) },
        refresh: { now in .preview(now: now) }
    )
}

extension DependencyValues {
    var homeRepository: HomeRepository {
        get { self[HomeRepository.self] }
        set { self[HomeRepository.self] = newValue }
    }
}

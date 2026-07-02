import ComposableArchitecture
import Foundation

/// The Eu screen's local surface. The mirror is the source of truth:
/// `observe` streams the snapshot on subscription and after every write that
/// changes it (there is no network refresh here — the other tabs keep the
/// mirror fresh). `localData` and `wipeLocalData` back the logout flow.
@DependencyClient
struct MeRepository: Sendable {
    var observe: @Sendable () -> AsyncStream<CachedMeOverview> = { .finished }
    var localData: @Sendable () async throws -> LocalDataSummary
    var wipeLocalData: @Sendable () async throws -> Void
}

extension MeRepository: TestDependencyKey {
    static let testValue = MeRepository()

    static let previewValue = MeRepository(
        observe: {
            AsyncStream { continuation in
                continuation.yield(CachedMeOverview(overview: .preview, syncedAt: .now))
            }
        },
        localData: { LocalDataSummary(semesters: 7, messages: 142) },
        wipeLocalData: {}
    )
}

extension DependencyValues {
    var meRepository: MeRepository {
        get { self[MeRepository.self] }
        set { self[MeRepository.self] = newValue }
    }
}

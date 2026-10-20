import ComposableArchitecture
import Foundation

/// The student's saved Meu trajeto. The server is the source of truth: `save`
/// and `remove` only touch the local mirror after the server accepted them,
/// with what it answered. `observe` streams the mirror on subscription and
/// after every write, so every screen sees the same plan.
@DependencyClient
struct CommutePlanRepository: Sendable {
    var observe: @Sendable () -> AsyncStream<CommutePlan> = { .finished }
    /// Pulls the server's plan into the mirror (another device may have changed it).
    var refresh: @Sendable () async throws -> Void
    var save: @Sendable (_ plan: CommutePlan) async throws -> CommutePlan
    var remove: @Sendable () async throws -> Void
}

extension CommutePlanRepository: TestDependencyKey {
    static let testValue = CommutePlanRepository()
    static let previewValue = CommutePlanRepository.preview(TransitFixtures.plan)

    static func preview(_ plan: CommutePlan) -> CommutePlanRepository {
        CommutePlanRepository(
            observe: { AsyncStream { $0.yield(plan) } },
            refresh: {},
            save: { $0 },
            remove: {}
        )
    }
}

extension DependencyValues {
    var commutePlanRepository: CommutePlanRepository {
        get { self[CommutePlanRepository.self] }
        set { self[CommutePlanRepository.self] = newValue }
    }
}

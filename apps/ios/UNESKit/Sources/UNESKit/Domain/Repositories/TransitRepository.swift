import ComposableArchitecture
import Foundation

/// The UEFS bus lines as the backend's collector sees them (`api/transit/*`,
/// signed-in only). The network rarely changes and is cached; arrivals and
/// follow are meant to be polled while their screen is visible.
@DependencyClient
struct TransitRepository: Sendable {
    var network: @Sendable () async throws -> TransitNetwork
    var arrivals: @Sendable (
        _ stopId: String,
        _ direction: TransitDirection,
        _ lineNumbers: [String]
    ) async throws -> TransitStopBoard
    /// Follows one departure by its arrival id, which survives it going from
    /// history to a bus on its way; `vehicleId` is the fallback.
    var follow: @Sendable (
        _ lineNumber: String,
        _ direction: TransitDirection,
        _ stopId: String,
        _ arrivalId: String,
        _ vehicleId: String?
    ) async throws -> TransitFollow
}

extension TransitRepository: TestDependencyKey {
    static let testValue = TransitRepository()
    static let previewValue = TransitRepository.mock(mode: .normal)

    static func mock(mode: TransitFixtures.BoardMode) -> TransitRepository {
        TransitRepository(
            network: { TransitFixtures.network },
            arrivals: { _, direction, _ in
                TransitFixtures.board(now: Date(), direction: direction, mode: mode)
            },
            follow: { _, _, _, _, _ in TransitFixtures.follow(now: Date()) }
        )
    }
}

extension DependencyValues {
    var transitRepository: TransitRepository {
        get { self[TransitRepository.self] }
        set { self[TransitRepository.self] = newValue }
    }
}

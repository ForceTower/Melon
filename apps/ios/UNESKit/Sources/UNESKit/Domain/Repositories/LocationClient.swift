import ComposableArchitecture
import Foundation

enum LocationAuthorization: Equatable, Sendable {
    case notDetermined
    case denied
    case authorized
}

enum LocationError: Error, Equatable {
    case denied
    case unavailable
}

/// While-in-use location, only ever asked for from a button the student taps.
/// Nothing here is stored or sent to the backend.
@DependencyClient
struct LocationClient: Sendable {
    var authorization: @Sendable () async -> LocationAuthorization = { .notDetermined }
    /// Prompts when undetermined, then waits for one fix.
    var currentLocation: @Sendable () async throws -> GeoPoint
}

extension LocationClient: TestDependencyKey {
    static let testValue = LocationClient()
    static let previewValue = LocationClient(
        authorization: { .authorized },
        currentLocation: { GeoPoint(latitude: -12.2372, longitude: -38.9590) }
    )
}

extension DependencyValues {
    var locationClient: LocationClient {
        get { self[LocationClient.self] }
        set { self[LocationClient.self] = newValue }
    }
}

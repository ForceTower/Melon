import ComposableArchitecture
import CoreLocation

private let log = Log.scoped("LocationClient")

extension LocationClient: DependencyKey {
    static let liveValue = LocationClient(
        authorization: {
            let status = await MainActor.run { CLLocationManager().authorizationStatus }
            return switch status {
            case .notDetermined: .notDetermined
            case .denied, .restricted: .denied
            default: .authorized
            }
        },
        currentLocation: {
            try await withThrowingTaskGroup(of: GeoPoint.self) { group in
                group.addTask { try await firstFix() }
                group.addTask {
                    try await Task.sleep(for: .seconds(15))
                    throw LocationError.unavailable
                }
                defer { group.cancelAll() }
                guard let point = try await group.next() else { throw LocationError.unavailable }
                return point
            }
        }
    )
}

/// The service session is what raises the when-in-use prompt; it lives only
/// for as long as one fix takes.
private func firstFix() async throws -> GeoPoint {
    let session = CLServiceSession(authorization: .whenInUse)
    defer { session.invalidate() }
    for try await update in CLLocationUpdate.liveUpdates() {
        if let location = update.location {
            return GeoPoint(latitude: location.coordinate.latitude, longitude: location.coordinate.longitude)
        }
        if update.authorizationDenied || update.authorizationDeniedGlobally || update.authorizationRestricted {
            log.info("location denied")
            throw LocationError.denied
        }
    }
    throw LocationError.unavailable
}

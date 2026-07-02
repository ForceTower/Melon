import ComposableArchitecture
import LocalAuthentication

extension LocalAuthClient: DependencyKey {
    /// Face ID with the device passcode as fallback.
    static let liveValue = LocalAuthClient(
        authenticate: { reason in
            try await LAContext().evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason)
        }
    )
}

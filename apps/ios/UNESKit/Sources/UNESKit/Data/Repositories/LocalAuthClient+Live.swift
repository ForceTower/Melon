import ComposableArchitecture
import LocalAuthentication

private let log = Log.scoped("LocalAuthClient")

extension LocalAuthClient: DependencyKey {
    /// Face ID with the device passcode as fallback.
    static let liveValue = LocalAuthClient(
        authenticate: { reason in
            log.debug("authenticate start")
            do {
                let granted = try await LAContext().evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason)
                log.info("authenticate ok granted=\(granted)")
                return granted
            } catch {
                log.warn("authenticate failed", error: error)
                throw error
            }
        }
    )
}

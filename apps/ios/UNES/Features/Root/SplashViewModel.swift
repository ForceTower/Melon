import Observation
@preconcurrency import Umbrella

@Observable
final class SplashViewModel {
    private let sessionStore: SessionSessionStore?
    private let log = Log.scoped("SplashViewModel")

    init(sessionStore: SessionSessionStore?) {
        self.sessionStore = sessionStore
    }

    func hasStoredSession() async -> Bool {
        guard let sessionStore else { return false }

        do {
            let token = try await sessionStore.getAccessToken()
            let present = token != nil
            log.info("splash session check hasSession=\(present)")
            return present
        } catch is CancellationError {
            return false
        } catch {
            log.warn("splash session check failed", error: error)
            return false
        }
    }
}

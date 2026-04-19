import Observation
@preconcurrency import Umbrella

@Observable
final class SplashViewModel {
    private let sessionStore: SessionSessionStore?

    init(sessionStore: SessionSessionStore?) {
        self.sessionStore = sessionStore
    }

    func hasStoredSession() async -> Bool {
        guard let sessionStore else { return false }

        do {
            let token = try await sessionStore.getAccessToken()
            return token != nil
        } catch is CancellationError {
            return false
        } catch {
            return false
        }
    }
}

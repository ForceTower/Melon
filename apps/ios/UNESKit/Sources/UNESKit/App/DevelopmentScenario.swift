#if DEBUG
import ComposableArchitecture
import Foundation

public enum DevelopmentScenario {
    static let now = Date(timeIntervalSince1970: 1_790_946_000)

    public static var isEnabled: Bool {
        guard let id = ProcessInfo.processInfo.environment["MELON_SCENARIO"] else { return false }
        precondition(id == "auth.login", "Unsupported iOS launch scenario: \(id)")
        return true
    }

    @MainActor
    static func makeStore() -> StoreOf<RootFeature> {
        NSTimeZone.default = TimeZone(identifier: "America/Bahia")!
        return withDependencies {
            $0.context = .preview
            $0.defaultAppStorage = .inMemory
            $0.database = try! inMemoryDatabase()
            $0.sessionStore = .inMemory()
            $0.date = .constant(now)
            $0.continuousClock = ContinuousClock()
            $0.uuid = .incrementing
            $0.apiClient = .live(baseURL: URL(string: "http://127.0.0.1:8788")!)
            $0.authRepository = .liveValue
            $0.syncRepository = .liveValue
            $0.profileRepository = .liveValue
            $0.homeRepository = .liveValue
            $0.messagesRepository = .liveValue
            $0.credentialStatusRepository = .liveValue
        } operation: {
            var onboarding = OnboardingFeature.State(splash: false)
            onboarding.path.append(.login(LoginFeature.State()))
            return Store(initialState: RootFeature.State.onboarding(onboarding)) {
                RootFeature()
            }
        }
    }
}
#endif

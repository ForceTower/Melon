import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

struct PushRegistrarTests {
    @Test
    func signingBackInWithoutARelaunchRegistersTheDeviceAgain() async throws {
        let suite = "PushRegistrarTests.\(UUID().uuidString)"
        defer { UserDefaults().removePersistentDomain(forName: suite) }
        let registrar = PushRegistrar(defaults: try #require(UserDefaults(suiteName: suite)))
        let sessionStore = SessionStore.inMemory(initial: .preview)
        let requests = LockIsolated<[APIRequest]>([])

        await withDependencies {
            $0.sessionStore = sessionStore
            $0.apiClient.send = { request in
                requests.withValue { $0.append(request) }
                return Data()
            }
        } operation: {
            await registrar.fcmTokenReceived("fcm-token")
            await registrar.unregister()
            try? sessionStore.clear()
            // Firebase only hands the token over at launch, so nothing else arrives before the next login.
            try? sessionStore.save(.preview)
            await registrar.reconcile()
        }

        #expect(requests.value.map { "\($0.method) \($0.path)" } == [
            "POST api/notifications/token",
            "DELETE api/notifications/token",
            "POST api/notifications/token",
        ])
        let relogin = try JSONDecoder().decode(TokenBody.self, from: try #require(requests.value.last?.body))
        #expect(relogin.token == "fcm-token")
        #expect(await registrar.currentToken() == "fcm-token")
    }
}

private struct TokenBody: Decodable {
    let token: String
}

import ComposableArchitecture
import Foundation
import UserNotifications
#if canImport(UIKit)
import UIKit
#endif

extension PushClient: DependencyKey {
    static let liveValue = PushClient(
        requestAuthorization: {
            _ = try? await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .badge, .sound])
        },
        tokenReceived: { token in
            UserDefaults.standard.set(token, forKey: tokenKey)
            @Dependency(\.sessionStore) var sessionStore
            guard sessionStore.current() != nil else { return }
            await register(token)
        },
        registerStoredToken: {
            guard let token = UserDefaults.standard.string(forKey: tokenKey) else { return }
            await register(token)
        }
    )

    private static let tokenKey = "messaging_notification_token"

    /// Best-effort: FCM re-delivers the token on every launch, so a failed
    /// registration heals on the next one.
    private static func register(_ token: String) async {
        @Dependency(\.apiClient) var apiClient
        #if canImport(UIKit)
        let deviceName: String? = await UIDevice.current.name
        #else
        let deviceName: String? = nil
        #endif
        let body = RegisterTokenBody(
            token: token,
            platform: "ios",
            deviceName: deviceName,
            appVersion: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String,
            locale: Locale.current.identifier
        )
        try? await apiClient.post(to: "api/notifications/token", body: body)
    }
}

private struct RegisterTokenBody: Encodable {
    let token: String
    let platform: String
    let deviceName: String?
    let appVersion: String?
    let locale: String
}

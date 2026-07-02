import ComposableArchitecture
import Foundation
import UserNotifications
#if canImport(UIKit)
import UIKit
#endif

private let log = Log.scoped("PushClient")

extension PushClient: DependencyKey {
    static let liveValue = PushClient(
        requestAuthorization: {
            log.debug("requestAuthorization start")
            do {
                let granted = try await UNUserNotificationCenter.current()
                    .requestAuthorization(options: [.alert, .badge, .sound])
                log.info("requestAuthorization ok granted=\(granted)")
            } catch {
                log.warn("requestAuthorization failed", error: error)
            }
        },
        tokenReceived: { token in
            UserDefaults.standard.set(token, forKey: tokenKey)
            log.debug("tokenReceived stored length=\(token.count)")
            @Dependency(\.sessionStore) var sessionStore
            guard sessionStore.current() != nil else {
                log.debug("tokenReceived deferred: no session")
                return
            }
            await register(token)
        },
        registerStoredToken: {
            guard let token = UserDefaults.standard.string(forKey: tokenKey) else {
                log.debug("registerStoredToken skipped: no stored token")
                return
            }
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
        log.debug("register start tokenLength=\(token.count)")
        do {
            try await apiClient.post(to: "api/notifications/token", body: body)
            log.info("register ok tokenLength=\(token.count)")
        } catch {
            switch error {
            case APIError.server(401, _):
                log.warn("register unauthorized")
            case let APIError.server(status, message):
                log.warn("register server \(status) message=\(message ?? "<none>")")
            case is URLError:
                log.warn("register transport failure", error: error)
            default:
                log.error("register failed", error: error)
            }
        }
    }
}

private struct RegisterTokenBody: Encodable {
    let token: String
    let platform: String
    let deviceName: String?
    let appVersion: String?
    let locale: String
}

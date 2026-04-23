import UIKit
@preconcurrency import Umbrella
import FirebaseCore
import FirebaseCrashlytics
import FirebaseMessaging
import FirebaseRemoteConfig

final class AppDelegate: NSObject, UIApplicationDelegate {
    lazy var graph: UmbrellaGraph = {
        let config = UmbrellaConfig(baseUrl: "https://melon.forcetower.dev")
        return UmbrellaGraph(config: config)
    }()

    private var isPreview: Bool {
        ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PLAYGROUNDS"] == "1"
    }

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        _ = graph

        guard !isPreview else { return true }
        FirebaseApp.configure()
#if DEBUG
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
#endif
        
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        application.registerForRemoteNotifications()
        
        RemoteConfig.remoteConfig().fetchAndActivate()
        
        return true
    }
    
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        // TODO received a data notification
//        Task {
//            let sync: ServerSyncDataUseCase = AppDIContainer.shared.resolve()
//            await sync.execute()
//            completionHandler(.newData)
//        }
    }

}

extension AppDelegate: UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }
    
    nonisolated func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        UserDefaults.standard.set(fcmToken, forKey: "messaging_notification_token")

        Task { @MainActor in
            guard (try? await graph.sessionStore.getAccessToken()) != nil else { return }

            let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
            let deviceName = UIDevice.current.name
            let locale = Locale.current.identifier

            _ = try? await graph.registerNotificationTokenUseCase.invoke(
                token: fcmToken,
                platform: "ios",
                deviceName: deviceName,
                appVersion: appVersion,
                locale: locale
            )
        }
    }
    
    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.list, .sound, .banner])
    }
}

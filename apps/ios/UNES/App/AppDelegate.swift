import UIKit
@preconcurrency import Umbrella
import FirebaseCore
import FirebaseCrashlytics
import FirebaseMessaging
import FirebaseRemoteConfig

final class AppDelegate: NSObject, UIApplicationDelegate {
    lazy var graph: UmbrellaGraph = {
        let config = UmbrellaConfig(
            baseUrl: "https://melon.forcetower.dev",
            appContext: ApplicationContext(),
            logging: LoggingLoggingConfig(
                serviceName: "melon-ios",
                serviceVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String,
                deploymentEnvironment: AppDelegate.environmentName,
                apiBaseUrl: nil,
                minLocalSeverity: Kermit_coreSeverity.verbose,
                minRemoteSeverity: Kermit_coreSeverity.info,
                minCrashBreadcrumbSeverity: Kermit_coreSeverity.info,
                minCrashReportSeverity: Kermit_coreSeverity.warn,
                enableRemote: true,
                enableCrashReporting: true
            ),
            crashReporter: FirebaseCrashReporter()
        )
        return UmbrellaGraph(config: config)
    }()

    lazy var logger: AppLogger = KermitAppLogger(logger: graph.logger)
    private let log = Log.scoped("AppDelegate")

    private static var environmentName: String {
#if DEBUG
        return "debug"
#else
        return "release"
#endif
    }

    private var isPreview: Bool {
        ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PLAYGROUNDS"] == "1"
    }

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Firebase must be configured before the logger's crash-reporter
        // breadcrumbs start flowing — otherwise Crashlytics.crashlytics() is
        // accessed pre-init on the very first log line below.
        if !isPreview {
            FirebaseApp.configure()
#if DEBUG
            Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
#endif
        }

        _ = graph
        Log.bootstrap(logger)
        log.info("app launching env=\(AppDelegate.environmentName) preview=\(isPreview)")

        guard !isPreview else { return true }

        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        application.registerForRemoteNotifications()

        RemoteConfig.remoteConfig().fetchAndActivate()

        log.info("app launch finished")
        return true
    }

    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        log.info("remote notification received keys=\(userInfo.keys.map { String(describing: $0) }.joined(separator: ","))")
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
        log.info("APNS token registered bytes=\(deviceToken.count)")
        Messaging.messaging().apnsToken = deviceToken
    }

    nonisolated func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        let nlog = Log.scoped("AppDelegate")
        guard let fcmToken else {
            nlog.warn("FCM token was nil")
            return
        }
        nlog.info("FCM token received length=\(fcmToken.count)")
        UserDefaults.standard.set(fcmToken, forKey: "messaging_notification_token")

        Task { @MainActor in
            guard (try? await graph.sessionStore.getAccessToken()) != nil else {
                self.log.info("FCM token skip register — no session yet")
                return
            }

            let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
            let deviceName = UIDevice.current.name
            let locale = Locale.current.identifier

            do {
                _ = try await graph.registerNotificationTokenUseCase.invoke(
                    token: fcmToken,
                    platform: "ios",
                    deviceName: deviceName,
                    appVersion: appVersion,
                    locale: locale
                )
                self.log.info("FCM token registered with backend")
            } catch {
                self.log.warn("FCM token backend register failed", error: error)
            }
        }
    }
    
    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.list, .sound, .banner])
    }
}

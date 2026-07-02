import FirebaseCore
import FirebaseCrashlytics
import FirebaseMessaging
import FirebaseRemoteConfig
import UIKit
import UNESKit
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate {
    /// Xcode previews launch the app delegate too — configuring Firebase
    /// there bugs Xcode out, so all of it is skipped.
    private var isPreview: Bool {
        ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PLAYGROUNDS"] == "1"
    }

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        guard !isPreview else { return true }

        FirebaseApp.configure()
        #if DEBUG
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #endif

        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        application.registerForRemoteNotifications()

        configureRemoteConfig()
        return true
    }

    /// Launch fetch for the baseline, then the real-time stream so console
    /// publishes land while the app is running.
    private func configureRemoteConfig() {
        let remoteConfig = RemoteConfig.remoteConfig()
        #if DEBUG
        // No 12h fetch cache while developing — flag flips apply right away.
        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = 0
        remoteConfig.configSettings = settings
        #endif

        Task {
            _ = try? await remoteConfig.fetchAndActivate()
            Self.publishFlags()
        }

        _ = remoteConfig.addOnConfigUpdateListener { update, error in
            guard update != nil, error == nil else { return }
            RemoteConfig.remoteConfig().activate { _, _ in
                Self.publishFlags()
            }
        }
    }

    /// Off the main actor — the update stream calls back on a Firebase queue.
    private nonisolated static func publishFlags() {
        FeatureFlags.update(
            enrollmentEnabled: RemoteConfig.remoteConfig()
                .configValue(forKey: "enable_enrollment").boolValue
        )
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }
}

extension AppDelegate: MessagingDelegate, UNUserNotificationCenterDelegate {
    nonisolated func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        Task { await PushTokens.received(fcmToken) }
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.list, .sound, .banner])
    }
}

import FirebaseCore
import FirebaseCrashlytics
import FirebaseMessaging
import FirebaseRemoteConfig
import UIKit
import UNESKit
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate {
    private let log = Log.scoped("AppDelegate")

    /// Xcode previews launch the app delegate too — configuring Firebase
    /// there bugs Xcode out, so all of it is skipped.
    private var isPreview: Bool {
        ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PLAYGROUNDS"] == "1"
    }

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        log.info("app launching preview=\(isPreview)")
        guard !isPreview else { return true }

        FirebaseApp.configure()
        log.info("firebase configured")
        #if DEBUG
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #endif

        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        application.registerForRemoteNotifications()

        configureRemoteConfig()
        log.info("app launch finished")
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

        Task { [log] in
            _ = try? await remoteConfig.fetchAndActivate()
            log.debug("remote config baseline fetch+activate completed")
            Self.publishFlags()
        }

        _ = remoteConfig.addOnConfigUpdateListener { [log] update, error in
            if let error {
                log.warn("remote config update stream error", error: error)
            }
            guard update != nil, error == nil else { return }
            log.debug("remote config update received -> republishing flags")
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
        log.info("APNS token registered bytes=\(deviceToken.count)")
        Messaging.messaging().apnsToken = deviceToken
    }
}

extension AppDelegate: MessagingDelegate, UNUserNotificationCenterDelegate {
    nonisolated func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        let log = Log.scoped("AppDelegate")
        guard let fcmToken else {
            log.warn("FCM token was nil")
            return
        }
        log.info("FCM token received length=\(fcmToken.count)")
        Task { await PushTokens.received(fcmToken) }
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // FCM data payloads ride along as string entries in userInfo; the
        // non-string values ("aps" & friends) are system metadata.
        let data = notification.request.content.userInfo
            .reduce(into: [String: String]()) { payload, entry in
                guard let key = entry.key as? String, let value = entry.value as? String else { return }
                payload[key] = value
            }
        if !data.isEmpty {
            Task { await PushEvents.received(data) }
        }
        completionHandler([.list, .sound, .banner])
    }
}

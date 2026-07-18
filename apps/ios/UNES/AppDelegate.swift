import FirebaseCore
import FirebaseCrashlytics
import FirebaseMessaging
import FirebaseRemoteConfig
import PostHog
import UIKit
import UNESKit
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate {
    private let log = Log.scoped("AppDelegate")

    /// Xcode previews launch the app delegate too — configuring Firebase
    /// there bugs Xcode out, so all of it is skipped. Xcode 26 runs previews
    /// and playgrounds on the same engine; older versions only set the
    /// PREVIEWS variable, so check both.
    private var isPreview: Bool {
        let environment = ProcessInfo.processInfo.environment
        return environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
            || environment["XCODE_RUNNING_FOR_PLAYGROUNDS"] == "1"
    }

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        log.info("app launching preview=\(isPreview)")
        guard !isPreview else { return true }

        // Before any scene task reads dependencies — the indexer's entity
        // types live in this target, so the package can't install it itself.
        SpotlightSupport.installIndexer(UNESSpotlightIndexer())

        FirebaseApp.configure()
        log.info("firebase configured")
        #if DEBUG
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #endif
        Crashlytics.crashlytics().setUserID(MachineIdentity.id)

        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        application.registerForRemoteNotifications()

        configureRemoteConfig()
        configurePostHog()
        log.info("app launch finished")
        return true
    }

    /// Events only — session replay and screen autocapture stay off, and DEBUG
    /// builds opt out so development runs don't pollute the dashboards.
    private func configurePostHog() {
        let config = PostHogConfig(
            apiKey: "phc_uhYjeNJg9RpdEknMM8mNxdRqeiEtr4jqEm6zsv9TETqu",
            host: "https://a.forcetower.dev"
        )
        config.sessionReplay = false
        config.captureScreenViews = false
        #if DEBUG
        config.debug = true
        config.optOut = true
        #endif
        PostHogSDK.shared.setup(config)
        let sink = PostHogAnalyticsSink()
        AnalyticsSupport.install(sink)
        // Same super property Android stamps, so PostHog rows line up with
        // the OTel logs keyed on machine_id.
        sink.register(properties: ["machine_id": MachineIdentity.id])
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
        let remoteConfig = RemoteConfig.remoteConfig()
        FeatureFlags.update(
            enrollmentEnabled: remoteConfig.configValue(forKey: "enable_enrollment").boolValue,
            certificateEnabled: remoteConfig.configValue(forKey: "enable_enrollment_certificate").boolValue,
            historyEnabled: remoteConfig.configValue(forKey: "enable_academic_history").boolValue,
            paradoxoEnabled: remoteConfig.configValue(forKey: "enable_paradoxo").boolValue,
            materialsEnabled: remoteConfig.configValue(forKey: "enable_materials").boolValue,
            campusEventEnabled: remoteConfig.configValue(forKey: "enable_campus_event").boolValue,
            documentCaptchaSiteKey: remoteConfig.configValue(forKey: "document_captcha_site_key").stringValue,
            documentCaptchaBaseURL: remoteConfig.configValue(forKey: "document_captcha_base_url").stringValue
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
    nonisolated func messaging(_ messaging: Messaging, didReceiveRegistration installationId: String?) {
        let log = Log.scoped("AppDelegate")
        guard let installationId else {
            log.warn("FID was nil")
            return
        }
        log.info("FID received length=\(installationId.count)")
        Task { await PushTokens.registrationReceived(installationId) }
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let data = Self.stringPayload(of: notification)
        if !data.isEmpty {
            Task { await PushEvents.received(data) }
        }
        completionHandler([.list, .sound, .banner])
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        // Only the tap itself routes — a custom-action dismissal shouldn't.
        let data = Self.stringPayload(of: response.notification)
        if response.actionIdentifier == UNNotificationDefaultActionIdentifier, !data.isEmpty {
            Task { await PushEvents.tapped(data) }
        }
        completionHandler()
    }

    /// FCM data payloads ride along as string entries in userInfo; the
    /// non-string values ("aps" & friends) are system metadata.
    private nonisolated static func stringPayload(of notification: UNNotification) -> [String: String] {
        notification.request.content.userInfo
            .reduce(into: [String: String]()) { payload, entry in
                guard let key = entry.key as? String, let value = entry.value as? String else { return }
                payload[key] = value
            }
    }
}

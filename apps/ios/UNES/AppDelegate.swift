import FirebaseAnalytics
import FirebaseCore
import FirebaseCrashlytics
import FirebaseMessaging
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
        Analytics.setAnalyticsCollectionEnabled(false)
        #endif
        Crashlytics.crashlytics().setUserID(MachineIdentity.id)

        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        application.registerForRemoteNotifications()

        configureRemoteSettings()
        configurePostHog()
        log.info("app launch finished")
        return true
    }

    /// Events only — session replay and screen autocapture stay off, and DEBUG
    /// builds opt out so development runs don't pollute the dashboards.
    private func configurePostHog() {
        let config = PostHogConfig(
            projectToken: "phc_uhYjeNJg9RpdEknMM8mNxdRqeiEtr4jqEm6zsv9TETqu",
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

    /// Publish once from whatever both layers already have on disk, then again
    /// on every change either of them reports.
    ///
    /// The composite outlives this call by being captured in the change
    /// callback — which is also what keeps lever's client, and its stream,
    /// alive for the process.
    private func configureRemoteSettings() {
        let settings = CompositeRemoteSettings.live()
        Self.publishFlags(from: settings)
        settings.start { Self.publishFlags(from: settings) }
    }

    /// Off the main actor — both layers call back on their own queues.
    private nonisolated static func publishFlags(from settings: some RemoteSettings) {
        FeatureFlags.update(
            enrollmentEnabled: settings.bool(.enrollment),
            certificateEnabled: settings.bool(.enrollmentCertificate),
            historyEnabled: settings.bool(.academicHistory),
            paradoxoEnabled: settings.bool(.paradoxo),
            materialsEnabled: settings.bool(.materials),
            libraryEnabled: settings.bool(.library),
            campusEventEnabled: settings.bool(.campusEvent),
            evaluationRemindersEnabled: settings.bool(.evaluationReminders),
            retrospectiveEnabled: settings.bool(.retrospective),
            documentCaptchaSiteKey: settings.string(.documentCaptchaSiteKey),
            documentCaptchaBaseURL: settings.string(.documentCaptchaBaseURL)
        )
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        log.info("APNS token registered bytes=\(deviceToken.count)")
        Messaging.messaging().apnsToken = deviceToken
    }

    /// Background wake for content-available pushes (spec 0008): the backend
    /// wake-marks mirror-visible changes so the app pulls the mirror before
    /// the user opens it. In foreground the same alert also hits
    /// `willPresent` — hand off to that debounced path and report no data so
    /// the two doors can't double-refresh.
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any]
    ) async -> UIBackgroundFetchResult {
        let data = Self.stringPayload(of: userInfo)
        guard !data.isEmpty else { return .noData }
        if application.applicationState == .active {
            await PushEvents.received(data)
            return .noData
        }
        switch await PushEvents.backgroundSync(data) {
        case .newData: return .newData
        case .noData: return .noData
        case .failed: return .failed
        }
    }
}

extension AppDelegate: MessagingDelegate, UNUserNotificationCenterDelegate {
    /// Fires on every launch once the APNS token is set, so each app open
    /// refreshes the token cache before the registrar re-sends it.
    nonisolated func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        Log.scoped("AppDelegate").info("fcm token received length=\(fcmToken.count)")
        Task { await PushTokens.fcmTokenReceived(fcmToken) }
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

    private nonisolated static func stringPayload(of notification: UNNotification) -> [String: String] {
        stringPayload(of: notification.request.content.userInfo)
    }

    /// FCM data payloads ride along as string entries in userInfo; the
    /// non-string values ("aps" & friends) are system metadata.
    private nonisolated static func stringPayload(of userInfo: [AnyHashable: Any]) -> [String: String] {
        userInfo.reduce(into: [String: String]()) { payload, entry in
            guard let key = entry.key as? String, let value = entry.value as? String else { return }
            payload[key] = value
        }
    }
}

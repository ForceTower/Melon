#if os(watchOS)
import ComposableArchitecture
import Foundation
import UserNotifications

private let log = Log.scoped("WatchNotifications")

/// Watch-side notification tap handling. Mirrored iPhone notifications carry
/// the same userInfo as the originals (the FCM `data` keys, including `url`),
/// so a tap rides the shared `Deeplinks` grammar and the buffered
/// `IntentRouter` hub — the same cold-launch-safe path the phone uses.
public enum WatchNotifications {
    /// Call from `App.init` so the delegate is in place before launch
    /// completes — required for a tap that cold-starts the app.
    @MainActor public static func install() {
        UNUserNotificationCenter.current().delegate = delegate
    }

    /// The center holds its delegate weakly.
    private static let delegate = WatchNotificationDelegate()

    /// Deliberately not `Deeplinks.post`: its signed-out gate reads the
    /// keychain session, which never exists on the watch. Signed-out gating
    /// happens where the route is consumed, against the snapshot.
    static func tapped(_ data: [String: String]) {
        @Dependency(\.intentRouter) var intentRouter
        // Key names only — the url value embeds message ids.
        log.info("tap received keys=\(data.keys.sorted().joined(separator: ","))")
        guard let route = WatchDeeplinkResolver.route(for: data) else {
            log.info("tap unroutable -> plain open")
            return
        }
        intentRouter.open(route)
        log.info("tap route posted kind=\(route.kindLabel)")
    }
}

private final class WatchNotificationDelegate: NSObject, UNUserNotificationCenterDelegate, Sendable {
    /// Without this, an installed delegate suppresses notifications that
    /// arrive while the watch app is frontmost.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.list, .sound, .banner])
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        // Only the tap itself routes — a custom-action dismissal shouldn't.
        if response.actionIdentifier == UNNotificationDefaultActionIdentifier {
            let data = response.notification.request.content.userInfo
                .reduce(into: [String: String]()) { payload, entry in
                    guard let key = entry.key as? String, let value = entry.value as? String else { return }
                    payload[key] = value
                }
            WatchNotifications.tapped(data)
        }
        completionHandler()
    }
}
#endif

import ComposableArchitecture

private let log = Log.scoped("PushEvents")

/// App-delegate entry point for notifications presented while the app is in
/// the foreground — hands the FCM data payload to `PushClient`, which decides
/// whether it signals a data change worth refreshing for.
public enum PushEvents {
    public static func received(_ data: [String: String]) async {
        @Dependency(\.push) var push
        log.debug("foreground notification keys=\(data.keys.sorted().joined(separator: ","))")
        await push.dataNotificationReceived(data)
    }

    /// Notification-tap entry point — the tap signals fresh data and carries
    /// the destination as a `unes://` URL in the `url` data key.
    public static func tapped(_ data: [String: String]) async {
        log.info("notification tap keys=\(data.keys.sorted().joined(separator: ","))")
        // Hub-free on purpose: a cold tap-launch has no hub subscriber yet,
        // and on a warm one the single-flight latch absorbs the duplicate
        // refresh the foreground path may already be running.
        _ = await backgroundSync(data)
        if let url = data["url"] {
            Deeplinks.post(url)
        }
    }

    /// Background-wake entry for content-available pushes (spec 0008), also
    /// backing taps. Deliberately hub-free: the hub has no replay, and on a
    /// background launch the scene's `.task` subscription that drains it may
    /// never start — so this path refreshes the mirror directly.
    public static func backgroundSync(_ data: [String: String]) async -> BackgroundSyncResult {
        guard let kind = data["kind"] else { return .noData }
        log.info("background sync start kind=\(kind)")
        return await latch.run {
            @Dependency(\.homeRepository) var homeRepository
            @Dependency(\.date) var date
            do {
                try await homeRepository.refresh(now: date.now)
                log.info("background sync refreshed the mirror kind=\(kind)")
                return .newData
            } catch {
                log.warn("background sync refresh failed", error: error)
                return .failed
            }
        }
    }
}

/// Outcome of a background push refresh, mirroring `UIBackgroundFetchResult`
/// without importing UIKit (UNESKit also builds for watchOS).
public enum BackgroundSyncResult: Equatable, Sendable {
    case newData
    case noData
    case failed
}

/// Coalesces overlapping background refreshes into one in-flight run — the
/// OS can deliver several pushes of a burst as separate wakes, and a tap can
/// land while a wake's refresh is still going.
private actor BackgroundSyncLatch {
    private var inFlight: Task<BackgroundSyncResult, Never>?

    func run(_ refresh: @escaping @Sendable () async -> BackgroundSyncResult) async -> BackgroundSyncResult {
        if let inFlight { return await inFlight.value }
        let task = Task { await refresh() }
        inFlight = task
        defer { inFlight = nil }
        return await task.value
    }
}

private let latch = BackgroundSyncLatch()

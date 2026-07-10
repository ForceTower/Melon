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
}

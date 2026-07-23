import ComposableArchitecture

private let log = Log.scoped("PushTokens")

/// App-delegate entry point for FCM registration (registration tokens) — the
/// foreground notification sibling is `PushEvents`; everything else stays
/// inside UNESKit.
public enum PushTokens {
    public static func fcmTokenReceived(_ token: String) async {
        @Dependency(\.push) var push
        log.info("fcm token received length=\(token.count) -> forwarding to client")
        await push.fcmTokenReceived(token)
    }
}

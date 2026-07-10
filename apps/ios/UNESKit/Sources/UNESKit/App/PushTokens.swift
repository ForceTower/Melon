import ComposableArchitecture

private let log = Log.scoped("PushTokens")

/// App-delegate entry point for FCM registration tokens — the foreground
/// notification sibling is `PushEvents`; everything else stays inside UNESKit.
public enum PushTokens {
    public static func received(_ token: String) async {
        @Dependency(\.push) var push
        log.info("push token received length=\(token.count) -> forwarding to client")
        await push.tokenReceived(token)
    }
}

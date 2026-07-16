import ComposableArchitecture

private let log = Log.scoped("PushTokens")

/// App-delegate entry point for FCM registration (Firebase Installation IDs) —
/// the foreground notification sibling is `PushEvents`; everything else stays
/// inside UNESKit.
public enum PushTokens {
    public static func registrationReceived(_ installationId: String) async {
        @Dependency(\.push) var push
        log.info("fid received length=\(installationId.count) -> forwarding to client")
        await push.fidReceived(installationId)
    }
}

import ComposableArchitecture

/// App-delegate entry point for FCM registration tokens — the only push
/// surface the app target needs; everything else stays inside UNESKit.
public enum PushTokens {
    public static func received(_ token: String) async {
        @Dependency(\.push) var push
        await push.tokenReceived(token)
    }
}

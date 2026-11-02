import ComposableArchitecture

/// App-delegate entry point for "Seguindo o 087" on the lock screen: a Live
/// Activity outlives the process that started it, so every launch picks its
/// push token and lifecycle back up.
public enum CommuteActivities {
    public static func resume() async {
        @Dependency(\.commuteActivity) var commuteActivity
        await commuteActivity.resume()
    }
}

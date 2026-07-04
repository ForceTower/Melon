import ComposableArchitecture

/// Keeps the paired watch fed: observes the mirrored watch payload,
/// republishes it over WatchConnectivity whenever it changes, and re-sends
/// the latest payload when the session (re)activates. No-op on platforms
/// without a paired watch.
@DependencyClient
struct WatchSyncClient: Sendable {
    /// Runs for the app's whole lifetime; cancelling the task stops it.
    var run: @Sendable () async -> Void
}

extension WatchSyncClient: TestDependencyKey {
    static let testValue = WatchSyncClient()
    static let previewValue = WatchSyncClient(run: {})
}

extension DependencyValues {
    var watchSync: WatchSyncClient {
        get { self[WatchSyncClient.self] }
        set { self[WatchSyncClient.self] = newValue }
    }
}

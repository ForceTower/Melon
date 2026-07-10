import ComposableArchitecture

/// Keeps the home/lock-screen widgets fed: observes the mirrored schedule,
/// republishes it into the shared App Group container whenever it changes,
/// and asks WidgetKit to reload the affected timelines.
@DependencyClient
struct WidgetSyncClient: Sendable {
    /// Runs for the app's whole lifetime; cancelling the task stops it.
    var run: @Sendable () async -> Void
}

extension WidgetSyncClient: TestDependencyKey {
    static let testValue = WidgetSyncClient()
    static let previewValue = WidgetSyncClient(run: {})
}

extension DependencyValues {
    var widgetSync: WidgetSyncClient {
        get { self[WidgetSyncClient.self] }
        set { self[WidgetSyncClient.self] = newValue }
    }
}

import ComposableArchitecture

/// Build / device metadata for the about sheet. `current` answers
/// synchronously with the release defaults; `resolved` refines the channel
/// through StoreKit so TestFlight installs get the right label.
@DependencyClient
struct AppInfoClient: Sendable {
    var current: @Sendable () -> AppInfo = { .preview }
    var resolved: @Sendable () async -> AppInfo = { .preview }
}

extension AppInfoClient: TestDependencyKey {
    static let testValue = AppInfoClient()

    static let previewValue = AppInfoClient(
        current: { .preview },
        resolved: { .preview }
    )
}

extension DependencyValues {
    var appInfo: AppInfoClient {
        get { self[AppInfoClient.self] }
        set { self[AppInfoClient.self] = newValue }
    }
}

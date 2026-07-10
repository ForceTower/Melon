import ComposableArchitecture

/// Reads and switches the Home Screen icon through UIKit's alternate-icon API.
@DependencyClient
struct AppIconClient: Sendable {
    var current: @Sendable () async -> AppIcon = { .aurora }
    var set: @Sendable (AppIcon) async throws -> Void
}

extension AppIconClient: TestDependencyKey {
    static let testValue = AppIconClient()
    static let previewValue = AppIconClient(current: { .aurora }, set: { _ in })
}

extension DependencyValues {
    var appIconClient: AppIconClient {
        get { self[AppIconClient.self] }
        set { self[AppIconClient.self] = newValue }
    }
}

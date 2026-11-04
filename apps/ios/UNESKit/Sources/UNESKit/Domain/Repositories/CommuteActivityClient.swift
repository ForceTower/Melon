import ComposableArchitecture

/// Following a departure on the lock screen and in the Dynamic Island. The
/// device starts the Live Activity; the backend keeps it current with
/// ActivityKit pushes (`api/transit/live-activities`) until the bus passes
/// the stop.
@DependencyClient
struct CommuteActivityClient: Sendable {
    var status: @Sendable (_ arrivalId: String) async -> CommuteActivityStatus = { _ in .unavailable }
    /// Replaces whichever bus was followed before: one at a time.
    var start: @Sendable (
        _ attributes: CommuteActivityAttributes,
        _ state: CommuteActivityAttributes.ContentState,
        _ vehicleId: String?
    ) async throws -> Void
    var stop: @Sendable (_ arrivalId: String) async -> Void
    /// The bus on the lock screen now; nil when none is.
    var current: @Sendable () async -> CommuteActivityContent? = { nil }
    /// Logout: a signed-out student's lock screen stops following anything.
    var stopAll: @Sendable () async -> Void
    /// App launch: activities still running pick their push tokens back up,
    /// and ones that ended while the app was not running are unregistered.
    var resume: @Sendable () async -> Void
}

enum CommuteActivityStatus: Equatable, Sendable {
    /// Live Activities are off for the app.
    case unavailable
    case available
    /// This departure is already on the lock screen.
    case running
}

extension CommuteActivityClient: TestDependencyKey {
    static let testValue = CommuteActivityClient()

    static let previewValue = CommuteActivityClient(
        status: { _ in .available },
        start: { _, _, _ in },
        stop: { _ in },
        current: { nil },
        stopAll: {},
        resume: {}
    )
}

extension DependencyValues {
    var commuteActivity: CommuteActivityClient {
        get { self[CommuteActivityClient.self] }
        set { self[CommuteActivityClient.self] = newValue }
    }
}

#if os(iOS)
import ActivityKit
#endif
import ComposableArchitecture
import Foundation

private let log = Log.scoped("CommuteActivity")

extension CommuteActivityClient: DependencyKey {
    static let liveValue: CommuteActivityClient = {
        #if os(iOS)
        let coordinator = CommuteActivityCoordinator(system: .live, defaults: .standard)
        return CommuteActivityClient(
            status: { await coordinator.status(arrivalId: $0) },
            start: { try await coordinator.start($0, state: $1, vehicleId: $2) },
            stop: { await coordinator.stop(arrivalId: $0) },
            current: { await coordinator.current() },
            stopAll: { await coordinator.stopAll() },
            resume: { await coordinator.resume() }
        )
        #else
        return CommuteActivityClient(
            status: { _ in .unavailable },
            start: { _, _, _ in },
            stop: { _ in },
            current: { nil },
            stopAll: {},
            resume: {}
        )
        #endif
    }()
}

#if os(iOS)
/// The part of ActivityKit the coordinator drives, so tests can play the
/// system. An `Activity` isn't Sendable, so it never leaves these closures.
struct CommuteActivitySystem: Sendable {
    var isEnabled: @Sendable () -> Bool
    var activities: @Sendable () -> [CommuteLiveActivity]
    var request: @Sendable (
        _ attributes: CommuteActivityAttributes,
        _ content: ActivityContent<CommuteActivityAttributes.ContentState>
    ) throws -> CommuteLiveActivity
    /// Ends the activity and takes it off the lock screen at once.
    var end: @Sendable (_ id: String) async -> Void
    var pushTokenUpdates: @Sendable (_ id: String) -> AsyncStream<Data>
    var stateUpdates: @Sendable (_ id: String) -> AsyncStream<ActivityState>
}

/// A Live Activity as ActivityKit last reported it.
struct CommuteLiveActivity: Sendable {
    var id: String
    var attributes: CommuteActivityAttributes
    var state: ActivityState
    var content: ActivityContent<CommuteActivityAttributes.ContentState>
    var pushToken: Data?
}

extension CommuteActivitySystem {
    static let live = CommuteActivitySystem(
        isEnabled: { ActivityAuthorizationInfo().areActivitiesEnabled },
        activities: { Activity<CommuteActivityAttributes>.activities.map(CommuteLiveActivity.init) },
        request: { attributes, content in
            CommuteLiveActivity(try Activity.request(attributes: attributes, content: content, pushType: .token))
        },
        end: { id in
            await activity(id)?.end(nil, dismissalPolicy: .immediate)
        },
        pushTokenUpdates: { id in updates(of: id) { $0.pushTokenUpdates } },
        stateUpdates: { id in updates(of: id) { $0.activityStateUpdates } }
    )

    private static func activity(_ id: String) -> Activity<CommuteActivityAttributes>? {
        Activity<CommuteActivityAttributes>.activities.first { $0.id == id }
    }

    private static func updates<Updates: AsyncSequence & SendableMetatype>(
        of id: String,
        _ sequence: @escaping @Sendable (Activity<CommuteActivityAttributes>) -> Updates
    ) -> AsyncStream<Updates.Element> where Updates.Element: Sendable, Updates.Failure == Never {
        AsyncStream { continuation in
            let task = Task {
                if let activity = activity(id) {
                    for await update in sequence(activity) {
                        continuation.yield(update)
                    }
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}

private extension CommuteLiveActivity {
    init(_ activity: Activity<CommuteActivityAttributes>) {
        self.init(
            id: activity.id,
            attributes: activity.attributes,
            state: activity.activityState,
            content: activity.content,
            pushToken: activity.pushToken
        )
    }
}

/// Owns the running activities' push-token and lifecycle observers.
actor CommuteActivityCoordinator {
    /// Matches the backend: this long without a push, the band freezes.
    private static let staleAfter: TimeInterval = 3 * 60
    /// Activities the backend knows about, so one that ended while the app
    /// was not running still gets unregistered.
    static let registeredKey = "commute_live_activities"
    /// The bus each activity started with, by activity id, so registering
    /// again after a relaunch still sends it.
    private static let vehiclesKey = "commute_live_activity_vehicles"

    private let system: CommuteActivitySystem
    private let defaults: UserDefaults
    /// Activities still running as far as this process knows.
    private var observers: [String: [Task<Void, Never>]] = [:]
    /// Start, stop, stopAll and resume each await the network; interleaved,
    /// a later tap would act on activities an earlier one was still replacing.
    private var isBusy = false
    private var waiting: [CheckedContinuation<Void, Never>] = []

    init(system: CommuteActivitySystem, defaults: UserDefaults) {
        self.system = system
        self.defaults = defaults
    }

    func status(arrivalId: String) -> CommuteActivityStatus {
        guard system.isEnabled() else { return .unavailable }
        let running = system.activities().contains {
            $0.attributes.arrivalId == arrivalId && Self.isRunning($0.state)
        }
        return running ? .running : .available
    }

    func start(
        _ attributes: CommuteActivityAttributes,
        state: CommuteActivityAttributes.ContentState,
        vehicleId: String?
    ) async throws {
        try await serially {
            for activity in system.activities() {
                await end(id: activity.id)
            }
            let activity = try system.request(
                attributes,
                ActivityContent(state: state, staleDate: Date().addingTimeInterval(Self.staleAfter))
            )
            log.info("started line=\(attributes.lineNumber) direction=\(attributes.direction.rawValue)")
            if let vehicleId {
                defaults.set(vehicleIds().merging([activity.id: vehicleId]) { $1 }, forKey: Self.vehiclesKey)
            }
            observe(activity, vehicleId: vehicleId)
        }
    }

    func stop(arrivalId: String) async {
        await serially {
            for activity in system.activities() where activity.attributes.arrivalId == arrivalId {
                await end(id: activity.id)
            }
        }
    }

    func current() -> CommuteActivityContent? {
        guard let activity = system.activities().first(where: { Self.isRunning($0.state) }) else { return nil }
        let content = activity.content
        return CommuteActivityContent(
            attributes: activity.attributes,
            state: content.state,
            // Every update, the app's and the backend's, is stale this long after it was sent.
            receivedAt: content.staleDate?.addingTimeInterval(-Self.staleAfter) ?? .distantPast
        )
    }

    func stopAll() async {
        await serially {
            for activity in system.activities() {
                await end(id: activity.id)
            }
        }
    }

    func resume() async {
        await serially {
            let activities = system.activities().filter { Self.isRunning($0.state) }
            let vehicles = vehicleIds()
            for activity in activities {
                observe(activity, vehicleId: vehicles[activity.id])
                // Its first registration never went through (offline, no FCM token yet).
                if let token = activity.pushToken, !registered().contains(activity.id) {
                    await register(activity.attributes, id: activity.id, token: token, vehicleId: vehicles[activity.id])
                }
            }
            let running = Set(activities.map(\.id))
            defaults.set(vehicles.filter { running.contains($0.key) }, forKey: Self.vehiclesKey)
            for id in registered() where !running.contains(id) {
                await unregister(id)
            }
        }
    }

    private func end(id: String) async {
        await system.end(id)
        await finished(id)
    }

    /// ActivityKit hands out the push token after the request returns and may
    /// rotate it later; each one is (re)registered. The backend ending the
    /// activity, or the student swiping it away, ends the observation.
    private func observe(_ activity: CommuteLiveActivity, vehicleId: String?) {
        let id = activity.id
        guard observers[id] == nil else { return }
        let attributes = activity.attributes
        let tokens = system.pushTokenUpdates(id)
        let states = system.stateUpdates(id)
        observers[id] = [
            Task {
                for await token in tokens {
                    await self.register(attributes, id: id, token: token, vehicleId: vehicleId)
                }
            },
            Task {
                for await state in states where !Self.isRunning(state) {
                    await self.finished(id)
                    return
                }
            },
        ]
    }

    private func finished(_ id: String) async {
        observers.removeValue(forKey: id)?.forEach { $0.cancel() }
        await unregister(id)
    }

    private func register(_ attributes: CommuteActivityAttributes, id: String, token: Data, vehicleId: String?) async {
        @Dependency(\.apiClient) var apiClient
        @Dependency(\.push) var push
        guard let deviceToken = await push.currentToken() else {
            // Without it FCM can't route the update; the activity still shows
            // its starting estimate and goes stale on schedule.
            log.warn("register skipped: no fcm token yet")
            return
        }
        let body = RegisterBody(
            activityId: id,
            pushToken: token.map { String(format: "%02x", $0) }.joined(),
            deviceToken: deviceToken,
            lineNumber: attributes.lineNumber,
            direction: attributes.direction.rawValue,
            stopId: attributes.stopId,
            arrivalId: attributes.arrivalId,
            vehicleId: vehicleId
        )
        do {
            try await uncancelled { [apiClient] in
                try await apiClient.post(to: "api/transit/live-activities", body: body)
            }
            remember(id)
            log.info("registered line=\(attributes.lineNumber)")
        } catch {
            log.warn("register failed", error: error)
            return
        }
        // It ended while the POST was in flight, before there was anything to unregister.
        if observers[id] == nil {
            await unregister(id)
        }
    }

    private func unregister(_ id: String) async {
        guard registered().contains(id) else { return }
        @Dependency(\.apiClient) var apiClient
        do {
            try await uncancelled { [apiClient] in
                try await apiClient.delete("api/transit/live-activities/\(id)")
            }
            forget(id)
        } catch {
            // Retried on the next launch; until then the backend ends it
            // when the bus passes or the activity expires.
            log.warn("unregister failed", error: error)
        }
    }

    /// On its own task: `finished` cancels the observers, even the one it runs
    /// on, and a cancelled request either never goes out or lands unrecorded.
    private func uncancelled(_ request: @escaping @Sendable () async throws -> Void) async throws {
        try await Task { try await request() }.value
    }

    private func serially<T>(_ operation: () async throws -> T) async rethrows -> T {
        if isBusy {
            await withCheckedContinuation { waiting.append($0) }
        } else {
            isBusy = true
        }
        defer {
            if waiting.isEmpty {
                isBusy = false
            } else {
                waiting.removeFirst().resume()
            }
        }
        return try await operation()
    }

    private static func isRunning(_ state: ActivityState) -> Bool {
        state == .active || state == .stale
    }

    private func vehicleIds() -> [String: String] {
        defaults.dictionary(forKey: Self.vehiclesKey) as? [String: String] ?? [:]
    }

    private func registered() -> [String] {
        defaults.stringArray(forKey: Self.registeredKey) ?? []
    }

    private func remember(_ id: String) {
        let ids = registered()
        guard !ids.contains(id) else { return }
        defaults.set(ids + [id], forKey: Self.registeredKey)
    }

    private func forget(_ id: String) {
        defaults.set(registered().filter { $0 != id }, forKey: Self.registeredKey)
    }
}

/// `POST api/transit/live-activities`; contract in `contracts/v1/transit.json`
/// → `liveActivity.register`.
private struct RegisterBody: Encodable, Sendable {
    let activityId: String
    let pushToken: String
    let deviceToken: String
    let lineNumber: String
    let direction: String
    let stopId: String
    let arrivalId: String
    let vehicleId: String?
}
#endif

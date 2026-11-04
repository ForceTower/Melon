#if os(iOS)
import ActivityKit
#endif
import ComposableArchitecture
import Foundation

private let log = Log.scoped("CommuteActivity")

extension CommuteActivityClient: DependencyKey {
    static let liveValue: CommuteActivityClient = {
        #if os(iOS)
        let coordinator = CommuteActivityCoordinator()
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
/// Owns the running activities' push-token and lifecycle observers. An
/// `Activity` isn't Sendable, so it never leaves this actor.
private actor CommuteActivityCoordinator {
    /// Matches the backend: this long without a push, the band freezes.
    private static let staleAfter: TimeInterval = 3 * 60
    /// Activities the backend knows about, so one that ended while the app
    /// was not running still gets unregistered.
    private static let registeredKey = "commute_live_activities"

    private var observers: [String: [Task<Void, Never>]] = [:]

    func status(arrivalId: String) -> CommuteActivityStatus {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return .unavailable }
        let running = Activity<CommuteActivityAttributes>.activities.contains {
            $0.attributes.arrivalId == arrivalId && Self.isRunning($0.activityState)
        }
        return running ? .running : .available
    }

    func start(
        _ attributes: CommuteActivityAttributes,
        state: CommuteActivityAttributes.ContentState,
        vehicleId: String?
    ) async throws {
        for activity in Activity<CommuteActivityAttributes>.activities {
            await end(id: activity.id)
        }
        let activity = try Activity.request(
            attributes: attributes,
            content: ActivityContent(state: state, staleDate: Date().addingTimeInterval(Self.staleAfter)),
            pushType: .token
        )
        log.info("started line=\(attributes.lineNumber) direction=\(attributes.direction.rawValue)")
        observe(activity, vehicleId: vehicleId)
    }

    func stop(arrivalId: String) async {
        for activity in Activity<CommuteActivityAttributes>.activities where activity.attributes.arrivalId == arrivalId {
            await end(id: activity.id)
        }
    }

    func current() -> CommuteActivityContent? {
        guard let activity = Activity<CommuteActivityAttributes>.activities.first(where: { Self.isRunning($0.activityState) })
        else { return nil }
        let content = activity.content
        return CommuteActivityContent(
            attributes: activity.attributes,
            state: content.state,
            // Every update, the app's and the backend's, is stale this long after it was sent.
            receivedAt: content.staleDate?.addingTimeInterval(-Self.staleAfter) ?? .distantPast
        )
    }

    func stopAll() async {
        for activity in Activity<CommuteActivityAttributes>.activities {
            await end(id: activity.id)
        }
    }

    func resume() async {
        let activities = Activity<CommuteActivityAttributes>.activities.filter { Self.isRunning($0.activityState) }
        for activity in activities {
            observe(activity, vehicleId: nil)
            // Its first registration never went through (offline, no FCM token yet).
            if let token = activity.pushToken, !registered().contains(activity.id) {
                await register(activity.attributes, id: activity.id, token: token, vehicleId: nil)
            }
        }
        let running = Set(activities.map(\.id))
        for id in registered() where !running.contains(id) {
            await unregister(id)
        }
    }

    private func end(id: String) async {
        await Self.dismiss(id: id)
        await finished(id)
    }

    /// `end` runs off the actor, so the activity is looked up there rather
    /// than handed over.
    private nonisolated static func dismiss(id: String) async {
        guard let activity = Activity<CommuteActivityAttributes>.activities.first(where: { $0.id == id }) else { return }
        await activity.end(nil, dismissalPolicy: .immediate)
    }

    /// ActivityKit hands out the push token after the request returns and may
    /// rotate it later; each one is (re)registered. The backend ending the
    /// activity, or the student swiping it away, ends the observation.
    private func observe(_ activity: Activity<CommuteActivityAttributes>, vehicleId: String?) {
        let id = activity.id
        guard observers[id] == nil else { return }
        observers[id] = [
            Task {
                for await token in activity.pushTokenUpdates {
                    await self.register(activity.attributes, id: id, token: token, vehicleId: vehicleId)
                }
            },
            Task {
                for await state in activity.activityStateUpdates where !Self.isRunning(state) {
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
            try await apiClient.post(to: "api/transit/live-activities", body: body)
            remember(id)
            log.info("registered line=\(attributes.lineNumber)")
        } catch {
            log.warn("register failed", error: error)
        }
    }

    private func unregister(_ id: String) async {
        guard registered().contains(id) else { return }
        @Dependency(\.apiClient) var apiClient
        do {
            try await apiClient.delete("api/transit/live-activities/\(id)")
            forget(id)
        } catch {
            // Retried on the next launch; until then the backend ends it
            // when the bus passes or the activity expires.
            log.warn("unregister failed", error: error)
        }
    }

    private static func isRunning(_ state: ActivityState) -> Bool {
        state == .active || state == .stale
    }

    private func registered() -> [String] {
        UserDefaults.standard.stringArray(forKey: Self.registeredKey) ?? []
    }

    private func remember(_ id: String) {
        let ids = registered()
        guard !ids.contains(id) else { return }
        UserDefaults.standard.set(ids + [id], forKey: Self.registeredKey)
    }

    private func forget(_ id: String) {
        UserDefaults.standard.set(registered().filter { $0 != id }, forKey: Self.registeredKey)
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

#if os(iOS)
import ActivityKit
import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

struct CommuteActivityCoordinatorTests {
    fileprivate static let state = CommuteActivityAttributes.ContentState(status: .live, atStop: false, signalLost: false)

    private static func attributes(arrivalId: String) -> CommuteActivityAttributes {
        CommuteActivityAttributes(
            lineNumber: "087",
            lineName: "UEFS",
            direction: .toCampus,
            stopId: TransitFixtures.myStopId,
            arrivalId: arrivalId,
            stops: [],
            mineIndex: 0
        )
    }

    @Test
    func swipingTheActivityAwayUnregistersIt() async throws {
        let harness = Harness()
        let coordinator = harness.launch()
        try await harness.run {
            try await coordinator.start(Self.attributes(arrivalId: "b"), state: Self.state, vehicleId: "90001")
        }
        let id = try #require(harness.activities.running().first?.id)
        harness.activities.issueToken(Data([0xAB]), to: id)
        await eventually { harness.registered == [id] }

        harness.activities.swipeAway(id)
        await eventually { !harness.api.calls("DELETE").isEmpty }
        #expect(harness.api.calls("DELETE").map(\.wasCancelled) == [false])
        #expect(harness.api.calls("DELETE").map(\.path) == ["api/transit/live-activities/\(id)"])
        await eventually { harness.registered.isEmpty }
    }

    @Test
    func aRegistrationRetriedAfterARelaunchStillSendsTheBus() async throws {
        let harness = Harness()
        harness.api.fail("POST")
        let firstLaunch = harness.launch()
        try await harness.run {
            try await firstLaunch.start(Self.attributes(arrivalId: "b"), state: Self.state, vehicleId: "90001")
        }
        let id = try #require(harness.activities.running().first?.id)
        harness.activities.issueToken(Data([0xAB]), to: id)
        await eventually { harness.api.calls("POST").count == 1 }

        harness.api.recover("POST")
        let secondLaunch = harness.launch()
        await harness.run { await secondLaunch.resume() }

        let posts = harness.api.calls("POST")
        #expect(posts.count == 2)
        let retry = try JSONDecoder().decode(Registration.self, from: try #require(posts.last?.body))
        #expect(retry.activityId == id)
        #expect(retry.vehicleId == "90001")
        #expect(harness.registered == [id])
    }

    @Test
    func anActivityThatEndsWhileItsRegistrationIsInFlightIsUnregistered() async throws {
        let harness = Harness()
        // A previous launch started it, but its registration never went through.
        let id = harness.activities.add(Self.attributes(arrivalId: "b"), pushToken: Data([0xAB]))
        harness.api.hold("POST")
        let coordinator = harness.launch()
        let resumed = Task { await harness.run { await coordinator.resume() } }
        await eventually { harness.api.calls("POST").count == 1 }

        harness.activities.swipeAway(id)
        await eventually { harness.activities.observerCount(of: id) == 0 }
        harness.api.release("POST")
        await resumed.value

        await eventually { harness.api.calls("DELETE").map(\.path) == ["api/transit/live-activities/\(id)"] }
        await eventually { harness.registered.isEmpty }
    }

    @Test
    func stoppingWhileAStartStillReplacesTheOldBusLeavesNothingRunning() async throws {
        let harness = Harness()
        let old = harness.activities.add(Self.attributes(arrivalId: "a"), pushToken: nil)
        harness.remember(old)
        harness.api.hold("DELETE")
        let coordinator = harness.launch()

        let started = Task {
            try await harness.run {
                try await coordinator.start(Self.attributes(arrivalId: "b"), state: Self.state, vehicleId: nil)
            }
        }
        await eventually { harness.api.calls("DELETE").count == 1 }
        let stopped = Task { await harness.run { await coordinator.stop(arrivalId: "b") } }
        try await Task.sleep(for: .milliseconds(100))
        harness.api.release("DELETE")
        try await started.value
        await stopped.value

        #expect(harness.activities.running(arrivalId: "b").isEmpty)
    }

    @Test
    func startStopStartInQuickSuccessionFollowsTheBusOnce() async throws {
        let harness = Harness()
        let old = harness.activities.add(Self.attributes(arrivalId: "a"), pushToken: nil)
        harness.remember(old)
        harness.api.hold("DELETE")
        let coordinator = harness.launch()

        let first = Task {
            try await harness.run {
                try await coordinator.start(Self.attributes(arrivalId: "b"), state: Self.state, vehicleId: nil)
            }
        }
        await eventually { harness.api.calls("DELETE").count == 1 }
        let stopped = Task { await harness.run { await coordinator.stop(arrivalId: "b") } }
        try await Task.sleep(for: .milliseconds(50))
        let second = Task {
            try await harness.run {
                try await coordinator.start(Self.attributes(arrivalId: "b"), state: Self.state, vehicleId: nil)
            }
        }
        try await Task.sleep(for: .milliseconds(100))
        harness.api.release("DELETE")
        try await first.value
        await stopped.value
        try await second.value

        #expect(harness.activities.running().map(\.attributes.arrivalId) == ["b"])
    }
}

private struct Registration: Decodable {
    let activityId: String
    let vehicleId: String?
}

private func eventually(
    _ condition: () -> Bool,
    sourceLocation: SourceLocation = #_sourceLocation
) async {
    for _ in 0 ..< 500 {
        if condition() { return }
        try? await Task.sleep(for: .milliseconds(10))
    }
    Issue.record("Condition never held", sourceLocation: sourceLocation)
}

private final class Harness: Sendable {
    let activities = FakeActivities()
    let api = FakeAPI()
    private let suite = "CommuteActivityCoordinatorTests.\(UUID().uuidString)"

    deinit {
        UserDefaults().removePersistentDomain(forName: suite)
    }

    /// What earlier launches wrote is still on disk.
    func launch() -> CommuteActivityCoordinator {
        CommuteActivityCoordinator(system: activities.system, defaults: UserDefaults(suiteName: suite)!)
    }

    var registered: [String] {
        UserDefaults(suiteName: suite)?.stringArray(forKey: CommuteActivityCoordinator.registeredKey) ?? []
    }

    func remember(_ id: String) {
        UserDefaults(suiteName: suite)?.set(registered + [id], forKey: CommuteActivityCoordinator.registeredKey)
    }

    func run(_ operation: () async throws -> Void) async rethrows {
        try await withDependencies {
            $0.apiClient = api.client
            $0.push.currentToken = { "fcm-token" }
        } operation: {
            try await operation()
        }
    }
}

/// Plays URLSession: a request whose task is already cancelled fails at once.
private final class FakeAPI: Sendable {
    struct Call: Sendable {
        var method: String
        var path: String
        var body: Data?
        var wasCancelled: Bool
    }

    private let recorded = LockIsolated<[Call]>([])
    private let failing = LockIsolated<Set<String>>([])
    private let held = LockIsolated<[String: Gate]>([:])

    func calls(_ method: String) -> [Call] {
        recorded.value.filter { $0.method == method }
    }

    func fail(_ method: String) {
        failing.withValue { _ = $0.insert(method) }
    }

    func recover(_ method: String) {
        failing.withValue { _ = $0.remove(method) }
    }

    func hold(_ method: String) {
        held.withValue { $0[method] = Gate() }
    }

    func release(_ method: String) {
        held.withValue { $0.removeValue(forKey: method) }?.open()
    }

    var client: APIClient {
        APIClient(send: { request in
            let call = Call(method: request.method, path: request.path, body: request.body, wasCancelled: Task.isCancelled)
            self.recorded.withValue { $0.append(call) }
            if call.wasCancelled { throw URLError(.cancelled) }
            await self.held.value[request.method]?.pass()
            if self.failing.value.contains(request.method) { throw URLError(.notConnectedToInternet) }
            return Data()
        })
    }
}

private final class Gate: Sendable {
    private struct State: Sendable {
        var isOpen = false
        var waiting: [CheckedContinuation<Void, Never>] = []
    }

    private let state = LockIsolated(State())

    func pass() async {
        await withCheckedContinuation { continuation in
            let isOpen = state.withValue { state in
                if !state.isOpen { state.waiting.append(continuation) }
                return state.isOpen
            }
            if isOpen { continuation.resume() }
        }
    }

    func open() {
        let waiting = state.withValue { state in
            state.isOpen = true
            defer { state.waiting = [] }
            return state.waiting
        }
        waiting.forEach { $0.resume() }
    }
}

/// Plays ActivityKit: every activity it lists is running.
private final class FakeActivities: Sendable {
    private struct Entry: Sendable {
        var activity: CommuteLiveActivity
        var tokens: [AsyncStream<Data>.Continuation] = []
        var states: [AsyncStream<ActivityState>.Continuation] = []
    }

    private let entries = LockIsolated<[Entry]>([])
    private let requested = LockIsolated(0)
    private let observers = LockIsolated<[String: Int]>([:])

    func running(arrivalId: String? = nil) -> [CommuteLiveActivity] {
        entries.value.map(\.activity).filter { arrivalId == nil || $0.attributes.arrivalId == arrivalId }
    }

    /// Token subscriptions still open, i.e. the coordinator still observes it.
    func observerCount(of id: String) -> Int {
        observers.value[id] ?? 0
    }

    /// One an earlier launch started.
    func add(_ attributes: CommuteActivityAttributes, pushToken: Data?) -> String {
        insert(attributes, pushToken: pushToken).id
    }

    private func insert(_ attributes: CommuteActivityAttributes, pushToken: Data?) -> CommuteLiveActivity {
        let id = requested.withValue { count in
            count += 1
            return "activity-\(count)"
        }
        let content = ActivityContent(state: CommuteActivityCoordinatorTests.state, staleDate: nil)
        let activity = CommuteLiveActivity(id: id, attributes: attributes, state: .active, content: content, pushToken: pushToken)
        entries.withValue { $0.append(Entry(activity: activity)) }
        return activity
    }

    func issueToken(_ token: Data, to id: String) {
        let subscribers = entries.withValue { entries -> [AsyncStream<Data>.Continuation] in
            guard let index = entries.firstIndex(where: { $0.activity.id == id }) else { return [] }
            entries[index].activity.pushToken = token
            return entries[index].tokens
        }
        subscribers.forEach { $0.yield(token) }
    }

    /// Off the lock screen, whether the student swiped it or the app ended it.
    func swipeAway(_ id: String) {
        let subscribers = entries.withValue { entries -> [AsyncStream<ActivityState>.Continuation] in
            guard let index = entries.firstIndex(where: { $0.activity.id == id }) else { return [] }
            return entries.remove(at: index).states
        }
        subscribers.forEach { $0.yield(.dismissed) }
    }

    var system: CommuteActivitySystem {
        CommuteActivitySystem(
            isEnabled: { true },
            activities: { self.running() },
            request: { attributes, _ in self.insert(attributes, pushToken: nil) },
            end: { id in self.swipeAway(id) },
            pushTokenUpdates: { id in
                let (stream, continuation) = AsyncStream.makeStream(of: Data.self)
                self.observers.withValue { $0[id, default: 0] += 1 }
                continuation.onTermination = { _ in self.observers.withValue { $0[id, default: 0] -= 1 } }
                self.entries.withValue { entries in
                    guard let index = entries.firstIndex(where: { $0.activity.id == id }) else { return }
                    entries[index].tokens.append(continuation)
                }
                return stream
            },
            stateUpdates: { id in
                let (stream, continuation) = AsyncStream.makeStream(of: ActivityState.self)
                self.entries.withValue { entries in
                    guard let index = entries.firstIndex(where: { $0.activity.id == id }) else { return }
                    entries[index].states.append(continuation)
                }
                return stream
            }
        )
    }
}
#endif

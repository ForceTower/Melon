import ComposableArchitecture
import Foundation

/// An in-app destination requested by a Siri/Shortcuts intent, a Spotlight
/// result tap, or a `unes://` deeplink.
enum IntentRoute: Equatable, Sendable {
    case tab(AppFeature.Tab)
    case discipline(semesterId: String, disciplineId: String)
    case message(id: String)
    case material(id: String)
    case materialsDiscipline(disciplineId: String)

    /// Log label — never carries ids or names (remote log).
    var kindLabel: String {
        switch self {
        case .tab: "tab"
        case .discipline: "discipline"
        case .message: "message"
        case .material: "material"
        case .materialsDiscipline: "materialsDiscipline"
        }
    }
}

/// Hands routes from intent `perform()` calls to whichever reducer is
/// subscribed — the same fan-out shape as `PushClient`, plus a one-route
/// buffer so a cold launch (intent runs before the connected shell renders)
/// doesn't lose the ask.
@DependencyClient
struct IntentRouter: Sendable {
    var open: @Sendable (IntentRoute) -> Void
    /// Routes posted after subscription, preceded by the buffered route (if
    /// any) for the first subscriber. A delivered route is never replayed.
    var routes: @Sendable () -> AsyncStream<IntentRoute> = { .finished }
}

extension IntentRouter: DependencyKey {
    static let liveValue = IntentRouter(
        open: { route in hub.send(route) },
        routes: { hub.stream() }
    )
}

extension IntentRouter: TestDependencyKey {
    static let testValue = IntentRouter()

    static let previewValue = IntentRouter(
        open: { _ in },
        routes: { .finished }
    )
}

extension DependencyValues {
    var intentRouter: IntentRouter {
        get { self[IntentRouter.self] }
        set { self[IntentRouter.self] = newValue }
    }
}

private let hub = IntentRouteHub()

/// Internal (not private) so tests can exercise the buffer-replay semantics
/// on their own instances instead of the live value's module-wide one.
struct IntentRouteHub: Sendable {
    private struct State {
        var subscribers: [UUID: AsyncStream<IntentRoute>.Continuation] = [:]
        /// The latest route posted with nobody listening — newest wins.
        var pending: IntentRoute?
    }

    private let state = LockIsolated(State())

    func send(_ route: IntentRoute) {
        state.withValue { state in
            guard !state.subscribers.isEmpty else {
                state.pending = route
                return
            }
            for continuation in state.subscribers.values {
                continuation.yield(route)
            }
        }
    }

    func stream() -> AsyncStream<IntentRoute> {
        let (stream, continuation) = AsyncStream<IntentRoute>.makeStream()
        let id = UUID()
        state.withValue { state in
            state.subscribers[id] = continuation
            if let pending = state.pending {
                state.pending = nil
                continuation.yield(pending)
            }
        }
        continuation.onTermination = { [state] _ in
            state.withValue { $0.subscribers[id] = nil }
        }
        return stream
    }
}

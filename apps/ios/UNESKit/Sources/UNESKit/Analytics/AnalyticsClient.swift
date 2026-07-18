import ComposableArchitecture

private let log = Log.scoped("Analytics")

/// Product-analytics boundary. The package only defines the interface — the
/// concrete implementation (PostHog today) lives in the app target, where the
/// vendor SDK is linked, so call sites depend on this and switching providers
/// touches one file. Mirrors the Android `Analytics` interface so both
/// platforms emit the same event shapes.
///
/// Deliberately small (GA4-style): most product questions are answered by
/// "what screen did they see" and "what did they tap". Add a method only when
/// something genuinely isn't a screen view or a content selection.
public protocol AnalyticsSink: Sendable {
    /// A screen was shown — "screen_view".
    func screen(name: String, properties: [String: any Sendable])

    /// A tap/open on a thing (a hub, a material, a ranking entry…) —
    /// "select_content".
    func selectContent(contentType: String, itemId: String?, properties: [String: any Sendable])

    /// Persistent properties stamped on every event, anonymous and identified —
    /// e.g. the device machine_id, so analytics events line up with the OTel
    /// logs (both keyed on the same id). These do NOT survive `reset()`, so
    /// re-apply device context after a logout.
    func register(properties: [String: any Sendable])

    /// Tie subsequent events to a person instead of just the device — call on
    /// login. Without it, analytics counts devices, not users, and can't
    /// stitch a person's phone + watch together.
    func identify(userId: String, properties: [String: any Sendable])

    /// Unlink the device from the person — call on logout.
    func reset()
}

/// What features consume via `@Dependency(\.analytics)` — forwards to the
/// installed `AnalyticsSink`. Capture is fire-and-forget (the SDK queues and
/// batches off the caller), so it's safe to call anywhere, including reducers.
@DependencyClient
public struct AnalyticsClient: Sendable {
    public var screen: @Sendable (_ name: String, _ properties: [String: any Sendable]) -> Void
    public var selectContent:
        @Sendable (_ contentType: String, _ itemId: String?, _ properties: [String: any Sendable])
            -> Void
    public var register: @Sendable (_ properties: [String: any Sendable]) -> Void
    public var identify: @Sendable (_ userId: String, _ properties: [String: any Sendable]) -> Void
    public var reset: @Sendable () -> Void
}

extension AnalyticsClient {
    public func screen(_ name: String) {
        screen(name: name, properties: [:])
    }

    public func selectContent(contentType: String, itemId: String? = nil) {
        selectContent(contentType: contentType, itemId: itemId, properties: [:])
    }

    public func identify(_ userId: String) {
        identify(userId: userId, properties: [:])
    }
}

extension AnalyticsClient: DependencyKey {
    /// The real sink needs the app target's vendor SDK, so the app installs it
    /// at launch via `AnalyticsSupport.install`; this default makes a missed
    /// installation loud in the logs, not invisible.
    public static let liveValue = AnalyticsClient(
        screen: { name, _ in log.warn("sink not installed — dropped screen \(name)") },
        selectContent: { type, _, _ in log.warn("sink not installed — dropped select_content \(type)") },
        register: { _ in log.warn("sink not installed — dropped register") },
        identify: { _, _ in log.warn("sink not installed — dropped identify") },
        reset: { log.warn("sink not installed — dropped reset") }
    )
}

extension AnalyticsClient: TestDependencyKey {
    /// No-op rather than unimplemented: analytics fires from dozens of reducer
    /// paths as passive telemetry, so forcing an override in every test adds
    /// noise without assertion value. Tests that care spy on the endpoints
    /// explicitly.
    public static let testValue = AnalyticsClient(
        screen: { _, _ in },
        selectContent: { _, _, _ in },
        register: { _ in },
        identify: { _, _ in },
        reset: {}
    )
    public static let previewValue = AnalyticsClient(
        screen: { _, _ in },
        selectContent: { _, _, _ in },
        register: { _ in },
        identify: { _, _ in },
        reset: {}
    )
}

extension DependencyValues {
    public var analytics: AnalyticsClient {
        get { self[AnalyticsClient.self] }
        set { self[AnalyticsClient.self] = newValue }
    }
}

public enum AnalyticsSupport {
    /// Called once from `AppDelegate.didFinishLaunching`, before any scene
    /// task reads dependencies.
    public static func install(_ sink: some AnalyticsSink) {
        prepareDependencies {
            $0.analytics = AnalyticsClient(
                screen: { sink.screen(name: $0, properties: $1) },
                selectContent: { sink.selectContent(contentType: $0, itemId: $1, properties: $2) },
                register: { sink.register(properties: $0) },
                identify: { sink.identify(userId: $0, properties: $1) },
                reset: { sink.reset() }
            )
        }
    }
}

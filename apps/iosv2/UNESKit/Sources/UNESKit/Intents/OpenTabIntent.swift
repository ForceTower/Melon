import AppIntents
import ComposableArchitecture

private let log = Log.scoped("AppIntents")

/// The tab bar, as a Siri-visible enum. Display names reuse the tab-title
/// catalog keys so Shortcuts and the tab bar always read the same. Written
/// as bare literal keys — the metadata extractor can't evaluate the
/// generated catalog symbols and rejects explicit bundles; it resolves the
/// keys against this module's catalog itself.
public enum TabDestination: String, AppEnum {
    case home, schedule, classes, messages, me

    public static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "intent.tab.typeName")

    public static let caseDisplayRepresentations: [TabDestination: DisplayRepresentation] = [
        .home: DisplayRepresentation(title: "nav.today"),
        .schedule: DisplayRepresentation(title: "nav.schedule"),
        .classes: DisplayRepresentation(title: "nav.classes"),
        .messages: DisplayRepresentation(title: "nav.messages"),
        .me: DisplayRepresentation(title: "nav.me"),
    ]

    var appTab: AppFeature.Tab {
        switch self {
        case .home: .home
        case .schedule: .schedule
        case .classes: .classes
        case .messages: .messages
        case .me: .me
        }
    }
}

/// "Abrir mensagens no UNES" — foregrounds the app and posts the tab through
/// `IntentRouter`; `AppFeature` consumes it once the connected shell renders.
public struct OpenTabIntent: AppIntent {
    public static let title: LocalizedStringResource = "intent.openTab.title"
    public static let openAppWhenRun = true

    @Parameter(title: "intent.tab.typeName")
    public var tab: TabDestination

    public init() {}

    public func perform() async throws -> some IntentResult {
        // Signed out, nothing subscribes — a buffered route would replay
        // after a later login, so don't post at all. The app still
        // foregrounds onto onboarding, which is correct.
        guard resolvedSessionStore().current() != nil else {
            log.info("open-tab route ignored signed-out")
            return .result()
        }
        resolvedIntentRouter().open(.tab(tab.appTab))
        log.info("open-tab route posted tab=\(tab.rawValue)")
        return .result()
    }
}

/// Dependencies are resolved outside the intent because both `@Dependency`
/// placements break the App Intents toolchain: a stored property crashes the
/// metadata const-extractor, and a local wrapper inside `perform()` trips
/// its associated-type inference.
private func resolvedSessionStore() -> SessionStore {
    @Dependency(\.sessionStore) var sessionStore
    return sessionStore
}

private func resolvedIntentRouter() -> IntentRouter {
    @Dependency(\.intentRouter) var intentRouter
    return intentRouter
}

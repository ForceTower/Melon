import ComposableArchitecture
import Foundation

private let log = Log.scoped("Deeplinks")

/// `unes://` deeplinks — carried by push notifications (the backend puts the
/// URL in the FCM `data.url` key) and by OS-level scheme opens. A URL resolves
/// to an `IntentRoute` and rides the same buffered `IntentRouter` path App
/// Intents use, which already solves cold-launch ordering (routes wait until
/// the connected shell subscribes).
public enum Deeplinks {
    /// OS-level `unes://` open (`onOpenURL`).
    public static func open(_ url: URL) {
        post(url.absoluteString)
    }

    static func post(_ url: String) {
        @Dependency(\.sessionStore) var sessionStore
        @Dependency(\.intentRouter) var intentRouter

        guard let route = parse(url) else {
            log.info("deeplink unparseable -> plain open")
            return
        }
        // Signed out, nothing subscribes — a buffered route would replay
        // after a later login, so don't post at all. The app still opens
        // onto onboarding, which is correct.
        guard sessionStore.current() != nil else {
            log.info("deeplink ignored signed-out")
            return
        }
        intentRouter.open(route)
        log.info("deeplink route posted kind=\(route.kindLabel)")
    }

    /// Unknown or malformed URLs resolve to nil and the link is dropped —
    /// the worst case is a plain open, never an error. Same grammar as the
    /// Android parser: scheme and host are case-insensitive, ids are not,
    /// query and fragment are ignored.
    static func parse(_ url: String) -> IntentRoute? {
        guard let schemeEnd = url.range(of: "://"),
              url[..<schemeEnd.lowerBound].lowercased() == "unes"
        else { return nil }
        var path = url[schemeEnd.upperBound...]
        if let fragment = path.firstIndex(of: "#") { path = path[..<fragment] }
        if let query = path.firstIndex(of: "?") { path = path[..<query] }
        let segments = path.split(separator: "/").map(String.init)

        guard let host = segments.first?.lowercased() else { return nil }
        let rest = Array(segments.dropFirst())
        switch (host, rest.count) {
        case (_, 0):
            return tabRoute(host)
        case ("messages", 1):
            return .message(id: rest[0])
        case ("materials", 2) where rest[0].lowercased() == "discipline":
            return .materialsDiscipline(disciplineId: rest[1])
        case ("materials", 1):
            return .material(id: rest[0])
        default:
            return nil
        }
    }

    private static func tabRoute(_ host: String) -> IntentRoute? {
        switch host {
        case "home": .tab(.home)
        case "schedule": .tab(.schedule)
        case "classes": .tab(.classes)
        case "messages": .tab(.messages)
        case "me": .tab(.me)
        default: nil
        }
    }
}

import Foundation

/// Where a deeplink lands on the watch — a projection of `IntentRoute` onto
/// the watch's `NavigationStack` destinations.
enum WatchDeeplinkDestination: Equatable, Sendable {
    case root
    case week
    case messages
    case message(id: String)
    case discipline(id: String)
}

/// The watch's routing table as pure functions, deliberately not
/// `#if os(watchOS)`-gated: `WatchAppFeature` can't run on the macOS test
/// destination, so the table lives here where it can.
enum WatchDeeplinkResolver {
    /// The route a notification tap asks for. Grade pushes name their
    /// discipline in push `data`, not the URL — a `unes://classes` tap
    /// carrying a `disciplineCode` refines to the discipline landing.
    static func route(for data: [String: String]) -> IntentRoute? {
        guard let url = data["url"], let route = Deeplinks.parse(url) else { return nil }
        if case .tab(.classes) = route,
           let code = data["disciplineCode"]?.trimmingCharacters(in: .whitespaces),
           !code.isEmpty {
            return .classesDiscipline(code: code)
        }
        return route
    }

    /// Content routes resolve against mirrored data and wait for the store's
    /// first emission; everything else applies immediately.
    static func needsSnapshot(_ route: IntentRoute) -> Bool {
        switch route {
        case .message, .classesDiscipline: true
        case .tab, .discipline, .material, .materialsDiscipline, .reauth: false
        }
    }

    static func resolve(_ route: IntentRoute, snapshot: WatchSnapshot?) -> WatchDeeplinkDestination {
        switch route {
        case let .tab(tab):
            switch tab {
            // Hoje is the only home the watch has for classes and Eu.
            case .home, .classes, .me: .root
            case .schedule: .week
            case .messages: .messages
            }
        case let .message(id):
            snapshot?.messages.contains(where: { $0.id == id }) == true
                ? .message(id: id)
                : .messages
        case let .classesDiscipline(code):
            snapshot?.disciplines.first(where: { $0.code == code })
                .map { .discipline(id: $0.id) } ?? .root
        // No materials surface on the watch; `.discipline` is never parsed
        // from a URL and has no Spotlight/App Intents source here. Re-auth
        // needs a password field the watch doesn't have — open Hoje and let
        // the phone carry it.
        case .discipline, .material, .materialsDiscipline, .reauth: .root
        }
    }
}

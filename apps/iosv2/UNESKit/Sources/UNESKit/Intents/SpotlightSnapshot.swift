import Foundation

/// The Spotlight projection of the mirror: plain value types with every
/// display string already resolved. UNESKit computes them and diffs them;
/// the app target wraps them in `AppEntity` values at the `CSSearchableIndex`
/// boundary. Pure data — no AppIntents import, and structurally no grades:
/// nothing here is derived from grade, absence, or result rows.
struct SpotlightSnapshot: Equatable, Codable, Sendable {
    /// One per enrolled discipline of the active semester — the Turmas cards.
    var disciplines: [SpotlightDiscipline]
    /// Every mirrored message, newest first.
    var messages: [SpotlightMessage]
}

public struct SpotlightDiscipline: Equatable, Codable, Sendable, Identifiable {
    /// Entity identifier — encodes the route back into the app.
    public var id: String
    public var semesterId: String
    public var disciplineId: String
    /// "Cálculo II"
    public var title: String
    /// "MAT202"
    public var code: String
    /// Code + weekly line + room: "MAT202 · seg · qua · 10:50 · MT-14".
    public var subtitle: String
    /// Code, name, teacher, semester labels.
    public var keywords: [String]
}

public struct SpotlightMessage: Equatable, Codable, Sendable, Identifiable {
    /// Entity identifier — encodes the route back into the app.
    public var id: String
    public var messageId: String
    /// Subject; sender name when the subject is empty.
    public var title: String
    /// "Sender · date".
    public var subtitle: String
    /// Indexed verbatim as full-text content.
    public var body: String
    /// Discipline code/name on class-scoped messages.
    public var keywords: [String]
}

/// Builds and parses the opaque identifier strings that ride Spotlight
/// results back into the app — the single place that knows the format.
enum SpotlightEntityID {
    private static let disciplineKind = "discipline"
    private static let messageKind = "message"

    /// Everything but "/" and "%" passes through, so an upstream id
    /// containing the separator can't shear the format.
    private static let allowed = CharacterSet(charactersIn: "/%").inverted

    static func discipline(semesterId: String, disciplineId: String) -> String {
        [disciplineKind, escape(semesterId), escape(disciplineId)].joined(separator: "/")
    }

    static func message(id: String) -> String {
        [messageKind, escape(id)].joined(separator: "/")
    }

    static func parse(_ identifier: String) -> IntentRoute? {
        let parts = identifier.split(separator: "/", omittingEmptySubsequences: false).map(String.init)
        if let route = route(from: parts) { return route }
        // A tapped Spotlight result carries the CSSearchableItem identifier,
        // which prefixes the entity id with the entity type name
        // ("DisciplineEntity/discipline/…") — drop it and retry.
        guard parts.count > 1 else { return nil }
        return route(from: Array(parts.dropFirst()))
    }

    private static func route(from parts: [String]) -> IntentRoute? {
        switch (parts.first, parts.count) {
        case (disciplineKind, 3):
            guard let semesterId = parts[1].removingPercentEncoding,
                  let disciplineId = parts[2].removingPercentEncoding
            else { return nil }
            return .discipline(semesterId: semesterId, disciplineId: disciplineId)
        case (messageKind, 2):
            guard let id = parts[1].removingPercentEncoding else { return nil }
            return .message(id: id)
        default:
            return nil
        }
    }

    private static func escape(_ component: String) -> String {
        component.addingPercentEncoding(withAllowedCharacters: allowed) ?? component
    }
}

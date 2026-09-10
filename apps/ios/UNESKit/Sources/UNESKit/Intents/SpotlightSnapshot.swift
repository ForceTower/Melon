import Foundation

/// The Spotlight projection of the mirror: plain value types with every
/// display string already resolved. UNESKit computes them and diffs them;
/// the app target wraps them in `AppEntity` values at the `CSSearchableIndex`
/// boundary. Pure data — no AppIntents import, and no grade *values*
/// anywhere: evaluations project their name, date, and discipline linkage
/// only, never what was scored.
struct SpotlightSnapshot: Equatable, Codable, Sendable {
    /// One per enrolled discipline of the active semester — the Turmas cards.
    var disciplines: [SpotlightDiscipline]
    /// Every mirrored message, newest first.
    var messages: [SpotlightMessage]
    /// Scheduled, still-pending evaluations of the active semester, soonest
    /// first.
    var evaluations: [SpotlightEvaluation]
    /// Lectures with a subject, per enrolled discipline of the active
    /// semester, chronological.
    var lectures: [SpotlightLecture] = []
    /// One per merged weekly session of the active semester — the Horário
    /// rail's rows. Written only on iOS 27 (calendar-schema entities).
    var sessions: [SpotlightSession] = []
    /// The student's own calendar entries, earliest first. Written only on
    /// iOS 27 (calendar-schema entities).
    var personalEvents: [SpotlightPersonalEvent] = []
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
    public var teacher: String? = nil
    /// First room the weekly pattern resolves.
    public var room: String? = nil
    /// Weekday labels + earliest start: "seg · qua · 10:50".
    public var schedule: String? = nil
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
    /// Sender name; "UNES" when upstream sent none.
    public var sender: String = "UNES"
    public var receivedAt: Date? = nil
    /// Upstream's read flag — the local overlay stays out of the projection.
    public var isRead: Bool = false
    /// The origin the inbox groups by, as a stable key: "class/<code>",
    /// "university", "secretariat", "app", or "sender/<name>".
    public var originId: String = "university"
    /// The origin's display name.
    public var originName: String = "UNES"
}

public struct SpotlightEvaluation: Equatable, Codable, Sendable, Identifiable {
    /// Entity identifier — encodes the route back into the app.
    public var id: String
    public var semesterId: String
    public var disciplineId: String
    /// The deduplicated grade-row key (`platformId ?? id` — the detail
    /// screen's grade id).
    public var gradeId: String
    /// "Prova 1 — Cálculo II"
    public var title: String
    /// Localized date line: "qui., 15 de ago."
    public var subtitle: String
    /// yyyy-MM-dd — sorts chronologically.
    public var dateStamp: String
    /// Evaluation name(s), discipline code and name.
    public var keywords: [String]
}

public struct SpotlightLecture: Equatable, Codable, Sendable, Identifiable {
    /// Entity identifier — encodes the route back into the app (the
    /// discipline detail, where lectures live).
    public var id: String
    public var semesterId: String
    public var disciplineId: String
    public var lectureId: String
    /// The posted subject: "Integrais duplas".
    public var title: String
    /// Localized date + discipline: "qui., 12 de set. · Cálculo II".
    public var subtitle: String
    /// yyyy-MM-dd; nil when the lecture isn't scheduled yet.
    public var dateStamp: String?
    /// Subject, discipline code and name.
    public var keywords: [String]
}

/// One merged weekly session of an enrolled class — the calendar-shaped
/// view of the schedule. Times are minutes from midnight in the campus day;
/// the app target turns them into recurring calendar events.
public struct SpotlightSession: Equatable, Codable, Sendable, Identifiable {
    public var id: String
    public var semesterId: String
    public var disciplineId: String
    public var classId: String
    /// "Cálculo II"
    public var title: String
    /// "MAT202"
    public var code: String
    /// 0 = Sunday, as upstream.
    public var day: Int
    public var startMinute: Int
    public var endMinute: Int?
    public var room: String?
    public var teacher: String?
    /// yyyy-MM-dd — the recurrence window.
    public var semesterStart: String
    public var semesterEnd: String
}

public struct SpotlightPersonalEvent: Equatable, Codable, Sendable, Identifiable {
    public var id: String
    public var eventId: String
    public var title: String
    /// yyyy-MM-dd.
    public var start: String
    /// yyyy-MM-dd of the last day; nil for single-day entries.
    public var end: String?
    public var notes: String
    /// `PersonalEvent.Category` raw value.
    public var category: String
    public var disciplineName: String?
    public var disciplineCode: String?
}

/// The calendar-shaped index kinds, as the app target's event entity
/// needs to tell them apart.
public enum SpotlightEventKind: Equatable, Sendable {
    case session, evaluation, personalEvent
}

/// Builds and parses the opaque identifier strings that ride Spotlight
/// results back into the app — the single place that knows the format.
enum SpotlightEntityID {
    private static let disciplineKind = "discipline"
    private static let messageKind = "message"
    private static let evaluationKind = "evaluation"
    private static let lectureKind = "lecture"
    private static let sessionKind = "session"
    private static let personalEventKind = "personalEvent"

    /// Everything but "/" and "%" passes through, so an upstream id
    /// containing the separator can't shear the format.
    private static let allowed = CharacterSet(charactersIn: "/%").inverted

    static func discipline(semesterId: String, disciplineId: String) -> String {
        [disciplineKind, escape(semesterId), escape(disciplineId)].joined(separator: "/")
    }

    static func message(id: String) -> String {
        [messageKind, escape(id)].joined(separator: "/")
    }

    static func evaluation(semesterId: String, disciplineId: String, gradeId: String) -> String {
        [evaluationKind, escape(semesterId), escape(disciplineId), escape(gradeId)].joined(separator: "/")
    }

    static func lecture(semesterId: String, disciplineId: String, lectureId: String) -> String {
        [lectureKind, escape(semesterId), escape(disciplineId), escape(lectureId)].joined(separator: "/")
    }

    static func session(semesterId: String, disciplineId: String, classId: String, day: Int, startMinute: Int) -> String {
        [sessionKind, escape(semesterId), escape(disciplineId), escape(classId), String(day), String(startMinute)]
            .joined(separator: "/")
    }

    static func personalEvent(id: String) -> String {
        [personalEventKind, escape(id)].joined(separator: "/")
    }

    /// Which calendar-shaped kind an identifier names — the app target's
    /// single calendar event entity resolves ids per kind.
    static func eventKind(of identifier: String) -> SpotlightEventKind? {
        switch identifier.split(separator: "/").first.map(String.init) {
        case sessionKind: .session
        case evaluationKind: .evaluation
        case personalEventKind: .personalEvent
        default: nil
        }
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
        case (evaluationKind, 4), (lectureKind, 4), (sessionKind, 6):
            // Evaluations, lectures, and sessions live on the discipline
            // detail screen — the tap resolves to the discipline route; the
            // trailing components only keep the index identifier unique.
            guard let semesterId = parts[1].removingPercentEncoding,
                  let disciplineId = parts[2].removingPercentEncoding
            else { return nil }
            return .discipline(semesterId: semesterId, disciplineId: disciplineId)
        case (personalEventKind, 2):
            return .calendar
        default:
            return nil
        }
    }

    private static func escape(_ component: String) -> String {
        component.addingPercentEncoding(withAllowedCharacters: allowed) ?? component
    }
}

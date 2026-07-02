import Foundation

/// Wire shape of `GET api/me/events` (subset — decoding ignores the rest).
struct EventListDTO: Decodable {
    let events: [EventDTO]
}

struct EventDTO: Decodable {
    let id: String
    let description: String
    /// yyyy-MM-dd.
    let start: String
    let end: String?
    let fixed: Bool
    let closed: Bool
    let scope: String
    let origin: String
}

extension EventDTO {
    var domain: AcademicEvent {
        AcademicEvent(
            id: id,
            summary: description,
            start: start,
            end: end,
            fixed: fixed,
            closed: closed,
            scope: AcademicEvent.Scope(rawValue: scope) ?? .unknown,
            origin: AcademicEvent.Origin(rawValue: origin) ?? .unknown
        )
    }
}

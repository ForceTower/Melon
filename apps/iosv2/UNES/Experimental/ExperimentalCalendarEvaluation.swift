#if UNES_IOS27_EXPERIMENT
import AppIntents
import UNESKit

/// §3.8(a) — calendar **entity-only** conformance.
///
/// Finding while building (27.0 SDK, 27A5209h): the spec's premise shifted.
/// `@AssistantEntity(schema:)` is the old Assistant-schemas mechanism and
/// its Entity namespace has NO calendar member. iOS 27 introduces a new
/// `AppSchema` system — `@AppEntity(schema:)` / `@AppIntent(schema:)` — and
/// the calendar domain exposes entity schemas directly
/// (`.calendar.attendee` / `.calendar.calendar` / `.calendar.event`).
///
/// E2 questions, updated:
/// 1. Does `@AppEntity(schema: .calendar.event)` build without the domain's
///    write intents (create/delete/update)?
/// 2. Do the required fields map from what we have (title, start date — no
///    invented data)?
/// 3. On the device, does Siri AI answer "when is my test from Cálculo"
///    better than with the plain `IndexedEntity` from §3.5?
@available(iOS 27, *)
@AppEntity(schema: .calendar.event)
struct ExperimentalEvaluationEvent {
    struct EventQuery: EntityStringQuery {
        func entities(for identifiers: [String]) async throws -> [ExperimentalEvaluationEvent] {
            await SpotlightSupport.evaluations(for: identifiers)
                .compactMap { ExperimentalEvaluationEvent(projection: $0) }
        }

        func entities(matching string: String) async throws -> [ExperimentalEvaluationEvent] {
            try await suggestedEntities().filter { $0.title.localizedStandardContains(string) }
        }

        func suggestedEntities() async throws -> [ExperimentalEvaluationEvent] {
            await SpotlightSupport.suggestedEvaluations()
                .compactMap { ExperimentalEvaluationEvent(projection: $0) }
        }
    }

    static let defaultQuery = EventQuery()

    let id: String
    var title: String
    var startDate: Date
    var endDate: Date
    var isAllDay: Bool
    // Types below harvested from the metadata processor's own errors —
    // that error output IS the E2 field table.
    var location: ExperimentalEventLocation?
    var alarms: [ExperimentalEventAlarm]
    var attendees: [ExperimentalAttendeeEntity]
    var calendar: ExperimentalCalendarEntity
    var note: String?
    var organizers: [IntentPerson]
    var recurrence: Calendar.RecurrenceRule?
    var status: ExperimentalEventStatus?
    var travelTime: Duration?
    var virtualLocation: URL?

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(title)",
            subtitle: "\(startDate.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated)))"
        )
    }
}

/// Stub conformers: `calendar` and `attendees` are required, non-optional
/// schema-entity properties, so the types must exist. The calendar stub is
/// honest data ("UNES" as the academic calendar); attendees stay empty.
@available(iOS 27, *)
@AppEntity(schema: .calendar.attendee)
struct ExperimentalAttendeeEntity {
    struct AttendeeQuery: EntityQuery {
        func entities(for identifiers: [String]) async throws -> [ExperimentalAttendeeEntity] { [] }
        func suggestedEntities() async throws -> [ExperimentalAttendeeEntity] { [] }
    }

    static let defaultQuery = AttendeeQuery()

    let id: String
    var person: IntentPerson
    var isAttendanceOptional: Bool
    var status: ExperimentalAttendeeStatus?
    var type: ExperimentalAttendeeType?

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(id)")
    }
}

@available(iOS 27, *)
@AppEnum(schema: .calendar.attendeeStatus)
enum ExperimentalAttendeeStatus: String {
    case accepted
    case declined
    case tentative
    case pending

    static let caseDisplayRepresentations: [ExperimentalAttendeeStatus: DisplayRepresentation] = [
        .accepted: "Accepted", .declined: "Declined", .tentative: "Tentative", .pending: "Pending",
    ]
}

@available(iOS 27, *)
@AppEnum(schema: .calendar.attendeeType)
enum ExperimentalAttendeeType: String {
    case person
    case room
    case resource

    static let caseDisplayRepresentations: [ExperimentalAttendeeType: DisplayRepresentation] = [
        .person: "Person", .room: "Room", .resource: "Resource",
    ]
}

/// The two union-typed properties, per the processor:
/// `EventLocationCases:(SystemEntity<GeoToolbox.PlaceDescriptorEntity> | String)`
/// and `EventAlarmCases:(Codable<Swift.Duration> | Date)`.
@available(iOS 27, *)
@UnionValue
enum ExperimentalEventLocation {
    case string(String)
}

@available(iOS 27, *)
@UnionValue
enum ExperimentalEventAlarm {
    case duration(Duration)
    case date(Date)
}

@available(iOS 27, *)
@AppEntity(schema: .calendar.calendar)
struct ExperimentalCalendarEntity {
    struct CalendarQuery: EntityQuery {
        func entities(for identifiers: [String]) async throws -> [ExperimentalCalendarEntity] {
            identifiers.map { ExperimentalCalendarEntity(id: $0, title: "UNES") }
        }

        func suggestedEntities() async throws -> [ExperimentalCalendarEntity] {
            [ExperimentalCalendarEntity(id: "unes", title: "UNES")]
        }
    }

    static let defaultQuery = CalendarQuery()

    let id: String
    var title: String

    // The @AppEntity macro wraps members in @Property, which removes the
    // memberwise init.
    init(id: String, title: String) {
        self.id = id
        self.title = title
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(title)")
    }
}

@available(iOS 27, *)
@AppEnum(schema: .calendar.eventStatus)
enum ExperimentalEventStatus: String {
    case confirmed
    case tentative
    case cancelled

    static let caseDisplayRepresentations: [ExperimentalEventStatus: DisplayRepresentation] = [
        .confirmed: "Confirmed", .tentative: "Tentative", .cancelled: "Cancelled",
    ]
}

@available(iOS 27, *)
extension ExperimentalEvaluationEvent {
    nonisolated init?(projection: SpotlightEvaluation) {
        // yyyy-MM-dd → start of that day; evaluations carry no time, so the
        // event is all-day by construction.
        let parts = projection.dateStamp.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3,
              let day = Calendar.current.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
        else { return nil }
        id = projection.id
        title = projection.title
        startDate = day
        endDate = day
        isAllDay = true
        location = nil
        alarms = []
        attendees = []
        calendar = ExperimentalCalendarEntity(id: "unes", title: "UNES")
        note = nil
        organizers = []
        recurrence = nil
        status = nil
        travelTime = nil
        virtualLocation = nil
    }
}
#endif

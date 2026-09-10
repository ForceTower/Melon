import AppIntents
import CoreSpotlight
import Foundation
import GeoToolbox
import LinkPresentation
import UNESKit

/// The Apple Intelligence schema entities — what lets Siri AI understand
/// *what kind of thing* a class session, an evaluation, a personal entry, or
/// an institutional message is, not just its title. All iOS 27: the
/// `calendar` domain and entity-only `messages` adoption exist only there.
/// Below 27 the plain entities in `UNESEntities.swift` keep serving
/// Spotlight and Shortcuts.
///
/// Shapes come from the toolchain's schema catalog (research §9): every
/// property the schema lists must exist with its exact type, the
/// `@AppEntity` macro wraps members in `@Property` (no memberwise inits),
/// and the validator allows **one type per schema per app** — hence a
/// single calendar event entity for the three calendar-shaped kinds.

// MARK: - Calendar domain

/// The one calendar every academic event belongs to.
@available(iOS 27, *)
@AppEntity(schema: .calendar.calendar)
struct UNESCalendarEntity {
    struct CalendarQuery: EntityQuery {
        func entities(for identifiers: [String]) async throws -> [UNESCalendarEntity] {
            identifiers.map { UNESCalendarEntity(id: $0) }
        }

        func suggestedEntities() async throws -> [UNESCalendarEntity] {
            [UNESCalendarEntity()]
        }
    }

    static let defaultQuery = CalendarQuery()

    let id: String
    var title: String

    init(id: String = "unes") {
        self.id = id
        title = "UNES"
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(title)")
    }
}

/// Required by the event schema; academic events carry no attendees.
@available(iOS 27, *)
@AppEntity(schema: .calendar.attendee)
struct AcademicAttendeeEntity: TransientAppEntity {
    var person: IntentPerson
    var isAttendanceOptional: Bool
    var status: AcademicAttendeeStatus?
    var type: AcademicAttendeeType?

    init() {
        person = IntentPerson(handle: .init(applicationDefined: "unes"))
        isAttendanceOptional = false
        status = nil
        type = nil
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "entity.attendee.title")
    }
}

@available(iOS 27, *)
@AppEnum(schema: .calendar.attendeeStatus)
enum AcademicAttendeeStatus: String {
    case accepted, declined, tentative

    static let caseDisplayRepresentations: [AcademicAttendeeStatus: DisplayRepresentation] = [
        .accepted: "enum.attendeeStatus.accepted",
        .declined: "enum.attendeeStatus.declined",
        .tentative: "enum.attendeeStatus.tentative",
    ]
}

@available(iOS 27, *)
@AppEnum(schema: .calendar.attendeeType)
enum AcademicAttendeeType: String {
    case person, room, resource

    static let caseDisplayRepresentations: [AcademicAttendeeType: DisplayRepresentation] = [
        .person: "enum.attendeeType.person",
        .room: "enum.attendeeType.room",
        .resource: "enum.attendeeType.resource",
    ]
}

@available(iOS 27, *)
@AppEnum(schema: .calendar.eventStatus)
enum AcademicEventStatus: String {
    case confirmed, tentative, cancelled

    static let caseDisplayRepresentations: [AcademicEventStatus: DisplayRepresentation] = [
        .confirmed: "enum.eventStatus.confirmed",
        .tentative: "enum.eventStatus.tentative",
        .cancelled: "enum.eventStatus.cancelled",
    ]
}

/// The schema's location union (`place | address`); rooms are plain text.
@available(iOS 27, *)
@UnionValue
enum AcademicEventLocation {
    case address(String)
}

@available(iOS 27, *)
@UnionValue
enum AcademicEventAlarm {
    case duration(Duration)
    case date(Date)
}

/// Every calendar-shaped thing UNES knows, as one event entity: a weekly
/// class session (recurring, room as location, teacher as organizer), a
/// scheduled evaluation (all-day), or the student's own entry (all-day).
/// The identifier's kind tells them apart. All of them stay visible in
/// Spotlight: on the iOS 27.0 RC a `hideInSpotlight` entity also vanished
/// from Siri AI's semantic index (E6), so sessions earn their rows with a
/// weekday · time · room subtitle next to the discipline's own row.
@available(iOS 27, *)
@AppEntity(schema: .calendar.event)
struct AcademicEventEntity: IndexedEntity {
    struct EventQuery: EntityQuery {
        func entities(for identifiers: [String]) async throws -> [AcademicEventEntity] {
            func ids(_ kind: SpotlightEventKind) -> [String] {
                identifiers.filter { SpotlightSupport.eventKind(of: $0) == kind }
            }
            let sessions = await SpotlightSupport.sessions(for: ids(.session)).compactMap(AcademicEventEntity.init)
            let evaluations = await SpotlightSupport.evaluations(for: ids(.evaluation)).compactMap(AcademicEventEntity.init)
            let personal = await SpotlightSupport.personalEvents(for: ids(.personalEvent)).compactMap(AcademicEventEntity.init)
            return sessions + evaluations + personal
        }

        /// Evaluations soonest first, then the weekly sessions, then the
        /// student's own entries.
        func suggestedEntities() async throws -> [AcademicEventEntity] {
            let evaluations = await SpotlightSupport.suggestedEvaluations().compactMap(AcademicEventEntity.init)
            let sessions = await SpotlightSupport.suggestedSessions().compactMap(AcademicEventEntity.init)
            let personal = await SpotlightSupport.suggestedPersonalEvents().compactMap(AcademicEventEntity.init)
            return evaluations + sessions + personal
        }
    }

    static let defaultQuery = EventQuery()

    let id: String
    var title: String
    var startDate: Date
    var endDate: Date
    var isAllDay: Bool
    var location: AcademicEventLocation?
    var alarms: [AcademicEventAlarm]
    var attendees: [AcademicAttendeeEntity]
    var calendar: UNESCalendarEntity
    var note: String?
    var organizers: [IntentPerson]
    var recurrence: Calendar.RecurrenceRule?
    var status: AcademicEventStatus?
    var travelTime: Duration?
    var virtualLocation: URL?

    private let subtitle: String
    private let keywords: [String]
    private let alternateNames: [String]

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(title)", subtitle: "\(subtitle)")
    }

    var attributeSet: CSSearchableItemAttributeSet {
        let attributes = defaultAttributeSet
        attributes.displayName = title
        attributes.contentDescription = subtitle
        attributes.keywords = keywords
        attributes.alternateNames = alternateNames
        return attributes
    }

    /// A weekly session. Nil when the semester window can't be parsed — no
    /// event without a real first occurrence.
    init?(projection: SpotlightSession) {
        let calendar = Calendar.current
        guard let semesterStart = AcademicDates.day(projection.semesterStart, calendar: calendar),
              let semesterEnd = AcademicDates.day(projection.semesterEnd, calendar: calendar),
              let weekday = AcademicDates.weekday(projection.day)
        else { return nil }
        // First occurrence of the session's weekday on or after the
        // semester's first day.
        let startWeekday = calendar.component(.weekday, from: semesterStart) - 1
        let offset = (projection.day - startWeekday + 7) % 7
        guard let firstDay = calendar.date(byAdding: .day, value: offset, to: semesterStart),
              let start = calendar.date(byAdding: .minute, value: projection.startMinute, to: firstDay),
              let end = calendar.date(
                  byAdding: .minute, value: projection.endMinute ?? projection.startMinute + 100, to: firstDay
              ),
              let windowEnd = calendar.date(byAdding: .day, value: 1, to: semesterEnd)
        else { return nil }

        // Plain stored fields first: the macro-wrapped properties assign
        // through their wrapper, which needs the rest of `self` in place.
        id = projection.id
        subtitle = [
            start.formatted(.dateTime.weekday(.wide).hour().minute()),
            projection.room,
        ].compactMap { $0 }.joined(separator: " · ")
        keywords = [projection.code, projection.title, projection.teacher].compactMap { $0 }
        alternateNames = [projection.code, projection.teacher].compactMap { $0 }
        title = projection.title
        startDate = start
        endDate = end
        isAllDay = false
        location = projection.room.map { .address($0) }
        alarms = []
        attendees = []
        self.calendar = UNESCalendarEntity()
        note = projection.code
        organizers = projection.teacher.map { [AcademicPeople.teacher(named: $0)] } ?? []
        recurrence = .weekly(calendar: calendar, end: .afterDate(windowEnd), weekdays: [.every(weekday)])
        status = .confirmed
        travelTime = nil
        virtualLocation = nil
    }

    /// A scheduled, still-pending evaluation — the iOS 27 shape of
    /// `EvaluationEntity`, visible in Spotlight like the plain entity.
    init?(projection: SpotlightEvaluation) {
        guard let day = AcademicDates.day(projection.dateStamp, calendar: .current) else { return nil }
        id = projection.id
        subtitle = projection.subtitle
        keywords = projection.keywords
        alternateNames = []
        title = projection.title
        startDate = day
        endDate = day
        isAllDay = true
        location = nil
        alarms = []
        attendees = []
        calendar = UNESCalendarEntity()
        note = nil
        organizers = []
        recurrence = nil
        status = .confirmed
        travelTime = nil
        virtualLocation = nil
    }

    /// The student's own calendar entry; taps land on Calendário.
    init?(projection: SpotlightPersonalEvent) {
        let calendar = Calendar.current
        guard let start = AcademicDates.day(projection.start, calendar: calendar) else { return nil }
        id = projection.id
        subtitle = [
            start.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated)),
            projection.disciplineName,
        ].compactMap { $0 }.joined(separator: " · ")
        keywords = [projection.disciplineCode, projection.disciplineName].compactMap { $0 }
        alternateNames = []
        title = projection.title
        startDate = start
        endDate = projection.end.flatMap { AcademicDates.day($0, calendar: calendar) } ?? start
        isAllDay = true
        location = nil
        alarms = []
        attendees = []
        self.calendar = UNESCalendarEntity()
        note = [projection.notes, projection.disciplineName].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: "\n")
        organizers = []
        recurrence = nil
        status = .confirmed
        travelTime = nil
        virtualLocation = nil
    }
}

// MARK: - Messages domain

/// An institutional message as a messages-schema entity — the shape WWDC26
/// shows for "messages about X" semantic questions. On iOS 27 this is the
/// message's only index entry: body words never matched lexically on any
/// path (E3), and a hidden twin beside a classic item stays out of Siri
/// AI's index (E6), so the visible schema entity carries the Spotlight row
/// and the tap-through as well.
@available(iOS 27, *)
@AppEntity(schema: .messages.message)
struct MessageEventEntity: IndexedEntity {
    struct MessageQuery: EntityQuery {
        func entities(for identifiers: [String]) async throws -> [MessageEventEntity] {
            await SpotlightSupport.messages(for: identifiers).map(MessageEventEntity.init)
        }

        func suggestedEntities() async throws -> [MessageEventEntity] {
            await SpotlightSupport.suggestedMessages().map(MessageEventEntity.init)
        }
    }

    static let defaultQuery = MessageQuery()

    let id: String
    var messageType: MessageKind
    var author: MessageSenderEntity
    var isRead: Bool
    var attributes: Set<MessageAttributeKind>
    var conversation: MessageOriginEntity
    var date: Date
    var subject: AttributedString?
    @Property(indexingKey: \.textContent)
    var body: AttributedString?
    var attachments: [IntentFile]
    var audioMessage: IntentFile?
    var customAttachments: [MessageAttachmentEntity]
    var locations: [PlaceDescriptor]
    var links: [LinkMetadata]
    var messageEffect: MessageEffectKind?
    var reaction: MessageReadReaction?
    var referencedMessage: MessageEventEntity?
    var notificationIdentifier: String?

    private let subtitle: String
    private let keywords: [String]

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(String((subject ?? AttributedString("")).characters))",
            subtitle: "\(subtitle)"
        )
    }

    var attributeSet: CSSearchableItemAttributeSet {
        let attributes = defaultAttributeSet
        attributes.displayName = String((subject ?? AttributedString("")).characters)
        attributes.contentDescription = subtitle
        attributes.keywords = keywords
        attributes.domainIdentifier = SpotlightDomain.message
        return attributes
    }

    init(projection: SpotlightMessage) {
        id = projection.id
        subtitle = projection.subtitle
        keywords = projection.keywords
        messageType = .unspecified
        author = MessageSenderEntity(name: projection.sender)
        isRead = projection.isRead
        attributes = []
        conversation = MessageOriginEntity(id: projection.originId, displayName: projection.originName)
        date = projection.receivedAt ?? .distantPast
        subject = AttributedString(projection.title)
        body = AttributedString(projection.body)
        attachments = []
        audioMessage = nil
        customAttachments = []
        locations = []
        links = []
        messageEffect = nil
        reaction = nil
        referencedMessage = nil
        notificationIdentifier = nil
    }
}

@available(iOS 27, *)
@AppEntity(schema: .messages.messagePerson)
struct MessageSenderEntity: TransientAppEntity {
    var person: IntentPerson

    init() {
        person = IntentPerson(handle: .init(applicationDefined: "UNES"))
    }

    init(name: String) {
        person = AcademicPeople.teacher(named: name)
    }

    var displayName: String {
        person.handle?.applicationDefinedValue ?? "UNES"
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(displayName)")
    }
}

/// Where a message came from — the discipline, the university, the
/// secretariat, or the sender — standing in for the schema's conversation.
@available(iOS 27, *)
@AppEntity(schema: .messages.conversation)
struct MessageOriginEntity {
    struct OriginQuery: EntityQuery {
        func entities(for identifiers: [String]) async throws -> [MessageOriginEntity] {
            identifiers.map { MessageOriginEntity(id: $0, displayName: $0.split(separator: "/").last.map(String.init) ?? $0) }
        }

        func suggestedEntities() async throws -> [MessageOriginEntity] { [] }
    }

    static let defaultQuery = OriginQuery()

    let id: String
    var recipients: [MessageSenderEntity]
    var displayName: String
    var previewText: AttributedString
    var conversationName: String?
    var isRead: Bool
    var attributes: Set<MessageOriginAttribute>
    var dateLastActive: Date?

    init(id: String, displayName: String) {
        self.id = id
        recipients = []
        self.displayName = displayName
        previewText = AttributedString("")
        conversationName = nil
        isRead = true
        attributes = []
        dateLastActive = nil
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(displayName)")
    }
}

@available(iOS 27, *)
@AppEntity(schema: .messages.customAttachment)
struct MessageAttachmentEntity: TransientAppEntity {
    var sourceName: AttributedString?
    var description: AttributedString?

    init() {
        sourceName = nil
        description = nil
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "entity.attachment.title")
    }
}

/// The catalog requires the single `unspecified` case; the other message
/// enums are free-form and carry one honest case each.
@available(iOS 27, *)
@AppEnum(schema: .messages.messageType)
enum MessageKind: String {
    case unspecified

    static let caseDisplayRepresentations: [MessageKind: DisplayRepresentation] = [
        .unspecified: "enum.messageType.unspecified",
    ]
}

@available(iOS 27, *)
@AppEnum(schema: .messages.messageAttribute)
enum MessageAttributeKind: String {
    case institutional

    static let caseDisplayRepresentations: [MessageAttributeKind: DisplayRepresentation] = [
        .institutional: "enum.messageAttribute.institutional",
    ]
}

@available(iOS 27, *)
@AppEnum(schema: .messages.conversationAttribute)
enum MessageOriginAttribute: String {
    case institutional

    static let caseDisplayRepresentations: [MessageOriginAttribute: DisplayRepresentation] = [
        .institutional: "enum.conversationAttribute.institutional",
    ]
}

@available(iOS 27, *)
@AppEnum(schema: .messages.messageEffect)
enum MessageEffectKind: String {
    case none

    static let caseDisplayRepresentations: [MessageEffectKind: DisplayRepresentation] = [
        .none: "enum.messageEffect.none",
    ]
}

@available(iOS 27, *)
@AppEnum(schema: .messages.customReaction)
enum MessageTapback: String {
    case like

    static let caseDisplayRepresentations: [MessageTapback: DisplayRepresentation] = [
        .like: "enum.customReaction.like",
    ]
}

@available(iOS 27, *)
@UnionValue
enum MessageReadReaction {
    case customReaction(MessageTapback)
    case attributedString(AttributedString)
}

// MARK: - Helpers

/// yyyy-MM-dd stamps and weekday indices as the projections carry them.
nonisolated enum AcademicDates {
    static func day(_ stamp: String, calendar: Calendar) -> Date? {
        let parts = stamp.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }

    /// 0 = Sunday, as upstream, to the locale weekday the recurrence rule wants.
    static func weekday(_ day: Int) -> Locale.Weekday? {
        let weekdays: [Locale.Weekday] = [.sunday, .monday, .tuesday, .wednesday, .thursday, .friday, .saturday]
        return weekdays.indices.contains(day) ? weekdays[day] : nil
    }
}

nonisolated enum AcademicPeople {
    /// A professor as a person Siri can name — an application-defined
    /// handle, never a contact lookup.
    static func teacher(named name: String) -> IntentPerson {
        var person = IntentPerson(handle: .init(applicationDefined: name))
        person.name = .displayName(name)
        return person
    }
}

extension IntentPerson.Handle {
    nonisolated fileprivate var applicationDefinedValue: String? {
        if case let .applicationDefined(value) = value { return value }
        return nil
    }
}

import AppIntents
import CoreSpotlight
import UNESKit

/// Spotlight/Shortcuts-visible entities — thin shells over UNESKit's
/// Spotlight projections. They live in the app target because the metadata
/// extractor reads their static display strings from this target's catalog
/// (bare literal keys only); instance strings are projected data and resolve
/// at runtime.

nonisolated enum SpotlightDomain {
    static let discipline = "unes.discipline"
    static let message = "unes.message"
    static let evaluation = "unes.evaluation"
    static let lecture = "unes.lecture"
}

struct DisciplineEntity: AppEntity, IndexedEntity {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "entity.discipline.typeName")
    static let defaultQuery = DisciplineEntityQuery()

    let projection: SpotlightDiscipline

    // Typed properties: what Shortcuts and Siri can read off a discipline
    // beyond its title — the answer to "who teaches / where / when".
    @Property(title: "entity.discipline.property.code")
    var code: String
    @Property(title: "entity.discipline.property.teacher")
    var teacher: String?
    @Property(title: "entity.discipline.property.room")
    var room: String?
    @Property(title: "entity.discipline.property.schedule")
    var schedule: String?

    init(projection: SpotlightDiscipline) {
        self.projection = projection
        code = projection.code
        teacher = projection.teacher
        room = projection.room
        schedule = projection.schedule
    }

    var id: String { projection.id }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(projection.title)", subtitle: "\(projection.subtitle)")
    }

    var attributeSet: CSSearchableItemAttributeSet {
        let attributes = CSSearchableItemAttributeSet(contentType: .item)
        attributes.displayName = projection.title
        // Post-iOS 17 Spotlight lexically matches display and alternate
        // names only — keywords never match. The alternate names are what
        // make "EXA807" and the professor's name find the discipline.
        attributes.alternateNames = [projection.code] + (projection.teacher.map { [$0] } ?? [])
        attributes.title = projection.title
        attributes.contentDescription = projection.subtitle
        attributes.keywords = projection.keywords
        attributes.domainIdentifier = SpotlightDomain.discipline
        return attributes
    }
}

struct DisciplineEntityQuery: EntityStringQuery {
    func entities(for identifiers: [String]) async throws -> [DisciplineEntity] {
        await SpotlightSupport.disciplines(for: identifiers).map(DisciplineEntity.init)
    }

    /// The active semester's disciplines, Turmas order — the Shortcuts picker.
    func suggestedEntities() async throws -> [DisciplineEntity] {
        await SpotlightSupport.suggestedDisciplines().map(DisciplineEntity.init)
    }

    func entities(matching string: String) async throws -> [DisciplineEntity] {
        await SpotlightSupport.disciplines(matching: string).map(DisciplineEntity.init)
    }
}

struct MessageEntity: AppEntity, IndexedEntity {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "entity.message.typeName")
    static let defaultQuery = MessageEntityQuery()

    let projection: SpotlightMessage

    var id: String { projection.id }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(projection.title)", subtitle: "\(projection.subtitle)")
    }

    var attributeSet: CSSearchableItemAttributeSet {
        // Not .message: Spotlight only full-text-indexes `textContent` on
        // text-conforming content types, and public.message isn't one —
        // bodies were unsearchable on device under it.
        let attributes = CSSearchableItemAttributeSet(contentType: .plainText)
        attributes.displayName = projection.title
        attributes.title = projection.title
        attributes.contentDescription = projection.subtitle
        // The body indexes verbatim: the index is device-local, encrypted,
        // and gated by unlock — the same posture Mail takes.
        attributes.textContent = projection.body
        attributes.keywords = projection.keywords
        attributes.domainIdentifier = SpotlightDomain.message
        return attributes
    }
}

/// No string query: nobody picks a message by typing its subject into
/// Shortcuts — Spotlight full-text search covers finding.
struct MessageEntityQuery: EntityQuery {
    func entities(for identifiers: [String]) async throws -> [MessageEntity] {
        await SpotlightSupport.messages(for: identifiers).map(MessageEntity.init)
    }

    func suggestedEntities() async throws -> [MessageEntity] {
        await SpotlightSupport.suggestedMessages().map(MessageEntity.init)
    }
}

/// A scheduled, still-pending evaluation — the calendar-shaped data. No
/// body text, so the entity path suffices (unlike messages), and no grade
/// values anywhere. On iOS 27 the indexer writes `EvaluationEventEntity`
/// (the calendar schema) instead.
struct EvaluationEntity: AppEntity, IndexedEntity {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "entity.evaluation.typeName")
    static let defaultQuery = EvaluationEntityQuery()

    let projection: SpotlightEvaluation

    var id: String { projection.id }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(projection.title)", subtitle: "\(projection.subtitle)")
    }

    var attributeSet: CSSearchableItemAttributeSet {
        let attributes = CSSearchableItemAttributeSet(contentType: .item)
        attributes.displayName = projection.title
        attributes.title = projection.title
        attributes.contentDescription = projection.subtitle
        attributes.keywords = projection.keywords
        attributes.domainIdentifier = SpotlightDomain.evaluation
        return attributes
    }
}

/// No string query: nobody types an evaluation name into Shortcuts —
/// Spotlight matches the display name.
struct EvaluationEntityQuery: EntityQuery {
    func entities(for identifiers: [String]) async throws -> [EvaluationEntity] {
        await SpotlightSupport.evaluations(for: identifiers).map(EvaluationEntity.init)
    }

    /// Soonest first.
    func suggestedEntities() async throws -> [EvaluationEntity] {
        await SpotlightSupport.suggestedEvaluations().map(EvaluationEntity.init)
    }
}

/// A lecture with a posted subject — the class content students search
/// for before a test. Taps land on the discipline detail.
struct LectureEntity: AppEntity, IndexedEntity {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "entity.lecture.typeName")
    static let defaultQuery = LectureEntityQuery()

    let projection: SpotlightLecture

    var id: String { projection.id }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(projection.title)", subtitle: "\(projection.subtitle)")
    }

    var attributeSet: CSSearchableItemAttributeSet {
        let attributes = CSSearchableItemAttributeSet(contentType: .item)
        attributes.displayName = projection.title
        attributes.title = projection.title
        attributes.contentDescription = projection.subtitle
        attributes.keywords = projection.keywords
        attributes.domainIdentifier = SpotlightDomain.lecture
        return attributes
    }
}

struct LectureEntityQuery: EntityQuery {
    func entities(for identifiers: [String]) async throws -> [LectureEntity] {
        await SpotlightSupport.lectures(for: identifiers).map(LectureEntity.init)
    }

    /// Most recently dated first.
    func suggestedEntities() async throws -> [LectureEntity] {
        await SpotlightSupport.suggestedLectures().map(LectureEntity.init)
    }
}

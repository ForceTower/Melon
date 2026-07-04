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
}

struct DisciplineEntity: AppEntity, IndexedEntity {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "entity.discipline.typeName")
    static let defaultQuery = DisciplineEntityQuery()

    let projection: SpotlightDiscipline

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

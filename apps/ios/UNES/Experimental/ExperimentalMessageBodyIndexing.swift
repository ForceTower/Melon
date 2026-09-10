#if DEBUG
import AppIntents
import CoreSpotlight
import UNESKit

/// E3 — the blocked half of Phase 2 criterion 3. On iOS 17–27.0,
/// entity-created index items never lexically match their body, which is
/// why shipped messages ride classic `CSSearchableItem`s. This driver
/// re-runs the check on a new OS build: it pushes the newest messages
/// through `indexAppEntities` with `@Property(indexingKey: \.textContent)`
/// so a body-word search in Spotlight gives a fresh verdict. Debug builds
/// only — nothing here ships.
@available(iOS 27, *)
struct ExperimentalIndexedMessageEntity: AppEntity, IndexedEntity {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "entity.message.typeName")
    static let defaultQuery = IndexedMessageQuery()

    let projection: SpotlightMessage

    var id: String { projection.id }

    /// The experiment: body text carried on the entity itself, indexed via
    /// the attribute-set key that classic items use for full-text search.
    @Property(indexingKey: \.textContent)
    var body: String

    init(projection: SpotlightMessage) {
        self.projection = projection
        self.body = projection.body
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(projection.title)", subtitle: "\(projection.subtitle)")
    }

    struct IndexedMessageQuery: EntityQuery {
        func entities(for identifiers: [String]) async throws -> [ExperimentalIndexedMessageEntity] {
            await SpotlightSupport.messages(for: identifiers).map(ExperimentalIndexedMessageEntity.init)
        }

        func suggestedEntities() async throws -> [ExperimentalIndexedMessageEntity] {
            await SpotlightSupport.suggestedMessages().map(ExperimentalIndexedMessageEntity.init)
        }
    }
}

/// One-shot driver: pushes the recent messages through `indexAppEntities`
/// so the E3 question gets a clean answer. Triggered from the Shortcuts app
/// via `ExperimentalBodyIndexIntent`; search a distinctive body word
/// afterwards, record the verdict.
@available(iOS 27, *)
enum ExperimentalMessageBodyIndexing {
    static func indexRecentMessages() async throws -> Int {
        let entities = await SpotlightSupport.suggestedMessages()
            .map(ExperimentalIndexedMessageEntity.init)
        try await CSSearchableIndex.default().indexAppEntities(entities)
        return entities.count
    }
}

/// The E3 trigger, visible in the Shortcuts app on debug builds only.
/// Bare literal strings on purpose: nothing here ships or localizes.
@available(iOS 27, *)
struct ExperimentalBodyIndexIntent: AppIntent {
    static let title: LocalizedStringResource = "E3: index message bodies"

    init() {}

    func perform() async throws -> some IntentResult & ProvidesDialog {
        let count = try await ExperimentalMessageBodyIndexing.indexRecentMessages()
        return .result(dialog: "Indexed \(count) messages via indexAppEntities. Now search a body word.")
    }
}
#endif

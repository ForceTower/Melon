#if UNES_IOS27_EXPERIMENT
import AppIntents
import CoreSpotlight
import UNESKit

/// §3.8(b) — the blocked half of Phase 2 criterion 3. On iOS ≤ 26,
/// entity-created index items never lexically match their body, which is
/// why shipped messages ride classic `CSSearchableItem`s. The 27.0 SDK adds
/// `@Property(indexingKey:)`; the question is whether `indexAppEntities`
/// items now match body words in system-wide search on device (E3).
@available(iOS 27, *)
struct ExperimentalIndexedMessageEntity: AppEntity, IndexedEntity {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "entity.message.typeName")
    static let defaultQuery = Query()

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

    struct Query: EntityQuery {
        func entities(for identifiers: [String]) async throws -> [ExperimentalIndexedMessageEntity] {
            await SpotlightSupport.messages(for: identifiers).map(ExperimentalIndexedMessageEntity.init)
        }

        func suggestedEntities() async throws -> [ExperimentalIndexedMessageEntity] {
            await SpotlightSupport.suggestedMessages().map(ExperimentalIndexedMessageEntity.init)
        }
    }
}

/// One-shot driver for the spike: pushes the recent messages through
/// `indexAppEntities` under a separate domain so the shipped classic-item
/// index stays untouched. Call from a debug hook, search a distinctive body
/// word, record the verdict.
@available(iOS 27, *)
enum ExperimentalMessageBodyIndexing {
    static func indexRecentMessages() async throws {
        let entities = await SpotlightSupport.suggestedMessages()
            .map(ExperimentalIndexedMessageEntity.init)
        try await CSSearchableIndex.default().indexAppEntities(entities)
    }
}
#endif

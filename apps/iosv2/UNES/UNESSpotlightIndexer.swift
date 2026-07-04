import AppIntents
import CoreSpotlight
import UNESKit

/// The `CSSearchableIndex` boundary — everything up to it (observation,
/// coalescing, diffing, the persisted ledger) runs in UNESKit through
/// `SpotlightSupport`; this type only turns projections into entities and
/// hands them to Apple's index.
struct UNESSpotlightIndexer: SpotlightIndexWriter {
    func index(disciplines: [SpotlightDiscipline], messages: [SpotlightMessage]) async throws {
        if !disciplines.isEmpty {
            try await CSSearchableIndex.default().indexAppEntities(disciplines.map(DisciplineEntity.init))
        }
        if !messages.isEmpty {
            // Classic searchable items, not indexAppEntities: entity-created
            // items never match on `textContent`, and body search is the
            // whole point of indexing messages. The entity association keeps
            // them on the Siri/Shortcuts surface.
            let items = messages.map { projection in
                let entity = MessageEntity(projection: projection)
                let item = CSSearchableItem(
                    uniqueIdentifier: projection.id,
                    domainIdentifier: SpotlightDomain.message,
                    attributeSet: entity.attributeSet
                )
                item.expirationDate = .distantFuture
                item.associateAppEntity(entity, priority: 0)
                return item
            }
            try await CSSearchableIndex.default().indexSearchableItems(items)
        }
    }

    func delete(disciplineIds: [String], messageIds: [String]) async throws {
        if !disciplineIds.isEmpty {
            try await CSSearchableIndex.default()
                .deleteAppEntities(identifiedBy: disciplineIds, ofType: DisciplineEntity.self)
        }
        if !messageIds.isEmpty {
            try await CSSearchableIndex.default().deleteSearchableItems(withIdentifiers: messageIds)
        }
    }

    func deleteAll() async throws {
        try await CSSearchableIndex.default().deleteSearchableItems(
            withDomainIdentifiers: [SpotlightDomain.discipline, SpotlightDomain.message]
        )
    }
}

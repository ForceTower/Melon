import AppIntents
import CoreSpotlight
import UNESKit

/// The `CSSearchableIndex` boundary — everything up to it (observation,
/// coalescing, diffing, the persisted ledger) runs in UNESKit through
/// `SpotlightSupport`; this type only turns projections into entities and
/// hands them to Apple's index.
struct UNESSpotlightIndexer: SpotlightIndexWriter {
    func index(
        disciplines: [SpotlightDiscipline],
        messages: [SpotlightMessage],
        evaluations: [SpotlightEvaluation]
    ) async throws {
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
        if !evaluations.isEmpty {
            try await CSSearchableIndex.default().indexAppEntities(evaluations.map(EvaluationEntity.init))
        }
    }

    func delete(disciplineIds: [String], messageIds: [String], evaluationIds: [String]) async throws {
        if !disciplineIds.isEmpty {
            try await CSSearchableIndex.default()
                .deleteAppEntities(identifiedBy: disciplineIds, ofType: DisciplineEntity.self)
        }
        if !messageIds.isEmpty {
            try await CSSearchableIndex.default().deleteSearchableItems(withIdentifiers: messageIds)
        }
        if !evaluationIds.isEmpty {
            try await CSSearchableIndex.default()
                .deleteAppEntities(identifiedBy: evaluationIds, ofType: EvaluationEntity.self)
        }
    }

    func deleteAll() async throws {
        // Not a domain wipe: entity-created items (disciplines, evaluations,
        // and any left behind by older identifier schemes) carry no domain
        // identifier, so only a true delete-all reaches them.
        try await CSSearchableIndex.default().deleteAllSearchableItems()
    }

    func disciplinesDidChange() async {
        // The Prova Final phrases embed discipline names; re-registering the
        // shortcuts refreshes what Siri accepts in the parameter slot.
        UNESAppShortcuts.updateAppShortcutParameters()
    }
}

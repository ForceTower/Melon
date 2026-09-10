import AppIntents
import CoreSpotlight
import UNESKit

/// The `CSSearchableIndex` boundary — everything up to it (observation,
/// coalescing, diffing, the persisted ledger) runs in UNESKit through
/// `SpotlightSupport`; this type only turns projections into entities and
/// hands them to Apple's index. On iOS 27 the calendar-shaped kinds and
/// messages are written as schema entities (Siri AI understands those);
/// the OS-major key in the ledger re-indexes everything after an upgrade.
struct UNESSpotlightIndexer: SpotlightIndexWriter {
    func index(_ batch: SpotlightIndexBatch) async throws {
        let index = CSSearchableIndex.default()
        if !batch.disciplines.isEmpty {
            try await index.indexAppEntities(batch.disciplines.map(DisciplineEntity.init))
        }
        if !batch.messages.isEmpty {
            if #available(iOS 27, *) {
                // The schema entity is the message's only entry on 27: Siri
                // AI reads it, Spotlight lists it, and the tap-through rides
                // its identifier.
                try await index.indexAppEntities(batch.messages.map(MessageEventEntity.init))
            } else {
                // Classic searchable items with the entity attached — what
                // Spotlight lists and taps through below 27.
                let items = batch.messages.map { projection in
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
                try await index.indexSearchableItems(items)
            }
        }
        if !batch.evaluations.isEmpty {
            if #available(iOS 27, *) {
                try await index.indexAppEntities(batch.evaluations.compactMap(AcademicEventEntity.init))
            } else {
                try await index.indexAppEntities(batch.evaluations.map(EvaluationEntity.init))
            }
        }
        if !batch.lectures.isEmpty {
            try await index.indexAppEntities(batch.lectures.map(LectureEntity.init))
        }
        if #available(iOS 27, *) {
            if !batch.sessions.isEmpty {
                try await index.indexAppEntities(batch.sessions.compactMap(AcademicEventEntity.init))
            }
            if !batch.personalEvents.isEmpty {
                try await index.indexAppEntities(batch.personalEvents.compactMap(AcademicEventEntity.init))
            }
        }
        // Below iOS 27 sessions and personal events are projected and
        // ledgered but never written; the ledger's OS key indexes them
        // once the device upgrades.
    }

    func delete(_ batch: SpotlightDeleteBatch) async throws {
        let index = CSSearchableIndex.default()
        if !batch.disciplineIds.isEmpty {
            try await index.deleteAppEntities(identifiedBy: batch.disciplineIds, ofType: DisciplineEntity.self)
        }
        if !batch.messageIds.isEmpty {
            if #available(iOS 27, *) {
                try await index.deleteAppEntities(identifiedBy: batch.messageIds, ofType: MessageEventEntity.self)
            } else {
                try await index.deleteSearchableItems(withIdentifiers: batch.messageIds)
            }
        }
        if !batch.evaluationIds.isEmpty {
            if #available(iOS 27, *) {
                try await index.deleteAppEntities(identifiedBy: batch.evaluationIds, ofType: AcademicEventEntity.self)
            } else {
                try await index.deleteAppEntities(identifiedBy: batch.evaluationIds, ofType: EvaluationEntity.self)
            }
        }
        if !batch.lectureIds.isEmpty {
            try await index.deleteAppEntities(identifiedBy: batch.lectureIds, ofType: LectureEntity.self)
        }
        if #available(iOS 27, *) {
            let eventIds = batch.sessionIds + batch.personalEventIds
            if !eventIds.isEmpty {
                try await index.deleteAppEntities(identifiedBy: eventIds, ofType: AcademicEventEntity.self)
            }
        }
    }

    func deleteAll() async throws {
        // Not a domain wipe: entity-created items (disciplines, evaluations,
        // and any left behind by older identifier schemes) carry no domain
        // identifier, so only a true delete-all reaches them.
        try await CSSearchableIndex.default().deleteAllSearchableItems()
    }

    func disciplinesDidChange() async {
        // The discipline-scoped phrases embed discipline names;
        // re-registering the shortcuts refreshes what Siri accepts in the
        // parameter slot.
        UNESAppShortcuts.updateAppShortcutParameters()
    }
}

import Foundation
import GRDB

// MARK: - Personal calendar entries

/// One student-created calendar entry. Unlike every other record here this
/// one has no upstream counterpart — the app is the only writer.
struct PersonalEventRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "personalEvents"
    var id: String
    var title: String
    /// yyyy-MM-dd.
    var start: String
    var end: String?
    var category: String
    var disciplineId: String?
    var disciplineCode: String?
    var disciplineName: String?
    var reminderDays: Int
    var notes: String
    var createdAt: String

    init(_ event: PersonalEvent) {
        id = event.id
        title = event.title
        start = event.start
        end = event.end
        category = event.category.rawValue
        disciplineId = event.discipline?.id
        disciplineCode = event.discipline?.code
        disciplineName = event.discipline?.name
        reminderDays = event.reminder.rawValue
        notes = event.notes
        createdAt = event.createdAt.formatted(MirrorStore.timestampFormat)
    }

    var domain: PersonalEvent {
        PersonalEvent(
            id: id,
            title: title,
            start: start,
            end: end,
            // An unknown kind can only come from a newer build's row — the
            // generic tone beats dropping the entry.
            category: PersonalEvent.Category(rawValue: category) ?? .task,
            discipline: discipline,
            reminder: PersonalEvent.Reminder(rawValue: reminderDays) ?? .none,
            notes: notes,
            createdAt: (try? Date(createdAt, strategy: MirrorStore.timestampFormat)) ?? .distantPast
        )
    }

    private var discipline: PersonalEvent.DisciplineTag? {
        guard let disciplineId, let disciplineCode, let disciplineName else { return nil }
        return PersonalEvent.DisciplineTag(id: disciplineId, code: disciplineCode, name: disciplineName)
    }
}

extension MirrorStore {
    func savePersonalEvent(_ event: PersonalEvent) async throws {
        try await writer.write { db in
            try PersonalEventRecord(event).upsert(db)
        }
    }

    func deletePersonalEvent(id: String) async throws {
        _ = try await writer.write { db in
            try PersonalEventRecord.deleteOne(db, key: id)
        }
    }

    /// Every stored entry, earliest first.
    func personalEvents() async throws -> [PersonalEvent] {
        try await writer.read { db in try Self.personalEvents(db) }
    }

    /// Emits the stored entries on subscription and again after every write
    /// that changes them.
    func personalEventUpdates() -> AsyncValueObservation<[PersonalEvent]> {
        ValueObservation
            .tracking { db in try Self.personalEvents(db) }
            .values(in: writer)
    }

    private static func personalEvents(_ db: Database) throws -> [PersonalEvent] {
        try PersonalEventRecord
            .order(Column("start"), Column("createdAt"), Column("id"))
            .fetchAll(db)
            .map(\.domain)
    }
}

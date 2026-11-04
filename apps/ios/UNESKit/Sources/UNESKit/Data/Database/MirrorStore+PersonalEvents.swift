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
    var completedAt: String?

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
        completedAt = event.completedAt?.formatted(MirrorStore.timestampFormat)
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
            createdAt: (try? Date(createdAt, strategy: MirrorStore.timestampFormat)) ?? .distantPast,
            completedAt: completedAt.flatMap { try? Date($0, strategy: MirrorStore.timestampFormat) }
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

    /// Touches only the completion column, so a check-off never races an
    /// edit made in the composer.
    func setPersonalEventCompleted(id: String, completedAt: Date?) async throws {
        let stamp = completedAt?.formatted(MirrorStore.timestampFormat)
        _ = try await writer.write { db in
            try PersonalEventRecord
                .filter(key: id)
                .updateAll(db, Column("completedAt").set(to: stamp))
        }
    }

    func deletePersonalEvent(id: String) async throws {
        _ = try await writer.write { db in
            try PersonalEventRecord.deleteOne(db, key: id)
        }
    }

    /// Every stored entry, earliest first.
    func personalEvents() async throws -> [PersonalEvent] {
        try await writer.read { db in try Self.fetchPersonalEvents(db) }
    }

    /// Emits the stored entries on subscription and again after every write
    /// that changes them.
    func personalEventUpdates() -> AsyncValueObservation<[PersonalEvent]> {
        ValueObservation
            .tracking { db in try Self.fetchPersonalEvents(db) }
            .values(in: writer)
    }

    /// Shared with the Spotlight projection, which reads the table inside
    /// its own observation.
    static func fetchPersonalEvents(_ db: Database) throws -> [PersonalEvent] {
        try PersonalEventRecord
            .order(Column("start"), Column("createdAt"), Column("id"))
            .fetchAll(db)
            .map(\.domain)
    }
}

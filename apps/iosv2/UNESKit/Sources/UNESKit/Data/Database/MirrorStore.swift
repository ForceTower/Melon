import Foundation
import GRDB

/// Read/write access to the local SQLite mirror of the apps/api sync data.
struct MirrorStore: Sendable {
    var writer: any DatabaseWriter

    private static let lastSyncedAtKey = "lastSyncedAt"
    private static let timestampFormat = Date.ISO8601FormatStyle(includingFractionalSeconds: true)

    // MARK: Write path

    /// Applies one successful refresh atomically: upserts the semester list,
    /// replaces the fetched semester's whole scope (so rows deleted upstream
    /// disappear locally), and stamps `lastSyncedAt`.
    func apply(semesters: [SemesterRecord], snapshot: SemesterSnapshot?, syncedAt: Date) async throws {
        try await writer.write { db in
            for semester in semesters {
                try semester.upsert(db)
            }
            if let snapshot {
                try Self.replaceScope(with: snapshot, db: db)
            }
            let stamp = syncedAt.formatted(Self.timestampFormat)
            try SyncStateRecord(key: Self.lastSyncedAtKey, value: stamp).upsert(db)
        }
    }

    func upsertMessages(_ messages: [MessageRecord]) async throws {
        try await writer.write { db in
            for message in messages {
                try message.upsert(db)
            }
        }
    }

    // MARK: Read path

    /// The Home snapshot as mirrored on disk; nil until the first successful
    /// refresh lands.
    func cachedOverview(now: Date) async throws -> CachedHomeOverview? {
        let today = now.dayStamp
        return try await writer.read { db in
            guard let syncedAt = try Self.lastSyncedAt(db) else { return nil }

            var overview = HomeOverview.empty
            let semesters = try SemesterRecord.order(Column("startDate").desc).fetchAll(db)
            if let active = semesters.map(\.domain).active(today: today),
               let record = semesters.first(where: { $0.id == active.id }) {
                overview = try Self.snapshot(for: record, db: db).homeOverview(now: now)
            }
            overview.messages = try Self.messagesSummary(db)
            return CachedHomeOverview(overview: overview, syncedAt: syncedAt)
        }
    }

    func messagesSummary() async throws -> MessagesSummary? {
        try await writer.read { db in try Self.messagesSummary(db) }
    }

    // MARK: Queries

    private static func replaceScope(with snapshot: SemesterSnapshot, db: Database) throws {
        try snapshot.semester.upsert(db)

        let scope = Column("semesterId") == snapshot.semester.id
        try DisciplineRecord.filter(scope).deleteAll(db)
        try DisciplineOfferRecord.filter(scope).deleteAll(db)
        try ClassRecord.filter(scope).deleteAll(db)
        try TeacherRecord.filter(scope).deleteAll(db)
        try ClassTeacherRecord.filter(scope).deleteAll(db)
        try SpaceRecord.filter(scope).deleteAll(db)
        try AllocationRecord.filter(scope).deleteAll(db)
        try StudentClassRecord.filter(scope).deleteAll(db)
        try StudentGradeRecord.filter(scope).deleteAll(db)
        try LectureRecord.filter(scope).deleteAll(db)

        for row in snapshot.disciplines { try row.insert(db) }
        for row in snapshot.disciplineOffers { try row.insert(db) }
        for row in snapshot.classes { try row.insert(db) }
        for row in snapshot.teachers { try row.insert(db) }
        for row in snapshot.classTeachers { try row.insert(db) }
        for row in snapshot.spaces { try row.insert(db) }
        for row in snapshot.allocations { try row.insert(db) }
        for row in snapshot.studentClasses { try row.insert(db) }
        for row in snapshot.studentGrades { try row.insert(db) }
        for row in snapshot.lectures { try row.insert(db) }
    }

    private static func snapshot(for semester: SemesterRecord, db: Database) throws -> SemesterSnapshot {
        func scoped<R: FetchableRecord & TableRecord>(_ type: R.Type) throws -> [R] {
            try R.filter(Column("semesterId") == semester.id).orderByPrimaryKey().fetchAll(db)
        }
        return try SemesterSnapshot(
            semester: semester,
            disciplines: scoped(DisciplineRecord.self),
            disciplineOffers: scoped(DisciplineOfferRecord.self),
            classes: scoped(ClassRecord.self),
            teachers: scoped(TeacherRecord.self),
            classTeachers: scoped(ClassTeacherRecord.self),
            spaces: scoped(SpaceRecord.self),
            allocations: scoped(AllocationRecord.self),
            studentClasses: scoped(StudentClassRecord.self),
            studentGrades: scoped(StudentGradeRecord.self),
            lectures: scoped(LectureRecord.self)
        )
    }

    private static func messagesSummary(_ db: Database) throws -> MessagesSummary? {
        let messages = try MessageRecord.order(Column("timestamp").desc).fetchAll(db)
        guard let latest = messages.first else { return nil }
        let preview = [latest.content, latest.subject]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .first { !$0.isEmpty }
        return MessagesSummary(
            unreadCount: messages.count { $0.read == false },
            latestSenderName: latest.senderName?.trimmingCharacters(in: .whitespacesAndNewlines),
            latestPreview: preview
        )
    }

    private static func lastSyncedAt(_ db: Database) throws -> Date? {
        try SyncStateRecord.fetchOne(db, key: lastSyncedAtKey)
            .flatMap { try? Date($0.value, strategy: timestampFormat) }
    }
}

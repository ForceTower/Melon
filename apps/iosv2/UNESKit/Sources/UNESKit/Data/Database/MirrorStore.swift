import Foundation
import GRDB

/// Read/write access to the local SQLite mirror of the apps/api sync data.
struct MirrorStore: Sendable {
    var writer: any DatabaseWriter

    private static let lastSyncedAtKey = "lastSyncedAt"
    static let timestampFormat = Date.ISO8601FormatStyle(includingFractionalSeconds: true)

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

    // MARK: Read path

    /// The Home snapshot as mirrored on disk; nil until the first successful
    /// refresh lands.
    func cachedOverview(now: Date) async throws -> CachedHomeOverview? {
        try await writer.read { db in try Self.cachedOverview(db, now: now) }
    }

    /// Emits the mirrored Home snapshot on subscription and again after every
    /// write that feeds it — sync refreshes, message read/star overlays,
    /// semester downloads.
    func overviewUpdates(now: @escaping @Sendable () -> Date) -> AsyncValueObservation<CachedHomeOverview?> {
        ValueObservation
            .tracking { db in try Self.cachedOverview(db, now: now()) }
            .values(in: writer)
    }

    private static func cachedOverview(_ db: Database, now: Date) throws -> CachedHomeOverview? {
        guard let syncedAt = try lastSyncedAt(db) else { return nil }

        var overview = HomeOverview.empty
        let semesters = try SemesterRecord.order(Column("startDate").desc).fetchAll(db)
        if let active = semesters.map(\.domain).active(today: now.dayStamp),
           let record = semesters.first(where: { $0.id == active.id }) {
            overview = try snapshot(for: record, db: db).homeOverview(now: now)
        }
        overview.messages = try messagesSummary(db)
        return CachedHomeOverview(overview: overview, syncedAt: syncedAt)
    }

    /// The Horário week as mirrored on disk; nil until the first successful
    /// refresh lands.
    func cachedScheduleOverview(now: Date) async throws -> ScheduleOverview? {
        let today = now.dayStamp
        return try await writer.read { db in
            guard try Self.lastSyncedAt(db) != nil else { return nil }
            let semesters = try SemesterRecord.order(Column("startDate").desc).fetchAll(db)
            guard let active = semesters.map(\.domain).active(today: today),
                  let record = semesters.first(where: { $0.id == active.id })
            else { return .empty }
            return try Self.snapshot(for: record, db: db).scheduleOverview(now: now)
        }
    }

    /// The Turmas snapshot as mirrored on disk; nil until the first
    /// successful refresh lands.
    func cachedDisciplinesOverview(now: Date) async throws -> DisciplinesOverview? {
        try await writer.read { db in
            guard try Self.lastSyncedAt(db) != nil else { return nil }
            return try Self.disciplinesOverview(now: now, db: db)
        }
    }

    func disciplinesOverview(now: Date) async throws -> DisciplinesOverview {
        try await writer.read { db in try Self.disciplinesOverview(now: now, db: db) }
    }

    /// The detail feed for one mirrored discipline; nil while the semester's
    /// payload (or the discipline itself) isn't mirrored.
    func disciplineDetail(semesterId: String, disciplineId: String, now: Date) async throws -> DisciplineDetail? {
        try await writer.read { db in
            guard let semester = try SemesterRecord.fetchOne(db, key: semesterId) else { return nil }
            var detail = try Self.snapshot(for: semester, db: db)
                .disciplineDetail(disciplineId: disciplineId, now: now)
            detail?.syncedAt = try Self.lastSyncedAt(db)
            return detail
        }
    }

    private static func disciplinesOverview(now: Date, db: Database) throws -> DisciplinesOverview {
        let semesters = try SemesterRecord.order(Column("startDate").desc).fetchAll(db)
        // Enrollment rows are the ground truth for "payload mirrored".
        let downloadedIds = try Set(
            String.fetchAll(db, StudentClassRecord.select(Column("semesterId"), as: String.self).distinct())
        )
        let activeId = semesters.map(\.domain).active(today: now.dayStamp)?.id

        var overview = DisciplinesOverview()
        for record in semesters {
            if downloadedIds.contains(record.id) {
                let group = SemesterDisciplines(
                    id: record.id,
                    code: record.code,
                    disciplines: try snapshot(for: record, db: db).disciplineSummaries(now: now)
                )
                if record.id == activeId {
                    overview.current = group
                } else {
                    overview.past.append(group)
                }
            } else {
                overview.pending.append(
                    PendingSemester(id: record.id, code: record.code, disciplineCount: record.disciplineCount)
                )
            }
        }
        return overview
    }

    // MARK: Queries

    private static func replaceScope(with snapshot: SemesterSnapshot, db: Database) throws {
        // The payload's semester row carries no disciplineCount — keep the
        // one the semester list wrote instead of nulling it.
        var semester = snapshot.semester
        if semester.disciplineCount == nil {
            semester.disciplineCount = try SemesterRecord.fetchOne(db, key: semester.id)?.disciplineCount
        }
        try semester.upsert(db)

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
        try LectureMaterialRecord.filter(scope).deleteAll(db)

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
        for row in snapshot.lectureMaterials { try row.insert(db) }
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
            lectures: scoped(LectureRecord.self),
            lectureMaterials: scoped(LectureMaterialRecord.self)
        )
    }

    static func lastSyncedAt(_ db: Database) throws -> Date? {
        try SyncStateRecord.fetchOne(db, key: lastSyncedAtKey)
            .flatMap { try? Date($0.value, strategy: timestampFormat) }
    }
}

import ComposableArchitecture
import Foundation
import GRDB

private let log = Log.scoped("AppDatabase")

func appDatabase() throws -> any DatabaseWriter {
    try FileManager.default.createDirectory(
        at: .applicationSupportDirectory,
        withIntermediateDirectories: true
    )
    let path = URL.applicationSupportDirectory.appending(path: "unes.sqlite").path(percentEncoded: false)
    do {
        let database = try DatabaseQueue(path: path)
        try migrator().migrate(database)
        log.info("database opened path=\(path) migrations=applied")
        return database
    } catch {
        log.error("database open or migration failed path=\(path)", error: error)
        throw error
    }
}

/// A fresh, fully migrated queue for tests and previews.
func inMemoryDatabase() throws -> any DatabaseWriter {
    do {
        let database = try DatabaseQueue()
        try migrator().migrate(database)
        log.debug("in-memory database opened migrations=applied")
        return database
    } catch {
        log.error("in-memory database open or migration failed", error: error)
        throw error
    }
}

private func migrator() -> DatabaseMigrator {
    var migrator = DatabaseMigrator()
    #if DEBUG
    migrator.eraseDatabaseOnSchemaChange = true
    #endif

    // One table per api/sync payload array (plus messages and sync metadata).
    // Every semester-scoped table carries `semesterId` so a re-sync can
    // replace one semester's rows atomically. Disciplines, teachers, and
    // spaces are shared across semesters upstream, so their mirror rows are
    // keyed by (semesterId, id); the rest have globally unique upstream ids.
    migrator.registerMigration("v1") { db in
        try db.create(table: "semesters") { t in
            t.primaryKey("id", .text)
            t.column("code", .text).notNull()
            t.column("description", .text).notNull()
            t.column("startDate", .text).notNull()
            t.column("endDate", .text).notNull()
            t.column("disciplineCount", .integer)
        }
        try db.create(table: "disciplines") { t in
            t.column("semesterId", .text).notNull()
            t.column("id", .text).notNull()
            t.column("code", .text)
            t.column("name", .text).notNull()
            t.column("hours", .integer)
            t.primaryKey(["semesterId", "id"])
        }
        try db.create(table: "disciplineOffers") { t in
            t.primaryKey("id", .text)
            t.column("semesterId", .text).notNull().indexed()
            t.column("disciplineId", .text).notNull()
        }
        try db.create(table: "classes") { t in
            t.primaryKey("id", .text)
            t.column("semesterId", .text).notNull().indexed()
            t.column("offerId", .text).notNull()
            t.column("hours", .integer).notNull()
            t.column("groupName", .text)
            t.column("type", .text)
        }
        try db.create(table: "teachers") { t in
            t.column("semesterId", .text).notNull()
            t.column("id", .text).notNull()
            t.column("name", .text).notNull()
            t.primaryKey(["semesterId", "id"])
        }
        try db.create(table: "classTeachers") { t in
            t.column("semesterId", .text).notNull().indexed()
            t.column("classId", .text).notNull()
            t.column("teacherId", .text).notNull()
            t.primaryKey(["classId", "teacherId"])
        }
        try db.create(table: "spaces") { t in
            t.column("semesterId", .text).notNull()
            t.column("id", .text).notNull()
            t.column("location", .text).notNull()
            t.primaryKey(["semesterId", "id"])
        }
        try db.create(table: "allocations") { t in
            t.primaryKey("id", .text)
            t.column("semesterId", .text).notNull().indexed()
            t.column("classId", .text).notNull()
            t.column("spaceId", .text)
            t.column("day", .integer)
            t.column("startTime", .text)
            t.column("endTime", .text)
        }
        try db.create(table: "studentClasses") { t in
            t.primaryKey("id", .text)
            t.column("semesterId", .text).notNull().indexed()
            t.column("classId", .text).notNull()
            t.column("missedClasses", .integer)
            t.column("finalGrade", .text)
            t.column("approved", .boolean)
            t.column("wentToFinals", .boolean)
        }
        try db.create(table: "studentGrades") { t in
            t.primaryKey("id", .text)
            t.column("semesterId", .text).notNull().indexed()
            t.column("studentClassId", .text).notNull()
            t.column("name", .text)
            t.column("nameShort", .text)
            t.column("ordinal", .integer).notNull()
            t.column("value", .text)
            t.column("date", .text)
            t.column("platformId", .text)
            t.column("weight", .text)
        }
        try db.create(table: "lectures") { t in
            t.primaryKey("id", .text)
            t.column("semesterId", .text).notNull().indexed()
            t.column("classId", .text).notNull()
            t.column("date", .text)
            t.column("subject", .text)
        }
        try db.create(table: "messages") { t in
            t.primaryKey("id", .text)
            t.column("subject", .text)
            t.column("content", .text)
            t.column("senderName", .text)
            t.column("timestamp", .text)
            t.column("read", .boolean)
        }
        try db.create(table: "syncState") { t in
            t.primaryKey("key", .text)
            t.column("value", .text).notNull()
        }
    }

    // Discipline detail: syllabus + department on disciplines, offer-level
    // hours, lecture ordering, and the lecture materials ("Anexos") table.
    migrator.registerMigration("v2") { db in
        try db.alter(table: "disciplines") { t in
            t.add(column: "department", .text)
            t.add(column: "program", .text)
        }
        try db.alter(table: "disciplineOffers") { t in
            t.add(column: "hours", .integer)
        }
        try db.alter(table: "lectures") { t in
            t.add(column: "ordinal", .integer)
        }
        try db.create(table: "lectureMaterials") { t in
            t.primaryKey("id", .text)
            t.column("semesterId", .text).notNull().indexed()
            t.column("lectureId", .text).notNull()
            t.column("caption", .text)
            t.column("url", .text).notNull()
            t.column("position", .integer)
        }
    }
    // Schedule v2: the campus + module labels the spaces table was dropping
    // on the way in.
    migrator.registerMigration("v3") { db in
        try db.alter(table: "spaces") { t in
            t.add(column: "campus", .text)
            t.add(column: "modulo", .text)
        }
    }
    // Messages v2: the rest of the wire shape (source, senderType, starred),
    // the scope rows that resolve a message's origin, attachments, and the
    // local read/star overlay (the backend has no mutation endpoint).
    migrator.registerMigration("v4") { db in
        try db.alter(table: "messages") { t in
            t.add(column: "source", .text)
            t.add(column: "senderType", .integer)
            t.add(column: "starred", .boolean)
        }
        try db.create(table: "messageScopes") { t in
            t.primaryKey("id", .text)
            t.column("messageId", .text).notNull().indexed()
            t.column("scope", .text).notNull()
            t.column("classId", .text)
            t.column("disciplineCode", .text)
            t.column("disciplineName", .text)
        }
        try db.create(table: "messageAttachments") { t in
            t.primaryKey("id", .text)
            t.column("messageId", .text).notNull().indexed()
            t.column("kind", .text).notNull()
            t.column("name", .text)
            t.column("url", .text).notNull()
            t.column("position", .integer)
        }
        try db.create(table: "messageStates") { t in
            t.primaryKey("messageId", .text)
            t.column("readAt", .text)
            t.column("starred", .boolean).notNull().defaults(to: false)
        }
    }
    // Spotlight v2: the index ledger moves from its pre-Phase-3 JSON file
    // into the mirror database. Both tables deliberately survive `wipe()` —
    // the logout wipe empties the Spotlight index through the nil-snapshot
    // path, which needs the surviving non-empty ledger to know entries exist.
    migrator.registerMigration("v5") { db in
        try db.create(table: "spotlightLedger") { t in
            t.primaryKey("identifier", .text)
            t.column("kind", .text).notNull()
            t.column("digest", .text).notNull()
        }
        try db.create(table: "spotlightLedgerState") { t in
            t.primaryKey("key", .text)
            t.column("value", .text).notNull()
        }
    }
    // Campus events: the featured event as one JSON snapshot row, so the hub
    // works offline and observers wake on publish-driven changes only.
    migrator.registerMigration("v6") { db in
        try db.create(table: "campusEvent") { t in
            t.primaryKey("id", .text)
            t.column("revision", .integer).notNull()
            t.column("payload", .text).notNull()
            t.column("syncedAt", .text).notNull()
        }
    }
    // Personal calendar entries. The only table nothing upstream writes: the
    // student owns every row, so it has no `semesterId` scope and a re-sync
    // never touches it. The discipline columns are denormalized on purpose
    // (see `PersonalEvent.discipline`).
    migrator.registerMigration("v7") { db in
        try db.create(table: "personalEvents") { t in
            t.primaryKey("id", .text)
            t.column("title", .text).notNull()
            t.column("start", .text).notNull().indexed()
            t.column("end", .text)
            t.column("category", .text).notNull()
            t.column("disciplineId", .text)
            t.column("disciplineCode", .text)
            t.column("disciplineName", .text)
            t.column("reminderDays", .integer).notNull()
            t.column("notes", .text).notNull()
            t.column("createdAt", .text).notNull()
        }
    }
    // Semester staleness: the server's `dirtyAt` from the list plus the one
    // the client last applied, so refreshes can re-pull a mirrored semester
    // whose data moved server-side after it was downloaded.
    migrator.registerMigration("v8") { db in
        try db.alter(table: "semesters") { t in
            t.add(column: "dirtyAt", .text)
            t.add(column: "appliedDirtyAt", .text)
        }
    }
    // App-message attribution: the admin behind a comunicado, shown under the
    // sender name. Nullable — older rows and upstream messages have none.
    migrator.registerMigration("v9") { db in
        try db.alter(table: "messages") { t in
            t.add(column: "authorName", .text)
        }
    }
    // Course progress: the student's curriculum ("matriz curricular") as
    // real rows — the version, its hour-type buckets, every grid slot with
    // the student's status, and the prerequisite edges — plus one summary
    // row. Only ever one curriculum at a time (the binding), replaced whole
    // on every refresh so the fluxograma and progress screen work offline.
    migrator.registerMigration("v10") { db in
        try db.create(table: "curricula") { t in
            t.primaryKey("id", .text)
            t.column("code", .text).notNull()
            t.column("label", .text).notNull()
            t.column("asOf", .text).notNull()
            t.column("minPeriods", .integer)
            t.column("maxPeriods", .integer)
            t.column("stale", .boolean).notNull()
        }
        try db.create(table: "curriculumProgress") { t in
            t.primaryKey("key", .text)
            t.column("curriculumId", .text)
            t.column("completedHours", .integer).notNull()
            t.column("requiredHours", .integer)
            t.column("percent", .double)
            t.column("excludedHours", .integer).notNull()
            t.column("unclassifiedHours", .integer).notNull()
            t.column("disciplinesCompleted", .integer).notNull()
            t.column("disciplinesTotal", .integer).notNull()
            t.column("currentPeriod", .integer)
            t.column("prerequisitesKnown", .boolean).notNull()
            t.column("syncedAt", .text).notNull()
        }
        try db.create(table: "curriculumRequirements") { t in
            t.column("curriculumId", .text).notNull()
            t.column("code", .text).notNull()
            t.column("kind", .text).notNull()
            t.column("label", .text).notNull()
            t.column("shortLabel", .text).notNull()
            t.column("startsAtPeriod", .integer)
            t.column("hoursRequired", .integer).notNull()
            t.column("hoursCompleted", .integer).notNull()
            t.column("derivable", .boolean).notNull()
            t.column("percent", .double)
            t.column("position", .integer).notNull()
            t.primaryKey(["curriculumId", "code"])
        }
        try db.create(table: "curriculumEntries") { t in
            t.column("curriculumId", .text).notNull()
            t.column("code", .text).notNull()
            t.column("name", .text).notNull()
            t.column("hours", .integer).notNull()
            t.column("credits", .integer)
            t.column("period", .integer)
            t.column("coreqGroup", .integer)
            t.column("requirementCode", .text)
            t.column("status", .text).notNull()
            t.column("position", .integer).notNull()
            t.primaryKey(["curriculumId", "code"])
        }
        try db.create(table: "curriculumPrerequisites") { t in
            t.column("curriculumId", .text).notNull()
            t.column("entryCode", .text).notNull()
            t.column("requiresCode", .text).notNull()
            t.column("kind", .text).notNull()
            t.column("position", .integer).notNull()
            t.primaryKey(["curriculumId", "entryCode", "requiresCode", "kind"])
        }
    }
    return migrator
}

private enum DatabaseKey: DependencyKey {
    static let liveValue: any DatabaseWriter = try! appDatabase()
    static var testValue: any DatabaseWriter { try! inMemoryDatabase() }
    static var previewValue: any DatabaseWriter { try! inMemoryDatabase() }
}

extension DependencyValues {
    var database: any DatabaseWriter {
        get { self[DatabaseKey.self] }
        set { self[DatabaseKey.self] = newValue }
    }
}

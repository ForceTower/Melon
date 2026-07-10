import Foundation
import GRDB

private let log = Log.scoped("WatchStore")

/// The watch's structured cache of everything the phone pushes: a slim GRDB
/// database (schedule pattern, disciplines, grades, one status row) applied
/// as a full transactional replace per push. Views observe it through
/// `updates()`, so every landed push re-renders every screen reactively —
/// the same ValueObservation pattern the phone tabs use over their mirror.
///
/// The file lives in the watch's App Group container so the watch widget
/// extension can read the same data when building complication timelines.
struct WatchStore: Sendable {
    var writer: any DatabaseWriter

    // MARK: Write path (the WatchConnectivity receiver is the only caller)

    /// Replaces the whole cache with the pushed payload; nil (signed out)
    /// leaves the tables empty. The local read overlay survives pushes —
    /// only rows for messages that left the payload (or a sign-out) go.
    func apply(_ snapshot: WatchSnapshot?) async throws {
        try await writer.write { db in
            try WatchStatusRecord.deleteAll(db)
            try WatchDisciplineRecord.deleteAll(db)
            try WatchGradeRecord.deleteAll(db)
            try WatchSessionRecord.deleteAll(db)
            try WatchTopicRecord.deleteAll(db)
            try WatchMessageRecord.deleteAll(db)
            guard let snapshot else {
                try WatchMessageReadRecord.deleteAll(db)
                return
            }
            let messageIds = snapshot.messages.map(\.id)
            try WatchMessageReadRecord.filter(!messageIds.contains(Column("messageId"))).deleteAll(db)

            try WatchStatusRecord(
                semesterCode: snapshot.schedule.semesterCode,
                coefficient: snapshot.coefficient,
                coefficientDelta: snapshot.coefficientDelta,
                attendancePercent: snapshot.attendancePercent,
                remainingAbsences: snapshot.remainingAbsences,
                examLabel: snapshot.nextExam?.label,
                examDisciplineName: snapshot.nextExam?.disciplineName,
                examDate: snapshot.nextExam?.date,
                examTime: snapshot.nextExam?.time,
                syncedAt: snapshot.syncedAt
            ).insert(db)

            for discipline in snapshot.disciplines {
                try WatchDisciplineRecord(
                    id: discipline.id,
                    code: discipline.code,
                    name: discipline.name,
                    teacherName: discipline.teacherName,
                    hours: discipline.hours,
                    missedHours: discipline.missedHours,
                    partialAverage: discipline.partialAverage,
                    colorIndex: discipline.colorIndex
                ).insert(db)
                for (ordinal, grade) in discipline.grades.enumerated() {
                    try WatchGradeRecord(
                        id: grade.id,
                        disciplineId: discipline.id,
                        ordinal: ordinal,
                        label: grade.label,
                        name: grade.name,
                        value: grade.value,
                        date: grade.date
                    ).insert(db)
                }
            }
            for session in snapshot.schedule.sessions {
                try WatchSessionRecord(
                    classId: session.classId,
                    day: session.day,
                    startMinute: session.startMinute,
                    endMinute: session.endMinute,
                    code: session.code,
                    title: session.title,
                    room: session.room,
                    teacherName: session.teacherName,
                    colorIndex: session.colorIndex,
                    disciplineId: session.disciplineId
                ).insert(db)
            }
            for topic in snapshot.schedule.topics {
                try WatchTopicRecord(
                    classId: topic.classId,
                    dayStamp: topic.dayStamp,
                    subject: topic.subject
                ).insert(db)
            }
            for (ordinal, message) in snapshot.messages.enumerated() {
                try WatchMessageRecord(
                    id: message.id,
                    ordinal: ordinal,
                    origin: message.origin.rawValue,
                    disciplineCode: message.disciplineCode,
                    disciplineName: message.disciplineName,
                    disciplineColorIndex: message.disciplineColorIndex,
                    subject: message.subject,
                    body: message.body,
                    senderName: message.senderName,
                    receivedAt: message.receivedAt,
                    unread: message.unread
                ).insert(db)
                for (position, attachment) in message.attachments.enumerated() {
                    try WatchMessageAttachmentRecord(
                        messageId: message.id,
                        position: position,
                        id: attachment.id,
                        kind: attachment.kind.rawValue,
                        name: attachment.name,
                        url: attachment.url
                    ).insert(db)
                }
            }
        }
        log.info("apply ok hasSnapshot=\(snapshot != nil) disciplines=\(snapshot?.disciplines.count ?? 0) sessions=\(snapshot?.schedule.sessions.count ?? 0) messages=\(snapshot?.messages.count ?? 0)")
    }

    /// The watch-local read overlay: opening a message flips it immediately,
    /// even if the receipt never reaches the phone.
    func markMessageRead(id: String, now: Date) async throws {
        try await writer.write { db in
            try WatchMessageReadRecord(messageId: id, readAt: now).upsert(db)
        }
    }

    // MARK: Read path

    /// Emits the whole cached dataset on subscription and again after every
    /// applied push; nil while signed out (or never synced).
    func updates() -> AsyncValueObservation<WatchSnapshot?> {
        ValueObservation
            .tracking { db in try Self.snapshot(db) }
            .removeDuplicates()
            .values(in: writer)
    }

    /// One-shot read for widget timeline providers.
    func current() throws -> WatchSnapshot? {
        try writer.unsafeReentrantRead { db in try Self.snapshot(db) }
    }

    private static func snapshot(_ db: Database) throws -> WatchSnapshot? {
        guard let status = try WatchStatusRecord.fetchOne(db) else { return nil }

        let gradesByDiscipline = try Dictionary(
            grouping: WatchGradeRecord.order(Column("ordinal")).fetchAll(db),
            by: \.disciplineId
        )
        let disciplines = try WatchDisciplineRecord.order(Column("name")).fetchAll(db).map { record in
            WatchSnapshot.Discipline(
                id: record.id,
                code: record.code,
                name: record.name,
                teacherName: record.teacherName,
                hours: record.hours,
                missedHours: record.missedHours,
                grades: (gradesByDiscipline[record.id] ?? []).map {
                    WatchSnapshot.Grade(id: $0.id, label: $0.label, name: $0.name, value: $0.value, date: $0.date)
                },
                partialAverage: record.partialAverage,
                colorIndex: record.colorIndex
            )
        }

        let sessions = try WatchSessionRecord
            .order(Column("day"), Column("startMinute"))
            .fetchAll(db)
            .map { record in
                WidgetScheduleSnapshot.Session(
                    classId: record.classId,
                    day: record.day,
                    startMinute: record.startMinute,
                    endMinute: record.endMinute,
                    code: record.code,
                    title: record.title,
                    room: record.room,
                    teacherName: record.teacherName,
                    colorIndex: record.colorIndex,
                    disciplineId: record.disciplineId
                )
            }
        let topics = try WatchTopicRecord.fetchAll(db).map {
            WidgetScheduleSnapshot.Topic(classId: $0.classId, dayStamp: $0.dayStamp, subject: $0.subject)
        }

        var exam: WatchSnapshot.Exam?
        if let label = status.examLabel, let name = status.examDisciplineName, let date = status.examDate {
            exam = WatchSnapshot.Exam(label: label, disciplineName: name, date: date, time: status.examTime)
        }

        let attachmentsByMessage = try Dictionary(
            grouping: WatchMessageAttachmentRecord.order(Column("position")).fetchAll(db),
            by: \.messageId
        )
        let readIds = Set(try WatchMessageReadRecord.fetchAll(db).map(\.messageId))
        let messages = try WatchMessageRecord.order(Column("ordinal")).fetchAll(db).map { record in
            WatchSnapshot.Message(
                id: record.id,
                origin: MessageOrigin(rawValue: record.origin) ?? .campus,
                disciplineCode: record.disciplineCode,
                disciplineName: record.disciplineName,
                disciplineColorIndex: record.disciplineColorIndex,
                subject: record.subject,
                body: record.body,
                senderName: record.senderName,
                receivedAt: record.receivedAt,
                unread: record.unread && !readIds.contains(record.id),
                attachments: (attachmentsByMessage[record.id] ?? []).map {
                    WatchSnapshot.Message.Attachment(
                        id: $0.id,
                        kind: MessageAttachment.Kind(rawValue: $0.kind) ?? .other,
                        name: $0.name,
                        url: $0.url
                    )
                }
            )
        }

        return WatchSnapshot(
            schedule: WidgetScheduleSnapshot(
                semesterCode: status.semesterCode,
                sessions: sessions,
                topics: topics
            ),
            coefficient: status.coefficient,
            coefficientDelta: status.coefficientDelta,
            attendancePercent: status.attendancePercent,
            remainingAbsences: status.remainingAbsences,
            nextExam: exam,
            disciplines: disciplines,
            messages: messages,
            syncedAt: status.syncedAt
        )
    }
}

// MARK: - Database bootstrap

extension WatchStore {
    /// Opens (or creates) the on-disk store in the App Group container, so
    /// the watch app and its widget extension read the same file.
    static func onDisk() throws -> WatchStore {
        let folder = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: WidgetSnapshotStore.appGroupId)?
            .appending(path: "Database", directoryHint: .isDirectory)
            ?? URL.applicationSupportDirectory.appending(path: "Database", directoryHint: .isDirectory)
        try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
        return try WatchStore(path: folder.appending(path: "watch.sqlite").path)
    }

    init(path: String) throws {
        try self.init(migrating: DatabaseQueue(path: path))
    }

    /// In-memory store for previews and tests.
    static func inMemory() throws -> WatchStore {
        try WatchStore(migrating: DatabaseQueue())
    }

    init(migrating writer: any DatabaseWriter) throws {
        self.writer = writer
        try Self.migrator.migrate(writer)
    }

    private static var migrator: DatabaseMigrator {
        var migrator = DatabaseMigrator()
        #if DEBUG
        // The store is a disposable cache the next push rebuilds — dev
        // schema changes just start it over.
        migrator.eraseDatabaseOnSchemaChange = true
        #endif
        migrator.registerMigration("v1") { db in
            try db.create(table: "status") { t in
                t.primaryKey("id", .integer).check { $0 == 1 }
                t.column("semesterCode", .text)
                t.column("coefficient", .double)
                t.column("coefficientDelta", .double)
                t.column("attendancePercent", .integer)
                t.column("remainingAbsences", .integer)
                t.column("examLabel", .text)
                t.column("examDisciplineName", .text)
                t.column("examDate", .text)
                t.column("examTime", .text)
                t.column("syncedAt", .datetime).notNull()
            }
            try db.create(table: "disciplines") { t in
                t.primaryKey("id", .text)
                t.column("code", .text).notNull()
                t.column("name", .text).notNull()
                t.column("teacherName", .text)
                t.column("hours", .integer).notNull()
                t.column("missedHours", .integer).notNull()
                t.column("partialAverage", .double)
                t.column("colorIndex", .integer).notNull()
            }
            try db.create(table: "grades") { t in
                t.primaryKey("id", .text)
                t.column("disciplineId", .text).notNull()
                    .references("disciplines", onDelete: .cascade)
                t.column("ordinal", .integer).notNull()
                t.column("label", .text).notNull()
                t.column("name", .text).notNull()
                t.column("value", .double)
                t.column("date", .text)
            }
            try db.create(table: "scheduleSessions") { t in
                t.column("classId", .text).notNull()
                t.column("day", .integer).notNull()
                t.column("startMinute", .integer).notNull()
                t.column("endMinute", .integer)
                t.column("code", .text).notNull()
                t.column("title", .text).notNull()
                t.column("room", .text)
                t.column("teacherName", .text)
                t.column("colorIndex", .integer).notNull()
                t.column("disciplineId", .text)
                t.primaryKey(["classId", "day", "startMinute"])
            }
            try db.create(table: "scheduleTopics") { t in
                t.column("classId", .text).notNull()
                t.column("dayStamp", .text).notNull()
                t.column("subject", .text).notNull()
                t.primaryKey(["classId", "dayStamp"])
            }
        }
        migrator.registerMigration("v2-messages") { db in
            try db.create(table: "messages") { t in
                t.primaryKey("id", .text)
                // Push order (newest first), the display order.
                t.column("ordinal", .integer).notNull()
                t.column("origin", .text).notNull()
                t.column("disciplineCode", .text)
                t.column("disciplineName", .text)
                t.column("disciplineColorIndex", .integer)
                t.column("subject", .text)
                t.column("body", .text).notNull()
                t.column("senderName", .text).notNull()
                t.column("receivedAt", .datetime).notNull()
                t.column("unread", .boolean).notNull()
            }
            try db.create(table: "messageAttachments") { t in
                t.column("messageId", .text).notNull()
                    .references("messages", onDelete: .cascade)
                t.column("position", .integer).notNull()
                t.column("id", .text).notNull()
                t.column("kind", .text).notNull()
                t.column("name", .text)
                t.column("url", .text).notNull()
                t.primaryKey(["messageId", "position"])
            }
            // Local overlay, deliberately outside the full-replace cycle.
            try db.create(table: "messageReads") { t in
                t.primaryKey("messageId", .text)
                t.column("readAt", .datetime).notNull()
            }
        }
        return migrator
    }
}

// MARK: - Records

private struct WatchStatusRecord: Codable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "status"
    var id = 1
    var semesterCode: String?
    var coefficient: Double?
    var coefficientDelta: Double?
    var attendancePercent: Int?
    var remainingAbsences: Int?
    var examLabel: String?
    var examDisciplineName: String?
    var examDate: String?
    var examTime: String?
    var syncedAt: Date
}

private struct WatchDisciplineRecord: Codable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "disciplines"
    var id: String
    var code: String
    var name: String
    var teacherName: String?
    var hours: Int
    var missedHours: Int
    var partialAverage: Double?
    var colorIndex: Int
}

private struct WatchGradeRecord: Codable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "grades"
    var id: String
    var disciplineId: String
    var ordinal: Int
    var label: String
    var name: String
    var value: Double?
    var date: String?
}

private struct WatchSessionRecord: Codable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "scheduleSessions"
    var classId: String
    var day: Int
    var startMinute: Int
    var endMinute: Int?
    var code: String
    var title: String
    var room: String?
    var teacherName: String?
    var colorIndex: Int
    var disciplineId: String?
}

private struct WatchTopicRecord: Codable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "scheduleTopics"
    var classId: String
    var dayStamp: String
    var subject: String
}

private struct WatchMessageRecord: Codable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "messages"
    var id: String
    var ordinal: Int
    var origin: String
    var disciplineCode: String?
    var disciplineName: String?
    var disciplineColorIndex: Int?
    var subject: String?
    var body: String
    var senderName: String
    var receivedAt: Date
    var unread: Bool
}

private struct WatchMessageAttachmentRecord: Codable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "messageAttachments"
    var messageId: String
    var position: Int
    var id: String
    var kind: String
    var name: String?
    var url: String
}

private struct WatchMessageReadRecord: Codable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "messageReads"
    var messageId: String
    var readAt: Date
}

import GRDB

// One record per mirrored table. Column names are the property names; the
// schema lives in AppDatabase.swift.

struct SemesterRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "semesters"
    var id: String
    var code: String
    var description: String
    /// yyyy-MM-dd, compared lexicographically.
    var startDate: String
    var endDate: String
}

extension SemesterRecord {
    var domain: Semester {
        Semester(id: id, code: code, description: description, startDate: startDate, endDate: endDate)
    }
}

struct DisciplineRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "disciplines"
    var id: String
    var semesterId: String
    var code: String?
    var name: String
}

struct DisciplineOfferRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "disciplineOffers"
    var id: String
    var semesterId: String
    var disciplineId: String
}

struct ClassRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "classes"
    var id: String
    var semesterId: String
    var offerId: String
    var hours: Int
}

struct TeacherRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "teachers"
    var id: String
    var semesterId: String
    var name: String
}

struct ClassTeacherRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "classTeachers"
    var semesterId: String
    var classId: String
    var teacherId: String
}

struct SpaceRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "spaces"
    var id: String
    var semesterId: String
    var location: String
}

struct AllocationRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "allocations"
    var id: String
    var semesterId: String
    var classId: String
    var spaceId: String?
    /// Upstream day encoding: 0=Sunday … 6=Saturday.
    var day: Int?
    /// "HH:mm:ss".
    var startTime: String?
    var endTime: String?
}

struct StudentClassRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "studentClasses"
    var id: String
    var semesterId: String
    var classId: String
    /// Class-hours of absence (SAGRES totalFaltas).
    var missedClasses: Int?
}

struct StudentGradeRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "studentGrades"
    var id: String
    var semesterId: String
    var studentClassId: String
    var name: String?
    /// Compact evaluation label, e.g. "P2".
    var nameShort: String?
    var ordinal: Int
    /// Decimal string, e.g. "8.5"; null while ungraded.
    var value: String?
    /// yyyy-MM-dd.
    var date: String?
}

struct LectureRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "lectures"
    var id: String
    var semesterId: String
    var classId: String
    /// yyyy-MM-dd; null when not yet scheduled.
    var date: String?
    var subject: String?
}

struct MessageRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "messages"
    var id: String
    var subject: String?
    var content: String?
    var senderName: String?
    /// ISO8601 with fractional seconds, as sent on the wire. UTC, so it sorts
    /// lexicographically.
    var timestamp: String?
    var read: Bool?
}

struct SyncStateRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "syncState"
    var key: String
    var value: String
}

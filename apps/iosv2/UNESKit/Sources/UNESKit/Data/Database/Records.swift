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
    /// Enrolled disciplines as counted upstream — sizes the "download this
    /// semester" cards before the payload is mirrored. Only the semester
    /// list carries it.
    var disciplineCount: Int? = nil
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
    /// Catalog class-hours of the whole discipline (all groups combined).
    var hours: Int? = nil
    /// Owning department, e.g. "Departamento de Ciências Exatas".
    var department: String? = nil
    /// Upstream "ementa" — the syllabus text.
    var program: String? = nil
}

struct DisciplineOfferRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "disciplineOffers"
    var id: String
    var semesterId: String
    var disciplineId: String
    /// Offer-level class-hours; unlike group hours these are never replicated
    /// per group, so they're the safe multi-group fallback.
    var hours: Int? = nil
}

struct ClassRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "classes"
    var id: String
    var semesterId: String
    var offerId: String
    var hours: Int
    /// SAGRES group name, e.g. "T01" / "T01P01".
    var groupName: String? = nil
    /// Group kind, e.g. "Teórica" / "Prática".
    var type: String? = nil
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
    /// Room number/code within the module (upstream "numero").
    var location: String
    /// Campus label (upstream "pavilhao"); upstream coalesces absent to "".
    var campus: String? = nil
    /// Building/module label (upstream "localizacao") — a short code ("MT")
    /// or descriptive prose ("Pavilhão de aula padrão 2° andar").
    var modulo: String? = nil
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
    /// Class-hours of absence (SAGRES totalFaltas). Upstream reports it once
    /// per discipline and the backend replicates it onto every group row —
    /// read it with first-non-null per offer, never summed.
    var missedClasses: Int?
    /// Closing mean as a decimal string (e.g. "7.7"); null while ongoing.
    var finalGrade: String? = nil
    /// Authoritative pass/fail from upstream; null until the result is posted.
    var approved: Bool? = nil
    /// Whether a Prova Final evaluation exists for this class.
    var wentToFinals: Bool? = nil
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
    /// Upstream grade id. The backend replicates the same grade set onto
    /// every group row of a multi-group discipline; this is the dedup key.
    var platformId: String? = nil
    /// Evaluation weight as a decimal string.
    var weight: String? = nil
}

struct LectureRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "lectures"
    var id: String
    var semesterId: String
    var classId: String
    /// Upstream position within the class; orders undated lectures.
    var ordinal: Int? = nil
    /// yyyy-MM-dd; null when not yet scheduled.
    var date: String?
    var subject: String?
}

struct LectureMaterialRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "lectureMaterials"
    var id: String
    var semesterId: String
    var lectureId: String
    /// Upstream `description` — the display name of the material.
    var caption: String?
    var url: String
    var position: Int?
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
    /// "upstream" (SAGRES) or "app" (authored by the UNES team).
    var source: String? = nil
    /// SAGRES `perfilRemetente` — carried for fidelity; nothing in the stack
    /// interprets its values yet.
    var senderType: Int? = nil
    /// Server-side star, OR-merged across linked students. Star toggles flip
    /// it optimistically alongside the `messageStates` overlay; refreshes
    /// then re-stamp it with the acked value.
    var starred: Bool? = nil
}

struct MessageScopeRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "messageScopes"
    var id: String
    var messageId: String
    /// "university" / "coordination" / "course" / "class" / "personal" / "list".
    var scope: String
    var classId: String? = nil
    /// Populated only on class-scoped rows.
    var disciplineCode: String? = nil
    var disciplineName: String? = nil
}

struct MessageAttachmentRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "messageAttachments"
    var id: String
    var messageId: String
    /// "image" / "link" / "pdf" / "video" / "other".
    var kind: String
    var name: String?
    var url: String
    var position: Int?
}

/// Local read/star overlay. The backend has no mark-read or star endpoint, so
/// this table is written only by the app and never touched by sync — a
/// re-fetched message keeps its local state.
struct MessageStateRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "messageStates"
    var messageId: String
    /// ISO8601 stamp of the local read; nil while unread locally.
    var readAt: String?
    var starred: Bool
}

struct SyncStateRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "syncState"
    var key: String
    var value: String
}

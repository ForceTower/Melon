import SwiftUI

// MARK: - Models

struct GradeEntry: Identifiable, Hashable {
    let id = UUID()
    let label: String
    let title: String
    let date: String?
    let score: Double?
}

struct GradeSection: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let group: String?
    let grades: [GradeEntry]

    init(name: String, group: String? = nil, grades: [GradeEntry]) {
        self.name = name
        self.group = group
        self.grades = grades
    }
}

struct ClassEntry: Identifiable, Hashable {
    let id = UUID()
    let date: String?
    let title: String
    let attachments: Int?
    let past: Bool
    let isNext: Bool

    init(date: String?, title: String, attachments: Int?, past: Bool, isNext: Bool = false) {
        self.date = date
        self.title = title
        self.attachments = attachments
        self.past = past
        self.isNext = isNext
    }
}

enum AttachmentKind: String, Hashable {
    case pdf, slides, link, notes, other

    var label: String {
        switch self {
        case .pdf: return "PDF"
        case .slides: return "SLIDES"
        case .link: return "LINK"
        case .notes: return "NOTES"
        case .other: return "FILE"
        }
    }
}

struct Attachment: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let kind: AttachmentKind
    let added: String
    let group: String?
}

struct DisciplineGroup: Identifiable, Hashable {
    let id = UUID()
    let code: String
    let kind: String
    let prof: String
}

struct Discipline: Identifiable, Hashable {
    let id = UUID()
    let code: String
    let fullCode: String
    let title: String
    let dept: String
    let prof: String
    let color: Color
    let hours: Int
    let absences: Int
    let allowedAbsences: Int
    let sections: [GradeSection]
    let classes: [ClassEntry]
    let attachments: [Attachment]
    let ementa: String?
    let groups: [DisciplineGroup]
    let finalGrade: Double?

    // Keys carried for the detail-view handoff. Populated when the data
    // originates from the KMP feed; nil for preview fixtures.
    let disciplineId: String?
    let offerId: String?
    let semesterId: String?

    init(
        code: String,
        fullCode: String,
        title: String,
        dept: String,
        prof: String,
        color: Color,
        hours: Int,
        absences: Int,
        allowedAbsences: Int,
        sections: [GradeSection],
        classes: [ClassEntry] = [],
        attachments: [Attachment] = [],
        ementa: String? = nil,
        groups: [DisciplineGroup] = [],
        finalGrade: Double? = nil,
        disciplineId: String? = nil,
        offerId: String? = nil,
        semesterId: String? = nil
    ) {
        self.code = code
        self.fullCode = fullCode
        self.title = title
        self.dept = dept
        self.prof = prof
        self.color = color
        self.hours = hours
        self.absences = absences
        self.allowedAbsences = allowedAbsences
        self.sections = sections
        self.classes = classes
        self.attachments = attachments
        self.ementa = ementa
        self.groups = groups
        self.finalGrade = finalGrade
        self.disciplineId = disciplineId
        self.offerId = offerId
        self.semesterId = semesterId
    }
}

// MARK: - Derived helpers

enum AbsenceRisk { case ok, warn, risk }

struct DisciplineStatus: Hashable {
    enum Key: String { case approved, ongoing, low, failed, final, pending }
    let key: Key
    let label: String
}

struct NeededProjection {
    let required: Double
    let pending: Int
    let target: Double
}

extension Discipline {
    var allGrades: [GradeEntry] {
        sections.flatMap(\.grades)
    }

    /// Mean of all completed grades across sections. Nil when nothing's been released.
    var partialAverage: Double? {
        let scores = allGrades.compactMap(\.score)
        guard !scores.isEmpty else { return nil }
        return scores.reduce(0, +) / Double(scores.count)
    }

    var completedCount: Int {
        allGrades.lazy.filter { $0.score != nil }.count
    }

    var totalEvaluations: Int { allGrades.count }

    /// Required average on the remaining evaluations to close at `target`.
    /// Assumes equal weighting — matches `neededNext()` in the prototype.
    func needed(target: Double = 7.0) -> NeededProjection? {
        let done = allGrades.compactMap(\.score)
        let pending = allGrades.filter { $0.score == nil }
        guard !done.isEmpty, !pending.isEmpty else { return nil }
        let sumDone = done.reduce(0, +)
        let n = Double(allGrades.count)
        let required = (target * n - sumDone) / Double(pending.count)
        return NeededProjection(required: required, pending: pending.count, target: target)
    }

    var absenceRisk: AbsenceRisk {
        let ratio = Double(absences) / Double(allowedAbsences)
        if ratio >= 0.75 { return .risk }
        if ratio >= 0.50 { return .warn }
        return .ok
    }

    var status: DisciplineStatus {
        if let final = finalGrade {
            if final >= 7 { return .init(key: .approved, label: "aprovado") }
            if final >= 5 { return .init(key: .final, label: "prova final") }
            return .init(key: .failed, label: "reprovado")
        }
        guard let avg = partialAverage else {
            return .init(key: .pending, label: "sem notas")
        }
        if avg < 5.5 { return .init(key: .low, label: "nota baixa") }
        return .init(key: .ongoing, label: "em andamento")
    }

    var hasMultipleGroups: Bool { groups.count > 1 }

    /// "Te · Pr" style label used on the list card.
    var groupsShortLabel: String? {
        guard hasMultipleGroups else { return nil }
        return groups.map { String($0.kind.prefix(2)) }.joined(separator: " · ")
    }

    /// Next unreleased evaluation with a scheduled date.
    var nextEvaluation: GradeEntry? {
        allGrades.first { $0.score == nil && $0.date != nil }
    }

    /// Trend across the last two completed grades (nil when fewer than two).
    var trend: Double? {
        let scores = allGrades.compactMap(\.score)
        guard scores.count >= 2 else { return nil }
        return scores[scores.count - 1] - scores[scores.count - 2]
    }

    func sections(for groupCode: String?) -> [GradeSection] {
        guard let groupCode else { return sections }
        return sections.filter { $0.group == groupCode || $0.group == nil }
    }

    func attachments(for groupCode: String?) -> [Attachment] {
        guard let groupCode else { return attachments }
        return attachments.filter { $0.group == groupCode || $0.group == nil }
    }
}

// MARK: - Semester container

struct Semester: Identifiable, Hashable {
    let id: String      // display code, e.g. "2026.1"
    var disciplines: [Discipline]
    var isDownloaded: Bool
    var estimatedCount: Int?
    // Opaque DB primary key used by SyncSemesterUseCase. Nil for preview
    // fixtures; populated when the data comes from the KMP feed.
    let dbSemesterId: String?

    init(
        id: String,
        disciplines: [Discipline] = [],
        isDownloaded: Bool = true,
        estimatedCount: Int? = nil,
        dbSemesterId: String? = nil
    ) {
        self.id = id
        self.disciplines = disciplines
        self.isDownloaded = isDownloaded
        self.estimatedCount = estimatedCount
        self.dbSemesterId = dbSemesterId
    }
}

// MARK: - Score palette

/// The ramp used in the prototype: teal for excellent, ink for good,
/// amber for passing-but-shaky, coral for below the cutoff.
enum DisciplineScoreColor {
    static let excellent = Color(red: 0x3B/255, green: 0x9E/255, blue: 0xAE/255)
    static let caution   = Color(red: 0xD9/255, green: 0x85/255, blue: 0x2E/255)
    static let danger    = Color(red: 0xE8/255, green: 0x5D/255, blue: 0x4E/255)

    static func color(for score: Double?) -> Color {
        guard let score else { return UNESColor.ink4 }
        if score >= 8.5 { return excellent }
        if score >= 7.0 { return UNESColor.ink }
        if score >= 5.0 { return caution }
        return danger
    }
}

// MARK: - Dates

enum DisciplineDate {
    /// Pinned "today" to match the rest of the prototype's mock data.
    static let today = DateComponents(calendar: .current, year: 2026, month: 4, day: 18).date!

    /// Days from today to a dd/MM/yyyy string; nil when the string is missing or malformed.
    static func daysUntil(_ dateString: String?) -> Int? {
        guard let dateString else { return nil }
        let parts = dateString.split(separator: "/").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        let target = DateComponents(calendar: .current, year: parts[2], month: parts[1], day: parts[0]).date!
        let diff = Calendar.current.dateComponents([.day], from: today, to: target).day ?? 0
        return diff
    }
}

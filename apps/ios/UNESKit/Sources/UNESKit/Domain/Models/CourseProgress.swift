import Foundation

// MARK: - Course progress ("Progresso do curso") — how much is left to graduate

/// Status of one curriculum slot for the student. Raw values are the wire
/// vocabulary of `api/curriculum`.
///
/// `available` / `blocked` only ever arrive when the payload says
/// prerequisites are known; otherwise untouched slots are `notTaken`.
enum CurriculumEntryStatus: String, Equatable, Sendable, Codable, CaseIterable {
    case completed
    case inProgress = "in_progress"
    case withdrawn
    case failed
    case available
    case blocked
    case notTaken = "not_taken"

    /// Legend order and the sort inside a period: what is done, what is
    /// happening, what can be picked, then what is stuck.
    static let displayOrder: [CurriculumEntryStatus] = [
        .completed, .inProgress, .available, .withdrawn, .failed, .blocked, .notTaken,
    ]

    var displayRank: Int {
        Self.displayOrder.firstIndex(of: self) ?? Self.displayOrder.count
    }
}

/// The stable hour-type bucket a requirement belongs to. The university's own
/// label travels alongside it and is what the UI shows.
enum CurriculumRequirementKind: String, Equatable, Sendable, Codable {
    case required, elective, complementary, internship, capstone, `extension`, other
}

/// The curriculum version ("matriz curricular") the student is bound to.
struct CurriculumVersion: Equatable, Sendable, Identifiable {
    var id: String
    /// SAGRES's version identifier, a semester code: "20232".
    var code: String
    /// Verbatim upstream label, e.g. "BACHAREL E FORMAÇÃO DE PSICÓLOGO".
    var label: String
    /// yyyy-MM-dd — how current the transcribed source document is.
    var asOf: String
    var minPeriods: Int?
    var maxPeriods: Int?
    /// The source document is old enough that required hours may have moved.
    var stale: Bool

    var asOfDate: Date? {
        try? Date(asOf, strategy: .iso8601.year().month().day())
    }

    /// The version as students read it — "2024.1" for the semester code
    /// "20241". Anything not shaped like a semester code shows verbatim.
    var codeLabel: String {
        guard code.count == 5, code.allSatisfy(\.isNumber) else { return code }
        return "\(code.prefix(4)).\(code.suffix(1))"
    }
}

/// The headline numbers. `requiredHours` is nil only when no curriculum is
/// held for the course — then the screen shows hours with no denominator.
struct CurriculumSummary: Equatable, Sendable {
    var completedHours: Int
    var requiredHours: Int?
    /// Capped per requirement so surplus electives can't exceed 100.
    var percent: Double?
    /// Of `requiredHours`, how much sits in buckets whose completion is never
    /// observable (paper certificates).
    var excludedHours: Int
    /// Completed hours whose requirement is unknown — in the total, absent
    /// from the bars.
    var unclassifiedHours: Int
    var disciplinesCompleted: Int
    var disciplinesTotal: Int

    var remainingHours: Int? {
        requiredHours.map { max(0, $0 - completedHours) }
    }

    static let empty = CurriculumSummary(
        completedHours: 0, requiredHours: nil, percent: nil,
        excludedHours: 0, unclassifiedHours: 0,
        disciplinesCompleted: 0, disciplinesTotal: 0
    )
}

/// One hour-type bucket ("natureza") with the student's progress against it.
struct CurriculumRequirementProgress: Equatable, Sendable, Identifiable {
    /// Stable slug, e.g. "nucleo-comum".
    var code: String
    var kind: CurriculumRequirementKind
    /// The university's own pt-BR wording.
    var label: String
    /// Abbreviated for narrow rows; equals `label` when none was authored.
    var shortLabel: String
    /// First período this bucket appears in — explains a legitimate 0%.
    var startsAtPeriod: Int?
    var hoursRequired: Int
    var hoursCompleted: Int
    /// False when completion lives outside anything observable (atividades
    /// complementares): render as "not counted yet", not as zero progress.
    var derivable: Bool
    var percent: Double?

    var id: String { code }

    var hoursRemaining: Int {
        max(0, hoursRequired - hoursCompleted)
    }
}

/// One slot in the curriculum grid.
struct CurriculumEntry: Equatable, Sendable, Identifiable {
    var code: String
    /// Arrives abbreviated and upper-case from the university system.
    var name: String
    var hours: Int
    var credits: Int?
    /// nil for the elective pool — not scheduled in any período.
    var period: Int?
    /// Entries sharing a group must be taken in the same período.
    var coreqGroup: Int?
    var requirementCode: String?
    var status: CurriculumEntryStatus
    /// Must be completed first; these gate `available` / `blocked`.
    var prerequisites: [String]
    /// Taken alongside; never gates availability.
    var corequisites: [String]

    var id: String { code }
}

/// The entries scheduled for one período; `period == nil` is the elective
/// pool.
struct CurriculumPeriod: Equatable, Sendable, Identifiable {
    var period: Int?
    var entries: [CurriculumEntry]

    /// The pool sorts after every numbered período.
    var id: Int { period ?? .max }

    var hours: Int { entries.reduce(0) { $0 + $1.hours } }

    func count(_ status: CurriculumEntryStatus) -> Int {
        entries.filter { $0.status == status }.count
    }

    var completedCount: Int { count(.completed) }
}

/// The one payload behind the progress screen and the fluxograma, as
/// mirrored on disk.
struct CourseProgress: Equatable, Sendable {
    /// nil when no curriculum is held for the student's course.
    var curriculum: CurriculumVersion?
    var summary: CurriculumSummary
    var requirements: [CurriculumRequirementProgress]
    /// Sorted by período, elective pool last.
    var periods: [CurriculumPeriod]
    /// Where the student is now — the highest período they have work in.
    var currentPeriod: Int?
    /// False when too few entries carry prerequisites to claim availability.
    var prerequisitesKnown: Bool
    var syncedAt: Date

    var hasCurriculum: Bool { curriculum != nil }

    /// The total is known but the per-requirement split didn't come back.
    var hasBreakdown: Bool { !requirements.isEmpty }

    /// Numbered períodos only — what the rail, map and grid lay out.
    var scheduledPeriods: [CurriculumPeriod] {
        periods.filter { $0.period != nil }
    }

    var electivePool: CurriculumPeriod? {
        periods.first { $0.period == nil && !$0.entries.isEmpty }
    }

    var entries: [CurriculumEntry] {
        periods.flatMap(\.entries)
    }

    var entriesByCode: [String: CurriculumEntry] {
        Dictionary(entries.map { ($0.code, $0) }, uniquingKeysWith: { first, _ in first })
    }

    func entry(_ code: String) -> CurriculumEntry? {
        entries.first { $0.code == code }
    }

    func period(_ number: Int) -> CurriculumPeriod? {
        periods.first { $0.period == number }
    }

    /// The período the fluxograma opens on: the student's own, else the first.
    var landingPeriod: Int {
        currentPeriod ?? scheduledPeriods.first?.period ?? 1
    }

    /// Entries that list `code` as a prerequisite — what completing it unlocks.
    func unlocks(of code: String) -> [CurriculumEntry] {
        entries.filter { $0.prerequisites.contains(code) }
    }

    /// Disciplines to be taken alongside `code` — its own list plus every
    /// entry that names it, since the relation is symmetric in practice but
    /// upstream only ever writes one side.
    func corequisites(of code: String) -> [CurriculumEntry] {
        var seen: Set<String> = [code]
        var result: [CurriculumEntry] = []
        for other in (entry(code)?.corequisites ?? []).compactMap(entry) where seen.insert(other.code).inserted {
            result.append(other)
        }
        for other in entries where other.corequisites.contains(code) && seen.insert(other.code).inserted {
            result.append(other)
        }
        return result
    }

    /// The prerequisite chain through `code`, both ways: everything it
    /// depends on (transitively) and everything it eventually unlocks. Always
    /// contains `code` itself.
    func trail(through code: String) -> Set<String> {
        let byCode = entriesByCode
        var unlocksByCode: [String: [String]] = [:]
        for entry in entries {
            for prerequisite in entry.prerequisites {
                unlocksByCode[prerequisite, default: []].append(entry.code)
            }
        }
        var visited: Set<String> = [code]
        func walk(_ current: String, next: (String) -> [String]) {
            for candidate in next(current) where !visited.contains(candidate) {
                visited.insert(candidate)
                walk(candidate, next: next)
            }
        }
        walk(code) { byCode[$0]?.prerequisites ?? [] }
        walk(code) { unlocksByCode[$0] ?? [] }
        return visited
    }

    /// The requirement label for an entry, when the bucket is known.
    func requirementLabel(for entry: CurriculumEntry) -> String? {
        guard let code = entry.requirementCode else { return nil }
        return requirements.first { $0.code == code }?.shortLabel
    }
}

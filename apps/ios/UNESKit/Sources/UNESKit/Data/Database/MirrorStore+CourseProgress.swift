import Foundation
import GRDB

private let log = Log.scoped("MirrorStore")

// MARK: - Course progress (curriculum mirror)

/// One curriculum version of the student's course, in the server's order
/// (newest first). The bound one is named by `CurriculumProgressRecord`.
struct CurriculumVersionRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "curriculumVersions"
    var id: String
    var code: String
    var label: String
    var asOf: String
    var minPeriods: Int?
    var maxPeriods: Int?
    var stale: Bool
    var current: Bool
    var supersededByCode: String?
    var supersededByEffectiveFrom: String?
    var source: String?
    var completedHours: Int?
    var requiredHours: Int?
    var percent: Double?
    var fit: Double?
    var position: Int

    init(_ version: CurriculumVersion, position: Int) {
        id = version.id
        code = version.code
        label = version.label
        asOf = version.asOf
        minPeriods = version.minPeriods
        maxPeriods = version.maxPeriods
        stale = version.stale
        current = version.current
        supersededByCode = version.supersededBy?.code
        supersededByEffectiveFrom = version.supersededBy?.effectiveFrom
        source = version.source?.rawValue
        completedHours = version.completedHours
        requiredHours = version.requiredHours
        percent = version.percent
        fit = version.fit
        self.position = position
    }

    var domain: CurriculumVersion {
        CurriculumVersion(
            id: id, code: code, label: label, asOf: asOf,
            minPeriods: minPeriods, maxPeriods: maxPeriods, stale: stale,
            current: current,
            supersededBy: supersededByCode.map { CurriculumSupersession(code: $0, effectiveFrom: supersededByEffectiveFrom) },
            source: source.flatMap(CurriculumBindingSource.init(rawValue:)),
            completedHours: completedHours, requiredHours: requiredHours,
            percent: percent, fit: fit
        )
    }
}

/// The student's headline numbers plus the payload-level facts that aren't
/// about any one entry. Single row keyed by `current`; `curriculumId` is nil
/// when no curriculum is held for the course (hours with no denominator).
struct CurriculumProgressRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "curriculumProgress"
    static let currentKey = "current"

    var key = CurriculumProgressRecord.currentKey
    var curriculumId: String?
    var completedHours: Int
    var requiredHours: Int?
    var percent: Double?
    var excludedHours: Int
    var unclassifiedHours: Int
    var disciplinesCompleted: Int
    var disciplinesTotal: Int
    var currentPeriod: Int?
    var prerequisitesKnown: Bool
    var syncedAt: String
    var approvedHours: Int

    init(_ progress: CourseProgress) {
        curriculumId = progress.curriculum?.id
        completedHours = progress.summary.completedHours
        requiredHours = progress.summary.requiredHours
        percent = progress.summary.percent
        excludedHours = progress.summary.excludedHours
        unclassifiedHours = progress.summary.unclassifiedHours
        disciplinesCompleted = progress.summary.disciplinesCompleted
        disciplinesTotal = progress.summary.disciplinesTotal
        currentPeriod = progress.currentPeriod
        prerequisitesKnown = progress.prerequisitesKnown
        syncedAt = progress.syncedAt.formatted(MirrorStore.timestampFormat)
        approvedHours = progress.approvedHours
    }

    var summary: CurriculumSummary {
        CurriculumSummary(
            completedHours: completedHours, requiredHours: requiredHours, percent: percent,
            excludedHours: excludedHours, unclassifiedHours: unclassifiedHours,
            disciplinesCompleted: disciplinesCompleted, disciplinesTotal: disciplinesTotal
        )
    }
}

struct CurriculumRequirementRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "curriculumRequirements"
    var curriculumId: String
    var code: String
    var kind: String
    var label: String
    var shortLabel: String
    var startsAtPeriod: Int?
    var hoursRequired: Int
    var hoursCompleted: Int
    var derivable: Bool
    var percent: Double?
    var position: Int

    init(_ requirement: CurriculumRequirementProgress, curriculumId: String, position: Int) {
        self.curriculumId = curriculumId
        code = requirement.code
        kind = requirement.kind.rawValue
        label = requirement.label
        shortLabel = requirement.shortLabel
        startsAtPeriod = requirement.startsAtPeriod
        hoursRequired = requirement.hoursRequired
        hoursCompleted = requirement.hoursCompleted
        derivable = requirement.derivable
        percent = requirement.percent
        self.position = position
    }

    var domain: CurriculumRequirementProgress {
        CurriculumRequirementProgress(
            code: code,
            // A kind this build doesn't know is a newer server bucket —
            // generic, not dropped.
            kind: CurriculumRequirementKind(rawValue: kind) ?? .other,
            label: label, shortLabel: shortLabel, startsAtPeriod: startsAtPeriod,
            hoursRequired: hoursRequired, hoursCompleted: hoursCompleted,
            derivable: derivable, percent: percent
        )
    }
}

struct CurriculumEntryRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "curriculumEntries"
    var curriculumId: String
    var code: String
    var name: String
    var hours: Int
    var credits: Int?
    var period: Int?
    var coreqGroup: Int?
    var requirementCode: String?
    var status: String
    var position: Int

    init(_ entry: CurriculumEntry, curriculumId: String, position: Int) {
        self.curriculumId = curriculumId
        code = entry.code
        name = entry.name
        hours = entry.hours
        credits = entry.credits
        period = entry.period
        coreqGroup = entry.coreqGroup
        requirementCode = entry.requirementCode
        status = entry.status.rawValue
        self.position = position
    }

    func domain(prerequisites: [String], corequisites: [String]) -> CurriculumEntry {
        CurriculumEntry(
            code: code, name: name, hours: hours, credits: credits, period: period,
            coreqGroup: coreqGroup, requirementCode: requirementCode,
            // Anything this build can't name is, at most, not completed.
            status: CurriculumEntryStatus(rawValue: status) ?? .notTaken,
            prerequisites: prerequisites, corequisites: corequisites
        )
    }
}

/// One edge of the grid: `entryCode` needs `requiresCode` first
/// (`prerequisite`) or alongside (`corequisite`).
struct CurriculumPrerequisiteRecord: Codable, Equatable, Sendable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "curriculumPrerequisites"
    static let prerequisiteKind = "prerequisite"
    static let corequisiteKind = "corequisite"

    var curriculumId: String
    var entryCode: String
    var requiresCode: String
    var kind: String
    var position: Int
}

extension MirrorStore {
    /// Replaces the mirrored curriculum with one refresh result, whole and in
    /// one transaction: a mid-run failure leaves the previous payload intact,
    /// never a half-written grid.
    func applyCourseProgress(_ progress: CourseProgress) async throws {
        do {
            try await writer.write { db in
                try Self.clearCourseProgress(db)
                try CurriculumProgressRecord(progress).insert(db)
                // The server lists the bound version among the alternatives;
                // should it ever not, it still has to be readable back.
                var versions = progress.availableVersions
                if let curriculum = progress.curriculum, !versions.contains(where: { $0.id == curriculum.id }) {
                    versions.insert(curriculum, at: 0)
                }
                for (position, version) in versions.enumerated() {
                    try CurriculumVersionRecord(version, position: position).insert(db)
                }
                guard let curriculum = progress.curriculum else { return }
                for (position, requirement) in progress.requirements.enumerated() {
                    try CurriculumRequirementRecord(requirement, curriculumId: curriculum.id, position: position)
                        .insert(db)
                }
                var position = 0
                for period in progress.periods {
                    for entry in period.entries {
                        try CurriculumEntryRecord(entry, curriculumId: curriculum.id, position: position).insert(db)
                        position += 1
                        for (index, code) in entry.prerequisites.enumerated() {
                            try CurriculumPrerequisiteRecord(
                                curriculumId: curriculum.id, entryCode: entry.code, requiresCode: code,
                                kind: CurriculumPrerequisiteRecord.prerequisiteKind, position: index
                            ).insert(db)
                        }
                        for (index, code) in entry.corequisites.enumerated() {
                            try CurriculumPrerequisiteRecord(
                                curriculumId: curriculum.id, entryCode: entry.code, requiresCode: code,
                                kind: CurriculumPrerequisiteRecord.corequisiteKind, position: index
                            ).insert(db)
                        }
                    }
                }
            }
            log.info("""
            upsert curriculum id=\(progress.curriculum?.id ?? "<none>") \
            versions=\(progress.availableVersions.count) requirements=\(progress.requirements.count) entries=\(progress.entries.count)
            """)
        } catch {
            log.error("apply curriculum failed", error: error)
            throw error
        }
    }

    /// The mirrored progress; nil until the first successful refresh lands.
    func cachedCourseProgress() async throws -> CourseProgress? {
        try await writer.read { db in try Self.courseProgress(db) }
    }

    /// Emits the mirrored progress on subscription and again after every
    /// write that changes it — refreshes and the logout wipe.
    func courseProgressUpdates() -> AsyncValueObservation<CourseProgress?> {
        ValueObservation
            .tracking { db in try Self.courseProgress(db) }
            .values(in: writer)
    }

    static func clearCourseProgress(_ db: Database) throws {
        try CurriculumProgressRecord.deleteAll(db)
        try CurriculumVersionRecord.deleteAll(db)
        try CurriculumRequirementRecord.deleteAll(db)
        try CurriculumEntryRecord.deleteAll(db)
        try CurriculumPrerequisiteRecord.deleteAll(db)
    }

    private static func courseProgress(_ db: Database) throws -> CourseProgress? {
        guard let progressRecord = try CurriculumProgressRecord.fetchOne(db, key: CurriculumProgressRecord.currentKey)
        else { return nil }
        let syncedAt = (try? Date(progressRecord.syncedAt, strategy: timestampFormat)) ?? .distantPast
        let versions = try CurriculumVersionRecord.order(Column("position")).fetchAll(db).map(\.domain)

        guard let curriculumId = progressRecord.curriculumId,
              let curriculum = versions.first(where: { $0.id == curriculumId })
        else {
            return CourseProgress(
                curriculum: nil, summary: progressRecord.summary, requirements: [], periods: [],
                currentPeriod: progressRecord.currentPeriod,
                prerequisitesKnown: progressRecord.prerequisitesKnown, syncedAt: syncedAt,
                availableVersions: versions, approvedHours: progressRecord.approvedHours
            )
        }

        let requirements = try CurriculumRequirementRecord
            .filter(Column("curriculumId") == curriculumId)
            .order(Column("position"))
            .fetchAll(db)
            .map(\.domain)

        var prerequisites: [String: [String]] = [:]
        var corequisites: [String: [String]] = [:]
        let edges = try CurriculumPrerequisiteRecord
            .filter(Column("curriculumId") == curriculumId)
            .order(Column("position"))
            .fetchAll(db)
        for edge in edges {
            if edge.kind == CurriculumPrerequisiteRecord.corequisiteKind {
                corequisites[edge.entryCode, default: []].append(edge.requiresCode)
            } else {
                prerequisites[edge.entryCode, default: []].append(edge.requiresCode)
            }
        }

        let entries = try CurriculumEntryRecord
            .filter(Column("curriculumId") == curriculumId)
            .order(Column("position"))
            .fetchAll(db)
            .map { record in
                record.domain(
                    prerequisites: prerequisites[record.code] ?? [],
                    corequisites: corequisites[record.code] ?? []
                )
            }

        // Position order already groups a período's entries together; the
        // pool (nil período) sorts last.
        var periods: [CurriculumPeriod] = []
        for entry in entries {
            if let index = periods.firstIndex(where: { $0.period == entry.period }) {
                periods[index].entries.append(entry)
            } else {
                periods.append(CurriculumPeriod(period: entry.period, entries: [entry]))
            }
        }
        periods.sort { $0.id < $1.id }

        return CourseProgress(
            curriculum: curriculum,
            summary: progressRecord.summary,
            requirements: requirements,
            periods: periods,
            currentPeriod: progressRecord.currentPeriod,
            prerequisitesKnown: progressRecord.prerequisitesKnown,
            syncedAt: syncedAt,
            availableVersions: versions,
            approvedHours: progressRecord.approvedHours
        )
    }
}

import ComposableArchitecture
import Foundation

private let log = Log.scoped("CourseProgressRepository")

extension CourseProgressRepository: DependencyKey {
    static let liveValue = CourseProgressRepository(
        cached: {
            @Dependency(\.database) var wrappedDatabase
            return try await MirrorStore(writer: wrappedDatabase).cachedCourseProgress()
        },
        refresh: {
            @Dependency(\.apiClient) var wrappedClient
            let apiClient = wrappedClient
            log.debug("refresh start")
            try await apply("refresh") {
                try await apiClient.get(from: "api/curriculum")
            }
        },
        selectVersion: { curriculumId in
            @Dependency(\.apiClient) var wrappedClient
            let apiClient = wrappedClient
            log.info("select version id=\(curriculumId)")
            try await apply("select version") {
                try await apiClient.put(at: "api/curriculum/version", body: VersionSelectionBody(curriculumId: curriculumId))
            }
        },
        resetVersion: {
            @Dependency(\.apiClient) var wrappedClient
            let apiClient = wrappedClient
            log.info("reset version to automatic")
            try await apply("reset version") {
                try await apiClient.delete(CurriculumPayloadDTO.self, "api/curriculum/version")
            }
        },
        observe: {
            @Dependency(\.database) var wrappedDatabase
            let mirror = MirrorStore(writer: wrappedDatabase)
            log.debug("observe subscribed")
            return AsyncStream { continuation in
                let task = Task {
                    // Observation only fails if the database itself is gone;
                    // ending the stream is all there is to do.
                    do {
                        for try await progress in mirror.courseProgressUpdates() {
                            continuation.yield(progress)
                        }
                    } catch {
                        log.error("observe failed", error: error)
                    }
                    continuation.finish()
                }
                continuation.onTermination = { _ in task.cancel() }
            }
        }
    )

    /// Every write path returns the rebuilt payload, so fetching and mirroring
    /// are the same move for a refresh, a pick and a reset.
    private static func apply(
        _ operation: String,
        _ fetch: @Sendable () async throws -> CurriculumPayloadDTO
    ) async throws {
        @Dependency(\.database) var wrappedDatabase
        @Dependency(\.date) var wrappedDate
        let mirror = MirrorStore(writer: wrappedDatabase)
        do {
            let progress = try await fetch().domain(syncedAt: wrappedDate.now)
            try await mirror.applyCourseProgress(progress)
            log.info("""
            \(operation) ok curriculum=\(progress.curriculum?.code ?? "<none>") \
            source=\(progress.curriculum?.source?.rawValue ?? "<none>") versions=\(progress.availableVersions.count) \
            completed=\(progress.summary.completedHours)h required=\(progress.summary.requiredHours.map(String.init) ?? "?")h \
            entries=\(progress.entries.count)
            """)
        } catch {
            logFailure(operation, error: error)
            throw error
        }
    }

    private static func logFailure(_ operation: String, error: Error) {
        switch error {
        case APIError.server(401, _):
            log.warn("\(operation) unauthorized")
        case let APIError.server(status, message):
            log.warn("\(operation) server \(status) message=\(message ?? "<none>")")
        case APIError.emptyEnvelope:
            log.warn("\(operation) 2xx envelope had null data")
        case is URLError:
            log.warn("\(operation) transport failure", error: error)
        default:
            log.error("\(operation) failed", error: error)
        }
    }
}

// MARK: - DTOs (`api/curriculum`)

private struct VersionSelectionBody: Encodable {
    var curriculumId: String
}

struct CurriculumPayloadDTO: Decodable {
    struct Supersession: Decodable {
        var code: String
        var effectiveFrom: String? = nil

        var domain: CurriculumSupersession {
            CurriculumSupersession(code: code, effectiveFrom: effectiveFrom)
        }
    }

    struct Version: Decodable {
        var id: String
        var code: String
        var label: String
        var asOf: String
        var minPeriods: Int? = nil
        var maxPeriods: Int? = nil
        var stale: Bool? = nil
        var current: Bool? = nil
        var supersededBy: Supersession? = nil
        var source: String? = nil
        var completedHours: Int? = nil
        var requiredHours: Int? = nil
        var percent: Double? = nil
        var fit: Double? = nil

        var domain: CurriculumVersion {
            CurriculumVersion(
                id: id, code: code, label: label, asOf: asOf,
                minPeriods: minPeriods, maxPeriods: maxPeriods, stale: stale ?? false,
                current: current ?? false,
                supersededBy: supersededBy?.domain,
                source: source.flatMap(CurriculumBindingSource.init(rawValue:)),
                completedHours: completedHours, requiredHours: requiredHours,
                percent: percent, fit: fit
            )
        }
    }

    struct Summary: Decodable {
        var completedHours: Int
        var requiredHours: Int? = nil
        var percent: Double? = nil
        var excludedHours: Int? = nil
        var unclassifiedHours: Int? = nil
        var disciplinesCompleted: Int? = nil
        var disciplinesTotal: Int? = nil

        var domain: CurriculumSummary {
            CurriculumSummary(
                completedHours: completedHours, requiredHours: requiredHours, percent: percent,
                excludedHours: excludedHours ?? 0, unclassifiedHours: unclassifiedHours ?? 0,
                disciplinesCompleted: disciplinesCompleted ?? 0, disciplinesTotal: disciplinesTotal ?? 0
            )
        }
    }

    struct Requirement: Decodable {
        var code: String
        var kind: String
        var label: String
        var shortLabel: String? = nil
        var startsAtPeriod: Int? = nil
        var hoursRequired: Int
        var hoursCompleted: Int
        var derivable: Bool? = nil
        var percent: Double? = nil

        var domain: CurriculumRequirementProgress {
            CurriculumRequirementProgress(
                code: code,
                kind: CurriculumRequirementKind(rawValue: kind) ?? .other,
                label: label,
                shortLabel: shortLabel ?? label,
                startsAtPeriod: startsAtPeriod,
                hoursRequired: hoursRequired,
                hoursCompleted: hoursCompleted,
                derivable: derivable ?? true,
                percent: percent
            )
        }
    }

    struct Entry: Decodable {
        var code: String
        var name: String
        var hours: Int
        var credits: Int? = nil
        var period: Int? = nil
        var coreqGroup: Int? = nil
        var requirementCode: String? = nil
        var status: String
        var prerequisites: [String]? = nil
        var corequisites: [String]? = nil

        var domain: CurriculumEntry {
            CurriculumEntry(
                code: code, name: name, hours: hours, credits: credits, period: period,
                coreqGroup: coreqGroup, requirementCode: requirementCode,
                status: CurriculumEntryStatus(rawValue: status) ?? .notTaken,
                prerequisites: prerequisites ?? [], corequisites: corequisites ?? []
            )
        }
    }

    struct Period: Decodable {
        var period: Int? = nil
        var entries: [Entry]

        var domain: CurriculumPeriod {
            CurriculumPeriod(period: period, entries: entries.map(\.domain))
        }
    }

    var curriculum: Version? = nil
    var availableVersions: [Version]? = nil
    var approvedHours: Int? = nil
    var summary: Summary
    var requirements: [Requirement]? = nil
    var periods: [Period]? = nil
    var currentPeriod: Int? = nil
    var prerequisitesKnown: Bool? = nil

    func domain(syncedAt: Date) -> CourseProgress {
        CourseProgress(
            curriculum: curriculum?.domain,
            summary: summary.domain,
            requirements: (requirements ?? []).map(\.domain),
            periods: (periods ?? []).map(\.domain).sorted { $0.id < $1.id },
            currentPeriod: currentPeriod,
            prerequisitesKnown: prerequisitesKnown ?? false,
            syncedAt: syncedAt,
            availableVersions: (availableVersions ?? []).map(\.domain),
            approvedHours: approvedHours ?? summary.completedHours
        )
    }
}

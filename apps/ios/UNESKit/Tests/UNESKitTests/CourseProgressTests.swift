import Foundation
import Testing

@testable import UNESKit

struct CourseProgressTests {
    @Test
    func mirrorRoundTripsTheWholeCurriculum() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        #expect(try await store.cachedCourseProgress() == nil)

        let progress = CourseProgress.preview()
        try await store.applyCourseProgress(progress)

        let cached = try await store.cachedCourseProgress()
        #expect(cached == progress)
        #expect(cached?.entries.count == 63)
        #expect(cached?.entry("CHF345")?.prerequisites == ["CHF344"])
        #expect(cached?.availableVersions.map(\.code) == ["20232", "20122", "20051"])
        #expect(cached?.availableVersions[1].supersededBy?.code == "20232")
        #expect(cached?.approvedHours == 1500)
    }

    @Test
    func standingFollowsTheSuccession() {
        let progress = CourseProgress.preview()
        let versions = progress.availableVersions
        #expect(progress.standing(of: versions[0]) == .current)
        #expect(progress.standing(of: versions[1]) == .previous)
        #expect(progress.standing(of: versions[2]) == .retired)
        #expect(progress.canPickVersion)

        // Nothing bound but versions on file — the student has to pick.
        let unbound = CourseProgress.preview(curriculum: false)
        #expect(unbound.canPickVersion)

        // One version is nothing to pick between.
        var single = progress
        single.availableVersions = [versions[0]]
        #expect(!single.canPickVersion)
    }

    @Test
    func mirrorKeepsAnUnlistedBoundVersionReadable() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        var progress = CourseProgress.preview()
        progress.availableVersions.removeFirst()
        try await store.applyCourseProgress(progress)
        let cached = try await store.cachedCourseProgress()
        #expect(cached?.curriculum?.code == "20232")
        #expect(cached?.availableVersions.first?.code == "20232")
    }

    @Test
    func mirrorReplacesInsteadOfAccumulating() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        try await store.applyCourseProgress(.preview())

        // A refresh that dropped the binding leaves only the numerator.
        let noCurriculum = CourseProgress.preview(curriculum: false)
        try await store.applyCourseProgress(noCurriculum)

        let cached = try await store.cachedCourseProgress()
        #expect(cached == noCurriculum)
        #expect(cached?.hasCurriculum == false)
        #expect(cached?.entries.isEmpty == true)

        // And back again — the grid comes back whole, no leftovers.
        try await store.applyCourseProgress(.preview(stale: true))
        let restored = try await store.cachedCourseProgress()
        #expect(restored?.curriculum?.stale == true)
        #expect(restored?.entries.count == 63)
        #expect(restored?.requirements.count == 8)
    }

    @Test
    func wipeClearsTheCurriculum() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        try await store.applyCourseProgress(.preview())
        try await store.wipe()
        #expect(try await store.cachedCourseProgress() == nil)
    }

    @Test
    func trailWalksBothDirections() {
        let progress = CourseProgress.preview()
        // CHF288 → CHF299 → CHF344 → CHF345 → CHF322 → CHF346, plus the
        // other branches CHF299 unlocks (CHF302 → CHF353 → CHF354 → CHF360 →
        // CHF340 → CHF350).
        let trail = progress.trail(through: "CHF344")
        #expect(trail.contains("CHF344"))
        #expect(trail.contains("CHF299"))
        #expect(trail.contains("CHF288"))
        #expect(trail.contains("CHF345"))
        #expect(trail.contains("CHF322"))
        #expect(trail.contains("CHF346"))
        // Siblings hanging off the same prerequisite are not on the trail.
        #expect(!trail.contains("CHF302"))
        #expect(!trail.contains("CHF289"))
    }

    @Test
    func unlocksAreTheInverseOfPrerequisites() {
        let progress = CourseProgress.preview()
        #expect(progress.unlocks(of: "CHF299").map(\.code).sorted() == ["CHF302", "CHF344"])
        #expect(progress.unlocks(of: "CHF363").isEmpty)
    }

    @Test
    func corequisitesReadBothDirections() {
        var progress = CourseProgress.preview()
        // Upstream only writes one side: CHF289 lists CHF288 alongside.
        progress.periods[0].entries[0].corequisites = ["CHF288"]
        #expect(progress.corequisites(of: "CHF289").map(\.code) == ["CHF288"])
        #expect(progress.corequisites(of: "CHF288").map(\.code) == ["CHF289"])
        #expect(progress.corequisites(of: "SAU281").isEmpty)
    }

    @Test
    func payloadDecodesTheWireShape() throws {
        let json = """
        {
          "curriculum": {"id": "c1", "code": "20232", "label": "BACHAREL", "asOf": "2024-03-04", "minPeriods": 10, "maxPeriods": 15, "stale": false, "current": true, "supersededBy": null, "source": "manual", "completedHours": 1500, "requiredHours": 4040, "percent": 37.13, "fit": 97.5},
          "availableVersions": [
            {"id": "c1", "code": "20232", "label": "BACHAREL", "asOf": "2024-03-04", "minPeriods": 10, "maxPeriods": 15, "stale": false, "current": true, "supersededBy": null, "source": "manual", "completedHours": 1500, "requiredHours": 4040, "percent": 37.13, "fit": 97.5},
            {"id": "c0", "code": "20122", "label": "BACHAREL", "asOf": "2013-02-18", "minPeriods": 10, "maxPeriods": 15, "stale": true, "current": false, "supersededBy": {"code": "20232", "codeLabel": "2023.2", "effectiveFrom": "20232", "effectiveFromLabel": "2023.2"}, "source": null, "completedHours": 780, "requiredHours": 3890, "percent": 20.05, "fit": 52}
          ],
          "approvedHours": 1540,
          "summary": {"completedHours": 1500, "requiredHours": 4040, "percent": 37.13, "excludedHours": 200, "unclassifiedHours": 0, "disciplinesCompleted": 13, "disciplinesTotal": 2},
          "requirements": [
            {"code": "nucleo-comum", "kind": "required", "label": "Núcleo Comum", "shortLabel": "Núcleo Comum", "startsAtPeriod": 1, "hoursRequired": 2820, "hoursCompleted": 1500, "derivable": true, "percent": 53.19},
            {"code": "ac", "kind": "complementary", "label": "Atividade Complementar", "shortLabel": "Atividade Complementar", "startsAtPeriod": null, "hoursRequired": 200, "hoursCompleted": 0, "derivable": false, "percent": 0}
          ],
          "periods": [
            {"period": null, "entries": [{"code": "OPT1", "name": "OPTATIVA", "hours": 45, "credits": null, "period": null, "coreqGroup": null, "requirementCode": null, "status": "not_taken", "prerequisites": [], "corequisites": []}]},
            {"period": 1, "entries": [{"code": "CHF289", "name": "HIST.", "hours": 60, "credits": 4, "period": 1, "coreqGroup": null, "requirementCode": "nucleo-comum", "status": "completed", "prerequisites": [], "corequisites": ["CHF288"]}]}
          ],
          "currentPeriod": 3,
          "prerequisitesKnown": true
        }
        """
        let dto = try JSONDecoder().decode(CurriculumPayloadDTO.self, from: Data(json.utf8))
        let progress = dto.domain(syncedAt: Date(timeIntervalSince1970: 0))

        #expect(progress.curriculum?.code == "20232")
        #expect(progress.curriculum?.isManualPick == true)
        #expect(progress.curriculum?.fit == 97.5)
        #expect(progress.availableVersions.count == 2)
        #expect(progress.availableVersions[1].supersededBy == CurriculumSupersession(code: "20232", effectiveFrom: "20232"))
        #expect(progress.standing(of: progress.availableVersions[1]) == .previous)
        #expect(progress.approvedHours == 1540)
        #expect(progress.summary.percent == 37.13)
        #expect(progress.requirements.map(\.derivable) == [true, false])
        // Numbered períodos first, the elective pool last.
        #expect(progress.periods.map(\.period) == [1, nil])
        #expect(progress.entry("CHF289")?.corequisites == ["CHF288"])
        #expect(progress.electivePool?.entries.first?.code == "OPT1")
        #expect(progress.currentPeriod == 3)
    }

    @Test
    func mirrorKeepsTheElectivePool() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        var progress = CourseProgress.preview()
        progress.periods.append(CurriculumPeriod(period: nil, entries: [
            CurriculumEntry(
                code: "OPT1", name: "OPTATIVA", hours: 45, credits: nil, period: nil, coreqGroup: nil,
                requirementCode: "optativa", status: .notTaken, prerequisites: [], corequisites: ["CHF289"]
            ),
        ]))
        try await store.applyCourseProgress(progress)
        let cached = try await store.cachedCourseProgress()
        #expect(cached == progress)
        #expect(cached?.electivePool?.entries.first?.corequisites == ["CHF289"])
    }
}

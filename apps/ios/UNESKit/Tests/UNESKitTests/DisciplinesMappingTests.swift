import Foundation
import GRDB
import Testing

@testable import UNESKit

/// Fixture semester with the shapes the backend actually produces: a
/// multi-group discipline (theory + practice) whose grades and totalFaltas
/// are replicated onto every group row, plus a plain single-group one.
enum DisciplinesFixtures {
    static func payload(
        semesterId: String = "sem1",
        code: String = "20261",
        startDate: String = "2026-03-01",
        endDate: String = "2026-07-31",
        idPrefix p: String = ""
    ) -> SemesterPayloadDTO {
        SemesterPayloadDTO(
            semester: .init(
                id: semesterId, code: code, description: "Semestre \(code)",
                startDate: startDate, endDate: endDate
            ),
            disciplines: [
                .init(id: "d-alg", code: "EXA805", name: "Algoritmos II", hours: 60),
                .init(id: "d-fis", code: "EXA412", name: "Física II", hours: 75),
            ],
            disciplineOffers: [
                .init(id: "\(p)o-alg", disciplineId: "d-alg"),
                .init(id: "\(p)o-fis", disciplineId: "d-fis"),
            ],
            classes: [
                .init(id: "\(p)c-alg", offerId: "\(p)o-alg", hours: 60, groupName: "T01", type: "Teórica"),
                .init(id: "\(p)c-fis-t", offerId: "\(p)o-fis", hours: 45, groupName: "T01", type: "Teórica"),
                .init(id: "\(p)c-fis-p", offerId: "\(p)o-fis", hours: 30, groupName: "T01P01", type: "Prática"),
            ],
            teachers: [
                .init(id: "t-alg", name: "Camila Ribeiro"),
                .init(id: "t-fis-t", name: "João Nascimento"),
                .init(id: "t-fis-p", name: "Beatriz Sampaio"),
            ],
            classTeachers: [
                .init(classId: "\(p)c-alg", teacherId: "t-alg"),
                .init(classId: "\(p)c-fis-t", teacherId: "t-fis-t"),
                .init(classId: "\(p)c-fis-p", teacherId: "t-fis-p"),
            ],
            spaces: [],
            allocations: [],
            studentClasses: [
                .init(id: "\(p)sc-alg", classId: "\(p)c-alg", missedClasses: 2),
                .init(id: "\(p)sc-fis-t", classId: "\(p)c-fis-t", missedClasses: 4),
                .init(id: "\(p)sc-fis-p", classId: "\(p)c-fis-p", missedClasses: 4),
            ],
            studentGrades:
                fisGrades(studentClassId: "\(p)sc-fis-t", rowPrefix: "\(p)gt") +
                fisGrades(studentClassId: "\(p)sc-fis-p", rowPrefix: "\(p)gp") + [
                    .init(
                        id: "\(p)g-alg-1", studentClassId: "\(p)sc-alg", name: "Prova 1", nameShort: "AV1",
                        ordinal: 1, value: "8.3", date: "2026-03-31", platformId: "pg-alg-1", weight: "5"
                    ),
                    .init(
                        id: "\(p)g-alg-2", studentClassId: "\(p)sc-alg", name: "Prova 2", nameShort: "AV2",
                        ordinal: 2, value: nil, date: "2026-05-12", platformId: "pg-alg-2", weight: "5"
                    ),
                ],
            lectures: [
                .init(id: "\(p)l1", classId: "\(p)c-alg", date: "2026-04-09", subject: "Filas"),
            ]
        )
    }

    /// The same grade set the backend copies onto every group of the offer —
    /// identical platform ids, distinct row ids.
    private static func fisGrades(
        studentClassId: String,
        rowPrefix: String
    ) -> [SemesterPayloadDTO.StudentGradeDTO] {
        [
            .init(
                id: "\(rowPrefix)-1", studentClassId: studentClassId, name: "I Avaliação", nameShort: "AV1",
                ordinal: 1, value: "6.8", date: "2026-04-07", platformId: "pg-fis-1", weight: "2.5"
            ),
            .init(
                id: "\(rowPrefix)-2", studentClassId: studentClassId, name: "II Avaliação", nameShort: "AV2",
                ordinal: 2, value: nil, date: "2026-05-19", platformId: "pg-fis-2", weight: "2.5"
            ),
            .init(
                id: "\(rowPrefix)-3", studentClassId: studentClassId, name: "Relatórios", nameShort: "LAB",
                ordinal: 3, value: "9.0", date: nil, platformId: "pg-fis-3", weight: "5"
            ),
        ]
    }
}

struct DisciplinesMappingTests {
    let calendar = Calendar.current

    /// 2026-04-18 12:00 local — inside the fixture semester.
    var now: Date {
        calendar.date(from: DateComponents(year: 2026, month: 4, day: 18, hour: 12))!
    }

    @Test
    func multiGroupDisciplineMergesIntoOneSummary() {
        let summaries = DisciplinesFixtures.payload().snapshot.disciplineSummaries(now: now, calendar: calendar)

        // Name-sorted: Algoritmos II before Física II, index == colorIndex.
        #expect(summaries.map(\.code) == ["EXA805", "EXA412"])
        #expect(summaries.map(\.colorIndex) == [0, 1])

        let fis = summaries[1]
        // Replicated grade rows collapse by platform id.
        #expect(fis.grades.map(\.label) == ["AV1", "AV2", "LAB"])
        // totalFaltas is replicated per group — never summed.
        #expect(fis.missedHours == 4)
        #expect(fis.hours == 75)
        #expect(fis.groupsLabel == "Te · Pr")
        // Theory group ("T01") wins the teacher slot over "T01P01".
        #expect(fis.teacherName == "João Nascimento")
        // Weighted mean: (6.8×2.5 + 9.0×5) / 7.5.
        let weighted = try! #require(fis.partialAverage)
        #expect(abs(weighted - 62.0 / 7.5) < 0.0001)
        #expect(fis.nextEvaluation == UpcomingEvaluation(label: "AV2", daysUntil: 31))

        let alg = summaries[0]
        #expect(alg.grades.count == 2)
        #expect(alg.missedHours == 2)
        #expect(alg.groupsLabel == nil)
        #expect(alg.nextEvaluation == UpcomingEvaluation(label: "AV2", daysUntil: 24))
    }

    @Test
    func statusTrustsUpstreamBeforeInferringFromGrades() {
        var summary = DisciplineSummary(id: "d", code: "X", name: "X", hours: 60, missedHours: 0)

        // A finals passer closes with a 5–7 mean — the flag must win.
        summary.partialAverage = 5.2
        summary.finalGrade = 5.8
        summary.approved = true
        #expect(summary.status == .approved)

        summary.approved = false
        #expect(summary.status == .failed)

        summary.approved = nil
        summary.finalGrade = nil
        summary.wentToFinals = true
        #expect(summary.status == .finals)

        summary.wentToFinals = false
        summary.finalGrade = 8.0
        #expect(summary.status == .approved)
        summary.finalGrade = 6.0
        #expect(summary.status == .finals)
        summary.finalGrade = 4.5
        #expect(summary.status == .failed)

        summary.finalGrade = nil
        summary.partialAverage = nil
        #expect(summary.status == .noGrades)
        summary.partialAverage = 5.0
        #expect(summary.status == .lowGrade)
        summary.partialAverage = 7.5
        #expect(summary.status == .ongoing)
    }

    @Test
    func allowedAbsencesFloorTheQuarterRule() {
        // 25% of 30h is 7.5h — missing 8 already drops attendance below 75%,
        // so the allowance floors, never rounds up.
        func summary(hours: Int) -> DisciplineSummary {
            DisciplineSummary(id: "d", code: "X", name: "X", hours: hours, missedHours: 0)
        }
        #expect(summary(hours: 30).allowedMissedHours == 7)
        #expect(summary(hours: 45).allowedMissedHours == 11)
        #expect(summary(hours: 60).allowedMissedHours == 15)
        #expect(summary(hours: 75).allowedMissedHours == 18)
    }

    @Test
    func attendanceDoesNotDoubleCountMultiGroupAbsences() {
        let overview = DisciplinesFixtures.payload().snapshot.homeOverview(now: now, calendar: calendar)

        // 135 enrolled hours, 6 missed (4 for Física across both groups + 2)
        // → 96%. Summing the replicated rows would report 93%.
        #expect(overview.attendance?.percent == 96)
    }

    @Test
    func mirrorSplitsCurrentPastAndPendingSemesters() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())

        #expect(try await store.cachedDisciplinesOverview(now: now) == nil)

        let current = DisciplinesFixtures.payload()
        let past = DisciplinesFixtures.payload(
            semesterId: "sem0",
            code: "20252",
            startDate: "2025-08-01",
            endDate: "2025-12-20",
            idPrefix: "past-"
        )
        let semesters = [
            current.snapshot.semester,
            past.snapshot.semester,
            SemesterRecord(
                id: "sem-old", code: "20251", description: "Semestre 2025.1",
                startDate: "2025-03-01", endDate: "2025-07-31", disciplineCount: 5
            ),
        ]
        try await store.apply(semesters: semesters, snapshot: current.snapshot, syncedAt: now)
        try await store.apply(semesters: [], snapshot: past.snapshot, syncedAt: now)

        let overview = try #require(try await store.cachedDisciplinesOverview(now: now))
        #expect(overview.current?.id == "sem1")
        #expect(overview.current?.disciplines.count == 2)
        #expect(overview.past.map(\.id) == ["sem0"])
        #expect(overview.pending == [PendingSemester(id: "sem-old", code: "20251", disciplineCount: 5)])
    }

    @Test
    func payloadApplyKeepsTheListedDisciplineCount() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        let payload = DisciplinesFixtures.payload()

        var listed = payload.snapshot.semester
        listed.disciplineCount = 2
        try await store.apply(semesters: [listed], snapshot: nil, syncedAt: now)
        // The payload's semester row carries no count — the listed one stays.
        try await store.apply(semesters: [], snapshot: payload.snapshot, syncedAt: now)

        let semester = try await store.writer.read { db in
            try SemesterRecord.fetchOne(db, key: "sem1")
        }
        #expect(semester?.disciplineCount == 2)
    }

    @Test
    func semesterLabelFormatsRawSagresCodes() {
        #expect(DisciplinesFormat.semesterLabel("20261") == "2026.1")
        #expect(DisciplinesFormat.semesterLabel("2010.2") == "2010.2")
        #expect(DisciplinesFormat.semesterLabel("2010") == "2010")
        #expect(DisciplinesFormat.semesterLabel("20262E") == "20262E")
    }
}

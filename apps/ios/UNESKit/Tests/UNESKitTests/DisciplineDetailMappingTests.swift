import Foundation
import GRDB
import Testing

@testable import UNESKit

/// Fixture tuned for the detail screen: a multi-group discipline with
/// replicated grades, a lecture timeline with placeholder rows, and lecture
/// materials. `distinctPracticeGrades` flips the practice group to its own
/// grade set (distinct platform ids) — the rarer upstream shape.
private func detailPayload(distinctPracticeGrades: Bool = false) -> SemesterPayloadDTO {
    SemesterPayloadDTO(
        semester: .init(
            id: "sem1", code: "20261", description: "Semestre 2026.1",
            startDate: "2026-03-01", endDate: "2026-07-31"
        ),
        disciplines: [
            .init(
                id: "d-fis", code: "EXA412", name: "Física II",
                // hours nil → the mapping must fall back to the offer, not
                // the 45 + 30 group sum.
                hours: nil,
                department: "Departamento de Ciências Exatas",
                program: "Termodinâmica, oscilações e ondas."
            ),
        ],
        disciplineOffers: [
            .init(id: "o-fis", disciplineId: "d-fis", hours: 75),
        ],
        classes: [
            .init(id: "c-t", offerId: "o-fis", hours: 45, groupName: "T01", type: "Teórica"),
            .init(id: "c-p", offerId: "o-fis", hours: 30, groupName: "T01P01", type: "Prática"),
        ],
        teachers: [
            .init(id: "t-t", name: "João Nascimento"),
            .init(id: "t-p", name: "Beatriz Sampaio"),
        ],
        classTeachers: [
            .init(classId: "c-t", teacherId: "t-t"),
            .init(classId: "c-p", teacherId: "t-p"),
        ],
        spaces: [],
        allocations: [],
        studentClasses: [
            .init(id: "sc-t", classId: "c-t", missedClasses: 4),
            .init(id: "sc-p", classId: "c-p", missedClasses: 4),
        ],
        studentGrades: [
            .init(
                id: "gt-1", studentClassId: "sc-t", name: "I Avaliação", nameShort: "AV1",
                ordinal: 1, value: "6.8", date: "2026-04-07", platformId: "pg-1", weight: "2.5"
            ),
            .init(
                id: "gt-2", studentClassId: "sc-t", name: "II Avaliação", nameShort: "AV2",
                ordinal: 2, value: nil, date: "2026-05-19", platformId: "pg-2", weight: "2.5"
            ),
            .init(
                id: "gp-1", studentClassId: "sc-p", name: "Relatórios", nameShort: "LAB",
                ordinal: distinctPracticeGrades ? 1 : 3, value: "9.0", date: nil,
                platformId: distinctPracticeGrades ? "pg-lab" : "pg-1", weight: "5"
            ),
        ],
        lectures: [
            .init(id: "l-2", classId: "c-t", ordinal: 2, date: "2026-04-14", subject: "Oscilações"),
            .init(id: "l-1", classId: "c-t", ordinal: 1, date: "2026-04-07", subject: "Termodinâmica"),
            // Placeholder row: no subject, no materials — must be dropped.
            .init(id: "l-3", classId: "c-t", ordinal: 3, date: "2026-04-21", subject: nil),
            .init(id: "l-4", classId: "c-t", ordinal: 4, date: "2026-04-28", subject: "Ondas mecânicas"),
            .init(id: "l-p", classId: "c-p", ordinal: 1, date: "2026-04-09", subject: "Lab 01"),
        ],
        lectureMaterials: [
            .init(id: "m-1", lectureId: "l-1", description: "Slides — termodinâmica", url: "https://x/termo.pptx", position: 1),
            .init(id: "m-2", lectureId: "l-1", description: nil, url: "https://x/lista-01.pdf", position: 2),
            .init(id: "m-p", lectureId: "l-p", description: "Roteiro lab 01", url: "https://x/roteiro.pdf", position: 1),
        ]
    )
}

struct DisciplineDetailMappingTests {
    let calendar = Calendar.current

    /// 2026-04-18 12:00 local — inside the fixture semester.
    var now: Date {
        calendar.date(from: DateComponents(year: 2026, month: 4, day: 18, hour: 12))!
    }

    @Test
    func replicatedGradesCollapseIntoOneMergedSection() {
        let detail = detailPayload().snapshot.disciplineDetail(disciplineId: "d-fis", now: now, calendar: calendar)!

        #expect(detail.code == "EXA412")
        #expect(detail.department == "Departamento de Ciências Exatas")
        #expect(detail.ementa == "Termodinâmica, oscilações e ondas.")
        #expect(detail.groups.map(\.code) == ["T01", "T01P01"])
        #expect(detail.groups.map(\.teacherName) == ["João Nascimento", "Beatriz Sampaio"])

        // The replicated set collapses to one section, unpinned from its
        // group so every filter still shows it.
        #expect(detail.sections.count == 1)
        #expect(detail.sections[0].groupCode == nil)
        #expect(detail.sections[0].grades.map(\.label) == ["AV1", "AV2"])
        #expect(detail.grades(forGroup: "T01P01").count == 2)

        // AV2 is 31 days out from the pinned "today".
        #expect(detail.sections[0].grades[1].daysUntil == 31)
        #expect(detail.sections[0].grades[0].daysUntil == nil)
    }

    @Test
    func distinctPracticeGradesKeepTheirOwnSection() {
        let detail = detailPayload(distinctPracticeGrades: true).snapshot
            .disciplineDetail(disciplineId: "d-fis", now: now, calendar: calendar)!

        #expect(detail.sections.map(\.groupCode) == ["T01", "T01P01"])
        #expect(detail.sections.map(\.name) == ["Teórica", "Prática"])
        #expect(detail.grades(forGroup: "T01P01").map(\.label) == ["LAB"])
        #expect(detail.grades(forGroup: nil).count == 3)
    }

    @Test
    func hoursFallBackToTheOfferNotTheGroupSum() {
        let snapshot = detailPayload().snapshot
        let detail = snapshot.disciplineDetail(disciplineId: "d-fis", now: now, calendar: calendar)!

        #expect(detail.hours == 75)
        #expect(detail.missedHours == 4)
        // The list mapping shares the fallback.
        #expect(snapshot.disciplineSummaries(now: now, calendar: calendar)[0].hours == 75)
    }

    @Test
    func lecturesDropPlaceholdersAndClassifyAgainstToday() {
        let detail = detailPayload().snapshot.disciplineDetail(disciplineId: "d-fis", now: now, calendar: calendar)!

        // Date-ascending across groups, no placeholder row.
        #expect(detail.lectures.map(\.id) == ["l-1", "l-p", "l-2", "l-4"])
        #expect(detail.lectures.map(\.isPast) == [true, true, true, false])
        #expect(detail.lectures[0].attachmentCount == 2)
        #expect(detail.lectures(forGroup: "T01").map(\.id) == ["l-1", "l-2", "l-4"])
    }

    @Test
    func attachmentsComeNewestFirstWithUrlNameFallback() {
        let detail = detailPayload().snapshot.disciplineDetail(disciplineId: "d-fis", now: now, calendar: calendar)!

        #expect(detail.attachments.map(\.id) == ["m-p", "m-1", "m-2"])
        // Nil caption falls back to the URL's last path component.
        #expect(detail.attachments.map(\.name) == ["Roteiro lab 01", "Slides — termodinâmica", "lista-01.pdf"])
        #expect(detail.attachments.map(\.groupCode) == ["T01P01", "T01", "T01"])
        #expect(detail.attachments(forGroup: "T01P01").map(\.id) == ["m-p"])
    }

    @Test
    func mirrorRoundTripCarriesTheDetailExtras() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        let payload = detailPayload()
        try await store.apply(semesters: [payload.snapshot.semester], snapshot: payload.snapshot, syncedAt: now)
        // Re-apply replaces the scope without duplicating materials.
        try await store.apply(semesters: [], snapshot: payload.snapshot, syncedAt: now)

        let detail = try #require(
            try await store.disciplineDetail(semesterId: "sem1", disciplineId: "d-fis", now: now)
        )
        #expect(detail.ementa == "Termodinâmica, oscilações e ondas.")
        #expect(detail.attachments.count == 3)
        #expect(detail.syncedAt != nil)

        let missing = try await store.disciplineDetail(semesterId: "sem1", disciplineId: "nope", now: now)
        #expect(missing == nil)
    }

    @Test
    func neededOnPendingIsWeightAware() {
        func grade(_ id: String, value: Double?, weight: Double?) -> DisciplineDetailGrade {
            DisciplineDetailGrade(id: id, label: id, title: id, value: value, weight: weight)
        }

        // Weighted: earned 6.8×2.5 + 9×5 = 62 of a 10-weight total →
        // (70 − 62) / 2.5 = 3.2 on the remaining evaluation.
        let weighted = DisciplineDetail.neededOnPending(of: [
            grade("AV1", value: 6.8, weight: 2.5),
            grade("AV2", value: nil, weight: 2.5),
            grade("LAB", value: 9.0, weight: 5),
        ])
        #expect(abs(weighted! - 3.2) < 0.0001)

        // Missing weights fall back to the plain mean: (21 − 8.3) / 2.
        let plain = DisciplineDetail.neededOnPending(of: [
            grade("AV1", value: 8.3, weight: nil),
            grade("AV2", value: nil, weight: nil),
            grade("AV3", value: nil, weight: nil),
        ])
        #expect(abs(plain! - 6.35) < 0.0001)

        // Already secured → clamped at zero, and nothing released → nil.
        #expect(
            DisciplineDetail.neededOnPending(of: [
                grade("AV1", value: 10, weight: 9),
                grade("AV2", value: nil, weight: 1),
            ]) == 0
        )
        #expect(DisciplineDetail.neededOnPending(of: [grade("AV1", value: nil, weight: 1)]) == nil)
    }

    @Test
    func detailFormattersSpeakPtBr() {
        #expect(DisciplinesFormat.longDate("2026-03-31") == "31/03/2026")
        #expect(DisciplinesFormat.shortDate("2026-03-31") == "31/03")
        // Relative-day copy follows the resource-bundle language, so assert
        // against the same symbols the formatter resolves rather than fixed pt-BR.
        #expect(DisciplinesFormat.inDaysLabel(0) == String.localized(.disciplinesToday))
        #expect(DisciplinesFormat.inDaysLabel(1) == String.localized(.disciplinesInDays(1)))
        #expect(DisciplinesFormat.inDaysLabel(12) == String.localized(.disciplinesInDays(12)))
        #expect(DisciplinesFormat.departmentLabel("Departamento de Ciências Exatas") == "Dep. de Ciências Exatas")
        #expect(DisciplinesFormat.departmentLabel("Ciências Exatas") == "Ciências Exatas")
        // Needed values round up — 6,9 would let the student fall short. The
        // decimal separator is locale-driven, so pin pt-BR for the assertion.
        let ptBR = Locale(identifier: "pt_BR")
        #expect(DisciplinesFormat.neededGrade(6.91, locale: ptBR) == "7,0")
        #expect(DisciplinesFormat.neededGrade(3.2, locale: ptBR) == "3,2")
    }
}

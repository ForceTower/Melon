import Foundation
import GRDB
import Testing

@testable import UNESKit

/// Fixture semester: April 16, 2026 is a Thursday (upstream day 4); ALGI (c1)
/// meets Thursday 08:00–10:00. `idPrefix` disambiguates the ids that are
/// globally unique upstream; disciplines/teachers/spaces reuse the same ids
/// across semesters on purpose (they are shared entities upstream).
enum MirrorFixtures {
    static func payload(
        semesterId: String = "sem1",
        code: String = "20261",
        idPrefix p: String = "",
        grades: [SemesterPayloadDTO.StudentGradeDTO]? = nil
    ) -> SemesterPayloadDTO {
        SemesterPayloadDTO(
            semester: .init(
                id: semesterId, code: code, description: "Semestre \(code)",
                startDate: "2026-01-01", endDate: "2026-12-31"
            ),
            disciplines: [.init(id: "d1", code: "ALGI", name: "Algoritmos I")],
            disciplineOffers: [.init(id: "\(p)o1", disciplineId: "d1")],
            classes: [.init(id: "\(p)c1", offerId: "\(p)o1", hours: 60)],
            teachers: [.init(id: "t1", name: "Adriana Matos")],
            classTeachers: [.init(classId: "\(p)c1", teacherId: "t1")],
            spaces: [.init(id: "s1", location: "LC-03")],
            allocations: [
                .init(id: "\(p)a1", classId: "\(p)c1", spaceId: "s1", day: 4, startTime: "08:00:00", endTime: "10:00:00"),
            ],
            studentClasses: [.init(id: "\(p)sc1", classId: "\(p)c1", missedClasses: 4)],
            studentGrades: grades ?? [
                .init(id: "\(p)g1", studentClassId: "\(p)sc1", name: "Prova 1", nameShort: "P1", ordinal: 1, value: "8.5", date: "2026-04-01"),
            ],
            lectures: [.init(id: "\(p)l1", classId: "\(p)c1", date: "2026-04-09", subject: "Introdução")]
        )
    }
}

struct MirrorStoreTests {
    let calendar = Calendar.current

    private func date(day: Int, hour: Int, minute: Int) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: 4, day: day, hour: hour, minute: minute))!
    }

    @Test
    func emptyMirrorHasNoCachedOverview() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        #expect(try await store.cachedOverview(now: .now) == nil)
    }

    @Test
    func roundTripMatchesTheDirectMapping() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        let snapshot = MirrorFixtures.payload().snapshot
        let syncedAt = Date(timeIntervalSince1970: 1_776_000_000)
        try await store.apply(semesters: [snapshot.semester], snapshot: snapshot, syncedAt: syncedAt)

        let now = date(day: 16, hour: 9, minute: 41)
        let cached = try await store.cachedOverview(now: now)

        #expect(cached?.overview == snapshot.homeOverview(now: now, calendar: calendar))
        #expect(cached?.overview.semesterCode == "20261")
        #expect(cached?.overview.today.count == 1)
        #expect(cached?.syncedAt == syncedAt)
    }

    @Test
    func reSyncDropsRowsDeletedUpstream() async throws {
        let database = try inMemoryDatabase()
        let store = MirrorStore(writer: database)
        let v1 = MirrorFixtures.payload(grades: [
            .init(id: "g1", studentClassId: "sc1", name: "Prova 1", nameShort: "P1", ordinal: 1, value: "7.0", date: "2026-03-01"),
            .init(id: "g2", studentClassId: "sc1", name: "Prova 2", nameShort: "P2", ordinal: 2, value: nil, date: "2026-05-01"),
        ]).snapshot
        try await store.apply(semesters: [v1.semester], snapshot: v1, syncedAt: .now)

        // Upstream deleted g2 and re-published g1 with a revised value.
        let v2 = MirrorFixtures.payload(grades: [
            .init(id: "g1", studentClassId: "sc1", name: "Prova 1", nameShort: "P1", ordinal: 1, value: "7.5", date: "2026-03-01"),
        ]).snapshot
        try await store.apply(semesters: [v2.semester], snapshot: v2, syncedAt: .now)

        let grades = try await database.read { try StudentGradeRecord.fetchAll($0) }
        #expect(grades == v2.studentGrades)
    }

    @Test
    func replaceOnlyTouchesItsOwnSemester() async throws {
        let database = try inMemoryDatabase()
        let store = MirrorStore(writer: database)
        let sem1 = MirrorFixtures.payload().snapshot
        let sem2 = MirrorFixtures.payload(semesterId: "sem2", code: "20252", idPrefix: "b-").snapshot
        try await store.apply(semesters: [sem1.semester, sem2.semester], snapshot: sem1, syncedAt: .now)
        try await store.apply(semesters: [], snapshot: sem2, syncedAt: .now)

        // Re-sync sem1 with everything gone upstream.
        let emptied = MirrorFixtures.payload(grades: []).snapshot
        try await store.apply(semesters: [], snapshot: emptied, syncedAt: .now)

        let grades = try await database.read { try StudentGradeRecord.fetchAll($0) }
        #expect(grades == sem2.studentGrades)

        // The same discipline id lives under both semesters independently.
        let disciplines = try await database.read {
            try DisciplineRecord.order(Column("semesterId")).fetchAll($0)
        }
        #expect(disciplines.map(\.semesterId) == ["sem1", "sem2"])
        #expect(disciplines.map(\.id) == ["d1", "d1"])
    }

    @Test
    func messageUpsertsAccumulateIntoTheSummary() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        try await store.upsertMessages(
            MessageMirrorPage(
                messages: [
                    MessageRecord(
                        id: "m1", subject: "Aviso", content: "Aula cancelada amanhã.",
                        senderName: "Adriana Matos", timestamp: "2026-04-15T18:22:11.123Z", read: false
                    ),
                    MessageRecord(
                        id: "m2", subject: "Material", content: "Slides na plataforma.",
                        senderName: "João Pereira", timestamp: "2026-04-14T09:10:00.000Z", read: true
                    ),
                ],
                scopes: [],
                attachments: []
            ),
            syncedAt: .now
        )

        let summary = try await store.messagesSummary()
        #expect(summary == MessagesSummary(
            unreadCount: 1,
            latestSenderName: "Adriana Matos",
            latestPreview: "Aula cancelada amanhã."
        ))

        // A later page marks m1 as read — upsert by id, not append.
        try await store.upsertMessages(
            MessageMirrorPage(
                messages: [
                    MessageRecord(
                        id: "m1", subject: "Aviso", content: "Aula cancelada amanhã.",
                        senderName: "Adriana Matos", timestamp: "2026-04-15T18:22:11.123Z", read: true
                    ),
                ],
                scopes: [],
                attachments: []
            ),
            syncedAt: .now
        )
        #expect(try await store.messagesSummary()?.unreadCount == 0)
    }
}

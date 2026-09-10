import Foundation
import Testing

@testable import UNESKit

/// The index kinds beyond disciplines, messages, and evaluations — the
/// discipline's typed fields, lectures, weekly sessions, and personal
/// events: their projections, diff kinds, identifiers, and the OS-major
/// ledger key that re-indexes after a system upgrade.
struct SpotlightIndexKindsTests {
    private var ptBR: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = Locale(identifier: "pt_BR")
        return calendar
    }

    // MARK: Projections

    @Test
    func disciplineCarriesTeacherRoomAndSchedule() {
        let snapshot = MirrorFixtures.payload().snapshot

        let discipline = snapshot.spotlightDisciplines(calendar: ptBR).first

        #expect(discipline?.teacher == "Adriana Matos")
        #expect(discipline?.room == "LC-03")
        #expect(discipline?.schedule == "qui · 08:00")
    }

    @Test
    func sessionsProjectEveryMergedWeeklyBlockWithRoomAndTeacher() {
        let snapshot = MirrorFixtures.payload().snapshot

        let sessions = snapshot.spotlightSessions()

        #expect(sessions.count == 1)
        let session = sessions[0]
        #expect(session.id == "session/sem1/d1/c1/4/480")
        #expect(session.title == "Algoritmos I")
        #expect(session.code == "ALGI")
        #expect(session.day == 4)
        #expect(session.startMinute == 480)
        #expect(session.room == "LC-03")
        #expect(session.teacher == "Adriana Matos")
        #expect(session.semesterStart == "2026-01-01")
        #expect(session.semesterEnd == "2026-12-31")
    }

    @Test
    func lecturesProjectSubjectsOfEnrolledClassesOnly() {
        var snapshot = MirrorFixtures.payload().snapshot
        snapshot.lectures.append(LectureRecord(id: "l2", semesterId: "sem1", classId: "c1", date: nil, subject: "  "))
        snapshot.lectures.append(LectureRecord(id: "l3", semesterId: "sem1", classId: "other", date: "2026-04-10", subject: "Alheia"))

        let lectures = snapshot.spotlightLectures(calendar: ptBR, locale: Locale(identifier: "pt_BR"))

        #expect(lectures.map(\.id) == ["lecture/sem1/d1/l1"])
        #expect(lectures.first?.title == "Introdução")
        #expect(lectures.first?.dateStamp == "2026-04-09")
        #expect(lectures.first?.subtitle.hasSuffix("Algoritmos I") == true)
        #expect(lectures.first?.keywords == ["Introdução", "ALGI", "Algoritmos I"])
    }

    @Test
    func personalEventsProjectTheirCalendarShape() {
        let event = PersonalEvent(
            id: "p1", title: "Entregar relatório", start: "2026-05-02", end: "2026-05-03",
            category: .task,
            discipline: PersonalEvent.DisciplineTag(id: "d1", code: "ALGI", name: "Algoritmos I"),
            reminder: .dayBefore, notes: "Anexar gráficos", createdAt: .now
        )

        let projected = MirrorStore.spotlightPersonalEvent(event)

        #expect(projected.id == "personalEvent/p1")
        #expect(projected.start == "2026-05-02")
        #expect(projected.end == "2026-05-03")
        #expect(projected.category == "task")
        #expect(projected.disciplineCode == "ALGI")
        #expect(projected.notes == "Anexar gráficos")
    }

    @Test
    func snapshotIncludesTheNewKinds() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        let payload = MirrorFixtures.payload()
        try await store.apply(semesters: [payload.snapshot.semester], snapshot: payload.snapshot, syncedAt: .now)
        try await store.savePersonalEvent(PersonalEvent(
            id: "p1", title: "Estudar", start: "2026-04-20", end: nil, category: .study,
            discipline: nil, reminder: .none, notes: "", createdAt: .now
        ))

        var iterator = store.spotlightUpdates(now: { Date(timeIntervalSince1970: 1_776_000_000) }).makeAsyncIterator()
        let snapshot = try await iterator.next() ?? nil

        #expect(snapshot?.lectures.map(\.lectureId) == ["l1"])
        #expect(snapshot?.sessions.map(\.classId) == ["c1"])
        #expect(snapshot?.personalEvents.map(\.eventId) == ["p1"])
    }

    // MARK: Identifiers

    @Test
    func newKindsRouteBackIntoTheApp() {
        #expect(SpotlightEntityID.parse("lecture/sem1/d1/l1") == .discipline(semesterId: "sem1", disciplineId: "d1"))
        #expect(SpotlightEntityID.parse("session/sem1/d1/c1/4/480") == .discipline(semesterId: "sem1", disciplineId: "d1"))
        #expect(SpotlightEntityID.parse("personalEvent/p1") == .calendar)
        // The type-name-prefixed form a tapped entity-created item carries.
        #expect(SpotlightEntityID.parse("LectureEntity/lecture/sem1/d1/l1") == .discipline(semesterId: "sem1", disciplineId: "d1"))
        #expect(SpotlightEntityID.parse("AcademicEventEntity/session/sem1/d1/c1/4/480") == .discipline(semesterId: "sem1", disciplineId: "d1"))
    }

    @Test
    func eventKindsAreReadOffTheIdentifier() {
        #expect(SpotlightEntityID.eventKind(of: "session/sem1/d1/c1/4/480") == .session)
        #expect(SpotlightEntityID.eventKind(of: "evaluation/sem1/d1/g1") == .evaluation)
        #expect(SpotlightEntityID.eventKind(of: "personalEvent/p1") == .personalEvent)
        #expect(SpotlightEntityID.eventKind(of: "lecture/sem1/d1/l1") == nil)
    }

    // MARK: Diff

    private func lecture(_ id: String, title: String = "Introdução") -> SpotlightLecture {
        SpotlightLecture(
            id: "lecture/sem1/d1/\(id)", semesterId: "sem1", disciplineId: "d1", lectureId: id,
            title: title, subtitle: "qui., 9 de abr. · Algoritmos I", dateStamp: "2026-04-09", keywords: []
        )
    }

    private func session(_ classId: String, room: String? = "LC-03") -> SpotlightSession {
        SpotlightSession(
            id: "session/sem1/d1/\(classId)/4/480", semesterId: "sem1", disciplineId: "d1", classId: classId,
            title: "Algoritmos I", code: "ALGI", day: 4, startMinute: 480, endMinute: 580,
            room: room, teacher: "Adriana Matos", semesterStart: "2026-01-01", semesterEnd: "2026-12-31"
        )
    }

    private func personalEvent(_ id: String, title: String = "Estudar") -> SpotlightPersonalEvent {
        SpotlightPersonalEvent(
            id: "personalEvent/\(id)", eventId: id, title: title, start: "2026-04-20", end: nil,
            notes: "", category: "study", disciplineName: nil, disciplineCode: nil
        )
    }

    @Test
    func diffTracksTheNewKindsIndependently() {
        let before = SpotlightSnapshot(
            disciplines: [], messages: [], evaluations: [],
            lectures: [lecture("l1")], sessions: [session("c1")], personalEvents: [personalEvent("p1")]
        )
        var ledger = SpotlightIndexLedger()
        let initial = SpotlightDiff.compute(ledger: ledger, snapshot: before)
        #expect(initial.lecturesToIndex == [lecture("l1")])
        #expect(initial.sessionsToIndex == [session("c1")])
        #expect(initial.personalEventsToIndex == [personalEvent("p1")])
        ledger.applyLectures(initial)
        ledger.applySessions(initial)
        ledger.applyPersonalEvents(initial)
        #expect(SpotlightDiff.compute(ledger: ledger, snapshot: before).isEmpty)

        let after = SpotlightSnapshot(
            disciplines: [], messages: [], evaluations: [],
            lectures: [lecture("l1", title: "Introdução revista")], sessions: [], personalEvents: [personalEvent("p1")]
        )
        let diff = SpotlightDiff.compute(ledger: ledger, snapshot: after)
        #expect(diff.lecturesToIndex == [lecture("l1", title: "Introdução revista")])
        #expect(diff.sessionIdsToDelete == ["session/sem1/d1/c1/4/480"])
        #expect(diff.personalEventsToIndex.isEmpty)
        #expect(diff.personalEventIdsToDelete.isEmpty)
    }

    // MARK: Ledger

    @Test
    func anotherOSMajorSignalsAWipe() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        var stale = SpotlightIndexLedger()
        stale.lectures["lecture/sem1/d1/l1"] = "digest"
        stale.platform = SpotlightIndexLedger.currentPlatform - 1

        try await store.saveSpotlightLedger(stale)

        #expect(try await store.spotlightLedger() == nil)
    }

    @Test
    func newKindsRoundTripThroughTheLedgerTable() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        var ledger = SpotlightIndexLedger()
        ledger.lectures["lecture/sem1/d1/l1"] = "digest-l"
        ledger.sessions["session/sem1/d1/c1/4/480"] = "digest-s"
        ledger.personalEvents["personalEvent/p1"] = "digest-p"

        try await store.saveSpotlightLedger(ledger)

        #expect(try await store.spotlightLedger() == ledger)
    }
}

import Foundation
import Testing

@testable import UNESKit

/// The mirror → Spotlight projection: what gets indexed, what never does
/// (grades, read/star state), and the identifier format that rides results
/// back into the app.
struct SpotlightProjectionTests {
    let calendar = Calendar.current

    private func date(day: Int, hour: Int, minute: Int) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: 4, day: day, hour: hour, minute: minute))!
    }

    private var now: Date { date(day: 16, hour: 9, minute: 41) }

    private func messagesPage(_ messages: [MessageRecord], scopes: [MessageScopeRecord] = []) -> MessageMirrorPage {
        MessageMirrorPage(messages: messages, scopes: scopes, attachments: [])
    }

    // MARK: Disciplines

    @Test
    func projectsOnlyTheActiveSemestersEnrolledDisciplines() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        let current = MirrorFixtures.payload().snapshot
        var past = MirrorFixtures.payload(semesterId: "sem0", code: "20252", idPrefix: "b-").snapshot
        past.semester.startDate = "2025-08-01"
        past.semester.endDate = "2025-12-20"
        try await store.apply(semesters: [current.semester, past.semester], snapshot: current, syncedAt: .now)
        try await store.apply(semesters: [], snapshot: past, syncedAt: .now)

        let disciplines = try await store.spotlightSuggestedDisciplines(now: now)

        #expect(disciplines.map(\.id) == ["discipline/sem1/d1"])
        #expect(disciplines.first?.title == "Algoritmos I")
        #expect(disciplines.first?.semesterId == "sem1")
        #expect(disciplines.first?.disciplineId == "d1")
    }

    @Test
    func subtitleCarriesCodeWeekdaysTimeAndRoom() {
        var snapshot = MirrorFixtures.payload().snapshot
        // A second, roomless meeting earlier in the week than the fixture's
        // Thursday one — the line orders days, keeps the earliest start, and
        // takes the first room it can resolve.
        snapshot.allocations.append(AllocationRecord(
            id: "a2", semesterId: "sem1", classId: "c1", spaceId: nil,
            day: 2, startTime: "10:50:00", endTime: "12:30:00"
        ))
        var ptBR = Calendar(identifier: .gregorian)
        ptBR.locale = Locale(identifier: "pt_BR")

        let disciplines = snapshot.spotlightDisciplines(calendar: ptBR)

        #expect(disciplines.map(\.subtitle) == ["ALGI · ter · qui · 08:00 · LC-03"])
        #expect(disciplines.map(\.keywords) == [
            ["ALGI", "Algoritmos I", "Adriana Matos", "20261", "Semestre 20261"],
        ])
    }

    @Test
    func stringMatchingIsDiacriticAndCaseInsensitive() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        var snapshot = MirrorFixtures.payload().snapshot
        snapshot.disciplines[0].name = "Cálculo Diferencial"
        try await store.apply(semesters: [snapshot.semester], snapshot: snapshot, syncedAt: .now)

        let byName = try await store.spotlightDisciplines(matching: "calculo", now: now)
        let byCode = try await store.spotlightDisciplines(matching: "algi", now: now)
        let miss = try await store.spotlightDisciplines(matching: "física", now: now)

        #expect(byName.map(\.disciplineId) == ["d1"])
        #expect(byCode.map(\.disciplineId) == ["d1"])
        #expect(miss.isEmpty)
    }

    @Test
    func noGradeDataLeaksIntoTheProjection() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        var snapshot = MirrorFixtures.payload(grades: [
            .init(
                id: "g1", studentClassId: "sc1", name: "Prova Distintiva",
                nameShort: "PD", ordinal: 1, value: "9.87", date: "2026-04-01"
            ),
        ]).snapshot
        snapshot.studentClasses[0].finalGrade = "6.9"
        snapshot.studentClasses[0].approved = false
        snapshot.studentClasses[0].wentToFinals = true
        try await store.apply(semesters: [snapshot.semester], snapshot: snapshot, syncedAt: .now)

        var updates = store.spotlightUpdates(now: { [now] in now }).makeAsyncIterator()
        let first = try await updates.next()
        let projected = try #require(first ?? nil)

        let encoded = String(decoding: try JSONEncoder().encode(projected), as: UTF8.self)
        #expect(encoded.contains("Algoritmos I"))
        #expect(!encoded.contains("9.87"))
        #expect(!encoded.contains("6.9"))
        #expect(!encoded.contains("Prova Distintiva"))
    }

    // MARK: Messages

    @Test
    func messageTitleFallsBackToSenderAndClassScopesBecomeKeywords() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        try await store.apply(semesters: [], snapshot: nil, syncedAt: .now)
        try await store.upsertMessages(messagesPage(
            [
                MessageRecord(
                    id: "m1", subject: "Prazo de matrícula", content: "O prazo termina sexta.",
                    senderName: "Colegiado de Computação", timestamp: "2026-04-10T12:00:00.000Z", read: false
                ),
                MessageRecord(
                    id: "m2", subject: "  ", content: "Aula cancelada hoje.",
                    senderName: "Adriana Matos", timestamp: "2026-04-11T12:00:00.000Z", read: false
                ),
            ],
            scopes: [MessageScopeRecord(
                id: "s1", messageId: "m2", scope: "class",
                classId: "c1", disciplineCode: "ALGI", disciplineName: "Algoritmos I"
            )]
        ), syncedAt: .now)

        let messages = try await store.spotlightRecentMessages(limit: 5)

        #expect(messages.map(\.messageId) == ["m2", "m1"])
        let scoped = messages[0]
        #expect(scoped.id == "message/m2")
        #expect(scoped.title == "Adriana Matos")
        // Sender is already the title — the subtitle is just the date.
        #expect(!scoped.subtitle.contains("Adriana"))
        #expect(!scoped.subtitle.isEmpty)
        #expect(scoped.body == "Aula cancelada hoje.")
        #expect(scoped.keywords == ["ALGI", "Algoritmos I"])
        let campus = messages[1]
        #expect(campus.title == "Prazo de matrícula")
        #expect(campus.subtitle.hasPrefix("Colegiado de Computação · "))
        #expect(campus.keywords.isEmpty)
    }

    @Test
    func readAndStarTogglesNeverReachTheProjectionStream() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        try await store.apply(semesters: [], snapshot: nil, syncedAt: .now)
        try await store.upsertMessages(messagesPage([
            MessageRecord(
                id: "m1", subject: "Aviso", content: "corpo",
                senderName: "UNES", timestamp: "2026-04-10T12:00:00.000Z", read: false
            ),
        ]), syncedAt: .now)

        var updates = store.spotlightUpdates(now: { [now] in now }).makeAsyncIterator()
        _ = try await updates.next()

        // The star toggle rewrites `messages.starred` (a tracked column) and
        // the read overlay writes an untracked table — neither may surface.
        try await store.setMessageStarred(id: "m1", starred: true)
        try await store.markMessageRead(id: "m1", now: now)
        try await store.upsertMessages(messagesPage([
            MessageRecord(
                id: "m2", subject: "Novo aviso", content: "outro corpo",
                senderName: "UNES", timestamp: "2026-04-11T12:00:00.000Z", read: false
            ),
        ]), syncedAt: .now)

        // The emission after the toggles is already the one carrying m2.
        let next = try await updates.next()
        let projected = try #require(next ?? nil)
        #expect(projected.messages.map(\.messageId) == ["m2", "m1"])
    }

    // MARK: Index gate

    @Test
    func projectionIsNilBeforeFirstSyncAndAfterAWipe() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        var updates = store.spotlightUpdates(now: { [now] in now }).makeAsyncIterator()

        let beforeSync = try await updates.next()
        #expect(beforeSync == .some(.none))

        let snapshot = MirrorFixtures.payload().snapshot
        try await store.apply(semesters: [snapshot.semester], snapshot: snapshot, syncedAt: .now)
        let synced = try await updates.next()
        #expect((synced ?? nil) != nil)

        try await store.wipe()
        let wiped = try await updates.next()
        #expect(wiped == .some(.none))
    }

    // MARK: Entity identifiers

    @Test
    func entityIdentifiersRoundTrip() {
        let tricky = ["plain", "sem/1", "d%2", "id with spaces", ""]
        for semesterId in tricky {
            for disciplineId in tricky {
                let id = SpotlightEntityID.discipline(semesterId: semesterId, disciplineId: disciplineId)
                #expect(SpotlightEntityID.parse(id)
                    == .discipline(semesterId: semesterId, disciplineId: disciplineId))
            }
        }
        for messageId in tricky {
            let id = SpotlightEntityID.message(id: messageId)
            #expect(SpotlightEntityID.parse(id) == .message(id: messageId))
        }
    }

    @Test
    func parsesTheTypeNamePrefixedSearchableItemIdentifier() {
        // Tapped results carry the CSSearchableItem identifier, which is the
        // entity id prefixed with the entity type name (seen on device).
        #expect(SpotlightEntityID.parse(
            "DisciplineEntity/discipline/20091441-d934-44cc-a0e4-2ab68a801625/cb3ad416-2eb0-4b87-a76c-562de99a91c7"
        ) == .discipline(
            semesterId: "20091441-d934-44cc-a0e4-2ab68a801625",
            disciplineId: "cb3ad416-2eb0-4b87-a76c-562de99a91c7"
        ))
        #expect(SpotlightEntityID.parse("MessageEntity/message/m1") == .message(id: "m1"))
    }

    @Test
    func garbageIdentifiersParseToNil() {
        #expect(SpotlightEntityID.parse("") == nil)
        #expect(SpotlightEntityID.parse("nonsense") == nil)
        #expect(SpotlightEntityID.parse("discipline/missing-part") == nil)
        #expect(SpotlightEntityID.parse("message/extra/part") == nil)
        #expect(SpotlightEntityID.parse("grade/x") == nil)
    }
}

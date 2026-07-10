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
            // Scheduled + pending: projected as an evaluation — its name is
            // indexed, but its weight (grade data) must never be.
            .init(
                id: "g2", studentClassId: "sc1", name: "Prova Pendente",
                nameShort: "PP", ordinal: 2, value: nil, date: "2026-06-10", weight: "77.7"
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
        #expect(encoded.contains("Prova Pendente"))
        #expect(!encoded.contains("9.87"))
        #expect(!encoded.contains("6.9"))
        #expect(!encoded.contains("77.7"))
        // Released rows never become evaluations, so their names stay out.
        #expect(!encoded.contains("Prova Distintiva"))
    }

    // MARK: Evaluations

    @Test
    func projectsOnlyScheduledPendingFutureEvaluationsSoonestFirst() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        let snapshot = MirrorFixtures.payload(grades: [
            // Value posted → out, even with a future date.
            .init(id: "g1", studentClassId: "sc1", name: "Prova 1", nameShort: "P1", ordinal: 1, value: "8.5", date: "2026-04-20"),
            // Date passed → out.
            .init(id: "g2", studentClassId: "sc1", name: "Prova 2", nameShort: "P2", ordinal: 2, value: nil, date: "2026-04-10"),
            // Unscheduled → out.
            .init(id: "g3", studentClassId: "sc1", name: "Prova 3", nameShort: "P3", ordinal: 3, value: nil, date: nil),
            .init(id: "g4", studentClassId: "sc1", name: "Prova 4", nameShort: "P4", ordinal: 4, value: nil, date: "2026-06-10"),
            .init(id: "g5", studentClassId: "sc1", name: "Prova 5", nameShort: "P5", ordinal: 5, value: nil, date: "2026-05-10"),
        ]).snapshot
        try await store.apply(semesters: [snapshot.semester], snapshot: snapshot, syncedAt: .now)

        let evaluations = try await store.spotlightSuggestedEvaluations(now: now)

        #expect(evaluations.map(\.gradeId) == ["g5", "g4"])
        #expect(evaluations.map(\.id) == ["evaluation/sem1/d1/g5", "evaluation/sem1/d1/g4"])
        #expect(evaluations.first?.title == "Prova 5 — Algoritmos I")
        #expect(evaluations.first?.semesterId == "sem1")
        #expect(evaluations.first?.disciplineId == "d1")
        #expect(evaluations.first?.keywords.contains("ALGI") == true)
    }

    @Test
    func evaluationsDedupAcrossGroupRowsByPlatformId() {
        var snapshot = MirrorFixtures.payload(grades: []).snapshot
        // A second enrolled group of the same class — the backend replicates
        // the discipline-level grade set onto every group row.
        snapshot.studentClasses.append(
            StudentClassRecord(id: "sc2", semesterId: "sem1", classId: "c1", missedClasses: nil)
        )
        snapshot.studentGrades = [
            StudentGradeRecord(
                id: "g1", semesterId: "sem1", studentClassId: "sc1", name: "Prova 1",
                nameShort: "P1", ordinal: 1, value: nil, date: "2026-06-10", platformId: "pl1"
            ),
            StudentGradeRecord(
                id: "g2", semesterId: "sem1", studentClassId: "sc2", name: "Prova 1",
                nameShort: "P1", ordinal: 1, value: nil, date: "2026-06-10", platformId: "pl1"
            ),
        ]

        let evaluations = snapshot.spotlightEvaluations(now: now)

        #expect(evaluations.map(\.gradeId) == ["pl1"])
        #expect(evaluations.map(\.id) == ["evaluation/sem1/d1/pl1"])
    }

    // MARK: Messages

    @Test
    func subjectlessTitlesFollowTheMessageOriginAndClassScopesBecomeKeywords() async throws {
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
        // Subject-less class notice: titled by the discipline, with the
        // sender demoted to the subtitle beside the date + body snippet.
        #expect(scoped.title == "Algoritmos I")
        #expect(scoped.subtitle.hasPrefix("Adriana Matos · "))
        #expect(scoped.subtitle.contains("Aula cancelada hoje."))
        #expect(scoped.body == "Aula cancelada hoje.")
        #expect(scoped.keywords == ["ALGI", "Algoritmos I"])
        let campus = messages[1]
        #expect(campus.title == "Prazo de matrícula")
        #expect(campus.subtitle.hasPrefix("Colegiado de Computação · "))
        #expect(campus.keywords.isEmpty)
    }

    @Test
    func subjectlessInstitutionalOriginsGetLabelTitlesAndPersonalKeepsTheSender() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        try await store.apply(semesters: [], snapshot: nil, syncedAt: .now)
        try await store.upsertMessages(messagesPage(
            [
                MessageRecord(
                    id: "m1", subject: nil, content: "Documentos disponíveis.",
                    senderName: "Maria da Silva", timestamp: "2026-04-10T12:00:00.000Z", read: false
                ),
                MessageRecord(
                    id: "m2", subject: nil, content: "Retorno das atividades.",
                    senderName: "Reitoria UEFS", timestamp: "2026-04-11T12:00:00.000Z", read: false
                ),
                MessageRecord(
                    id: "m3", subject: nil, content: "Me procure após a aula.",
                    senderName: "Beatriz Sampaio", timestamp: "2026-04-12T12:00:00.000Z", read: false
                ),
            ],
            scopes: [
                MessageScopeRecord(id: "s1", messageId: "m1", scope: "coordination"),
                MessageScopeRecord(id: "s2", messageId: "m2", scope: "university"),
                MessageScopeRecord(id: "s3", messageId: "m3", scope: "personal"),
            ]
        ), syncedAt: .now)

        let messages = try await store.spotlightRecentMessages(limit: 5)

        #expect(messages.map(\.messageId) == ["m3", "m2", "m1"])
        // Personal note: the sender IS the headline, and stays out of the
        // subtitle.
        #expect(messages[0].title == "Beatriz Sampaio")
        #expect(!messages[0].subtitle.contains("Beatriz"))
        // Institutional messages headline their origin, not the individual.
        #expect(messages[1].title == String.localized(.messagesFilterUniversity))
        #expect(messages[1].subtitle.hasPrefix("Reitoria UEFS · "))
        #expect(messages[2].title == String.localized(.messagesRoleSecretariat))
        #expect(messages[2].subtitle.hasPrefix("Maria da Silva · "))
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
    func evaluationIdentifiersResolveToTheDisciplineRoute() {
        // Evaluations live on the discipline detail screen — the grade id
        // only keeps the index identifier unique.
        let tricky = ["plain", "sem/1", "d%2", "id with spaces", ""]
        for semesterId in tricky {
            for disciplineId in tricky {
                let id = SpotlightEntityID.evaluation(
                    semesterId: semesterId, disciplineId: disciplineId, gradeId: "g/1"
                )
                #expect(SpotlightEntityID.parse(id)
                    == .discipline(semesterId: semesterId, disciplineId: disciplineId))
            }
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
        #expect(SpotlightEntityID.parse("EvaluationEntity/evaluation/sem1/d1/g1")
            == .discipline(semesterId: "sem1", disciplineId: "d1"))
    }

    @Test
    func garbageIdentifiersParseToNil() {
        #expect(SpotlightEntityID.parse("") == nil)
        #expect(SpotlightEntityID.parse("nonsense") == nil)
        #expect(SpotlightEntityID.parse("discipline/missing-part") == nil)
        #expect(SpotlightEntityID.parse("message/extra/part") == nil)
        #expect(SpotlightEntityID.parse("grade/x") == nil)
        #expect(SpotlightEntityID.parse("evaluation/sem1/d1") == nil)
        #expect(SpotlightEntityID.parse("evaluation/sem1/d1/g1/extra") == nil)
    }
}

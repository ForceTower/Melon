import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

/// The live repository against a stubbed wire and a real in-memory mirror.
struct HomeRepositoryTests {
    let calendar = Calendar.current

    private func date(day: Int, hour: Int, minute: Int) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: 4, day: day, hour: hour, minute: minute))!
    }

    @Test
    func refreshMirrorsThePayloadForTheLocalRead() async throws {
        let database = try inMemoryDatabase()
        let now = date(day: 16, hour: 9, minute: 41)
        let repository = HomeRepository.liveValue

        let cached = try await withDependencies {
            $0.database = database
            $0.apiClient.send = { request in
                switch request.path {
                case "api/sync/semesters": Wire.semesterList
                case "api/sync/semesters/sem1": Wire.semesterPayload
                case "api/sync/messages": Wire.messages
                default: throw APIError.invalidResponse
                }
            }
        } operation: {
            try await repository.refresh(now: now)
            return try await repository.cached(now: now)
        }

        let overview = try #require(cached?.overview)
        #expect(overview.semesterId == "sem1")
        #expect(overview.semesterCode == "20261")
        #expect(overview.coefficient?.value == 8.5)
        #expect(overview.today.count == 1)
        #expect(overview.messages == MessagesSummary(
            unreadCount: 1,
            latestSenderName: "Adriana Matos",
            latestPreview: "Aula cancelada amanhã."
        ))
        #expect(cached?.syncedAt == now)
    }

    @Test
    func observationEmitsAfterMirrorWrites() async throws {
        let database = try inMemoryDatabase()
        let now = date(day: 16, hour: 9, minute: 41)
        let mirror = MirrorStore(writer: database)
        let repository = HomeRepository.liveValue

        try await withDependencies {
            $0.database = database
            $0.date = .constant(now)
            $0.apiClient.send = { request in
                switch request.path {
                case "api/sync/semesters": Wire.semesterList
                case "api/sync/semesters/sem1": Wire.semesterPayload
                case "api/sync/messages": Wire.messages
                default: throw APIError.invalidResponse
                }
            }
        } operation: {
            try await repository.refresh(now: now)

            var updates = repository.observe().makeAsyncIterator()
            let initial = await updates.next()
            #expect(initial?.overview.messages?.unreadCount == 1)

            // A read marked from another tab lands in the next emission.
            try await mirror.markMessageRead(id: "m1", now: now)
            let afterRead = await updates.next()
            #expect(afterRead?.overview.messages?.unreadCount == 0)
        }
    }

    @Test
    func refreshRedownloadsMirroredSemestersWhoseDirtyAtMoved() async throws {
        let database = try inMemoryDatabase()
        let repository = HomeRepository.liveValue
        let requested = LockIsolated<[String]>([])
        let respondFromSecondEra: @Sendable (APIRequest) throws -> Data = { request in
            requested.withValue { $0.append(request.path) }
            switch request.path {
            case "api/sync/semesters": return Wire.twoSemesterList
            case "api/sync/semesters/sem1": return Wire.sem1PayloadGraded
            case "api/sync/semesters/sem2": return Wire.sem2Payload
            case "api/sync/messages": return Wire.messages
            default: throw APIError.invalidResponse
            }
        }

        // 2026.1 mirrored while it was the active semester, before its
        // grades landed upstream.
        let april = date(day: 16, hour: 9, minute: 41)
        try await withDependencies {
            $0.database = database
            $0.apiClient.send = { request in
                switch request.path {
                case "api/sync/semesters": Wire.oneSemesterList
                case "api/sync/semesters/sem1": Wire.sem1PayloadUngraded
                case "api/sync/messages": Wire.messages
                default: throw APIError.invalidResponse
                }
            }
        } operation: {
            try await repository.refresh(now: april)
        }

        // 2026.2 has started and the worker has since applied 2026.1's
        // grades (its dirtyAt moved): the refresh pulls the active semester
        // AND re-pulls the stale one, and the CR follows.
        let september = calendar.date(from: DateComponents(year: 2026, month: 9, day: 1, hour: 10))!
        try await withDependencies {
            $0.database = database
            $0.apiClient.send = respondFromSecondEra
        } operation: {
            try await repository.refresh(now: september)
            let paths = requested.value
            #expect(paths.contains("api/sync/semesters/sem2"))
            #expect(paths.contains("api/sync/semesters/sem1"))
            let cached = try await repository.cached(now: september)
            #expect(cached?.overview.coefficient?.value == 9.0)
        }

        // Nothing moved since — the past semester is not fetched again.
        requested.setValue([])
        try await withDependencies {
            $0.database = database
            $0.apiClient.send = respondFromSecondEra
        } operation: {
            try await repository.refresh(now: september)
        }
        let paths = requested.value
        #expect(paths.contains("api/sync/semesters/sem2"))
        #expect(!paths.contains("api/sync/semesters/sem1"))
    }

    @Test
    func mirrorKeepsServingWhenTheNetworkIsDown() async throws {
        let database = try inMemoryDatabase()
        let now = date(day: 16, hour: 9, minute: 41)
        let snapshot = MirrorFixtures.payload().snapshot
        try await MirrorStore(writer: database).apply(
            semesters: [snapshot.semester],
            snapshot: snapshot,
            syncedAt: now
        )
        let repository = HomeRepository.liveValue

        try await withDependencies {
            $0.database = database
            $0.apiClient.send = { _ in throw APIError.invalidResponse }
        } operation: {
            await #expect(throws: APIError.self) {
                try await repository.refresh(now: now)
            }
            let cached = try await repository.cached(now: now)
            #expect(cached?.overview == snapshot.homeOverview(now: now, calendar: calendar))
            #expect(cached?.syncedAt == now)
        }
    }
}

/// Envelope-wrapped wire fixtures carrying the fields the DTOs skip, to pin
/// the lenient subset decode.
private enum Wire {
    static let semesterList = Data("""
    {"ok":true,"data":{"semesters":[
      {"id":"sem1","platformId":1000,"code":"20261","description":"Semestre 2026.1",
       "startDate":"2026-01-01","endDate":"2026-12-31","track":null,"dirtyAt":null}
    ]}}
    """.utf8)

    static let semesterPayload = Data("""
    {"ok":true,"data":{
      "semester":{"id":"sem1","platformId":1000,"code":"20261","description":"Semestre 2026.1",
                  "startDate":"2026-01-01","endDate":"2026-12-31","track":null},
      "disciplines":[{"id":"d1","code":"ALGI","platformId":null,"name":"Algoritmos I","hours":60,"department":"DEXA","program":null}],
      "disciplineOffers":[{"id":"o1","disciplineId":"d1","semesterId":"sem1","platformId":null,"hours":60,"program":null}],
      "classes":[{"id":"c1","offerId":"o1","platformId":null,"groupName":"T01","type":"T","hours":60,"program":null}],
      "teachers":[{"id":"t1","platformId":null,"name":"Adriana Matos"}],
      "classTeachers":[{"classId":"c1","teacherId":"t1"}],
      "spaces":[{"id":"s1","platformId":null,"type":"Sala","campus":null,"location":"LC-03","modulo":null}],
      "allocations":[{"id":"a1","classId":"c1","spaceId":"s1","timePlatformId":null,"day":4,"startTime":"08:00:00","endTime":"10:00:00"}],
      "studentClasses":[{"id":"sc1","classId":"c1","finalGrade":null,"missedClasses":4,"resultDescription":null,
                         "approved":null,"underRevision":null,"wentToFinals":null,"resultSyncedAt":null}],
      "evaluations":[],
      "studentGrades":[{"id":"g1","studentClassId":"sc1","evaluationId":null,"platformId":null,"name":"Prova 1",
                        "nameShort":"P1","ordinal":1,"weight":null,"value":"8.5","date":"2026-04-01"}],
      "lectures":[{"id":"l1","classId":"c1","ordinal":1,"situation":null,"date":"2026-04-09","subject":"Introdução"}],
      "lectureMaterials":[]
    }}
    """.utf8)

    // Staleness fixtures: 2026.1 alone and active, then 2026.2 active with
    // 2026.1's dirtyAt moved past the first list's value.
    static let oneSemesterList = Data("""
    {"ok":true,"data":{"semesters":[
      {"id":"sem1","platformId":1000,"code":"20261","description":"Semestre 2026.1",
       "startDate":"2026-01-01","endDate":"2026-06-30","track":null,"dirtyAt":"2026-04-10T00:00:00.000Z"}
    ]}}
    """.utf8)

    static let twoSemesterList = Data("""
    {"ok":true,"data":{"semesters":[
      {"id":"sem2","platformId":1001,"code":"20262","description":"Semestre 2026.2",
       "startDate":"2026-08-01","endDate":"2026-12-31","track":null,"dirtyAt":"2026-08-30T00:00:00.000Z"},
      {"id":"sem1","platformId":1000,"code":"20261","description":"Semestre 2026.1",
       "startDate":"2026-01-01","endDate":"2026-06-30","track":null,"dirtyAt":"2026-07-22T21:14:00.000Z"}
    ]}}
    """.utf8)

    static let sem1PayloadUngraded = Data("""
    {"ok":true,"data":{
      "semester":{"id":"sem1","platformId":1000,"code":"20261","description":"Semestre 2026.1",
                  "startDate":"2026-01-01","endDate":"2026-06-30","track":null},
      "disciplines":[{"id":"d1","code":"ALGI","platformId":null,"name":"Algoritmos I","hours":60,"department":null,"program":null}],
      "disciplineOffers":[{"id":"o1","disciplineId":"d1","semesterId":"sem1","platformId":null,"hours":60,"program":null}],
      "classes":[{"id":"c1","offerId":"o1","platformId":null,"groupName":"T01","type":"T","hours":60,"program":null}],
      "teachers":[],"classTeachers":[],"spaces":[],"allocations":[],
      "studentClasses":[{"id":"sc1","classId":"c1","finalGrade":null,"missedClasses":0,"resultDescription":null,
                         "approved":null,"underRevision":null,"wentToFinals":null,"resultSyncedAt":null}],
      "evaluations":[],"studentGrades":[],"lectures":[],"lectureMaterials":[]
    }}
    """.utf8)

    static let sem1PayloadGraded = Data("""
    {"ok":true,"data":{
      "semester":{"id":"sem1","platformId":1000,"code":"20261","description":"Semestre 2026.1",
                  "startDate":"2026-01-01","endDate":"2026-06-30","track":null},
      "disciplines":[{"id":"d1","code":"ALGI","platformId":null,"name":"Algoritmos I","hours":60,"department":null,"program":null}],
      "disciplineOffers":[{"id":"o1","disciplineId":"d1","semesterId":"sem1","platformId":null,"hours":60,"program":null}],
      "classes":[{"id":"c1","offerId":"o1","platformId":null,"groupName":"T01","type":"T","hours":60,"program":null}],
      "teachers":[],"classTeachers":[],"spaces":[],"allocations":[],
      "studentClasses":[{"id":"sc1","classId":"c1","finalGrade":"9.0","missedClasses":0,"resultDescription":null,
                         "approved":true,"underRevision":null,"wentToFinals":null,"resultSyncedAt":null}],
      "evaluations":[],"studentGrades":[],"lectures":[],"lectureMaterials":[]
    }}
    """.utf8)

    static let sem2Payload = Data("""
    {"ok":true,"data":{
      "semester":{"id":"sem2","platformId":1001,"code":"20262","description":"Semestre 2026.2",
                  "startDate":"2026-08-01","endDate":"2026-12-31","track":null},
      "disciplines":[{"id":"d2","code":"CALC","platformId":null,"name":"Cálculo II","hours":60,"department":null,"program":null}],
      "disciplineOffers":[{"id":"o2","disciplineId":"d2","semesterId":"sem2","platformId":null,"hours":60,"program":null}],
      "classes":[{"id":"c2","offerId":"o2","platformId":null,"groupName":"T01","type":"T","hours":60,"program":null}],
      "teachers":[],"classTeachers":[],"spaces":[],"allocations":[],
      "studentClasses":[{"id":"sc2","classId":"c2","finalGrade":null,"missedClasses":0,"resultDescription":null,
                         "approved":null,"underRevision":null,"wentToFinals":null,"resultSyncedAt":null}],
      "evaluations":[],"studentGrades":[],"lectures":[],"lectureMaterials":[]
    }}
    """.utf8)

    static let messages = Data("""
    {"ok":true,"data":{"messages":[
      {"id":"m1","source":"sagres","platformId":null,"subject":"Aviso","content":"Aula cancelada amanhã.",
       "senderName":"Adriana Matos","senderType":null,"timestamp":"2026-04-15T18:22:11.123Z",
       "createdAt":"2026-04-15T18:22:12.000Z","read":false,"readAt":null,"starred":false,"scopes":[],"attachments":[]},
      {"id":"m2","source":"sagres","platformId":null,"subject":"Material","content":"Slides na plataforma.",
       "senderName":"João Pereira","senderType":null,"timestamp":"2026-04-14T09:10:00.000Z",
       "createdAt":"2026-04-14T09:10:01.000Z","read":true,"readAt":null,"starred":false,"scopes":[],"attachments":[]}
    ],"nextCursor":null}}
    """.utf8)
}

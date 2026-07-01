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

        let (fresh, cached) = try await withDependencies {
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
            let fresh = try await repository.refresh(now: now)
            let cached = try await repository.cached(now: now)
            return (fresh, cached)
        }

        #expect(fresh.semesterCode == "20261")
        #expect(fresh.coefficient?.value == 8.5)
        #expect(fresh.today.count == 1)
        #expect(fresh.messages == MessagesSummary(
            unreadCount: 1,
            latestSenderName: "Adriana Matos",
            latestPreview: "Aula cancelada amanhã."
        ))
        #expect(cached?.overview == fresh)
        #expect(cached?.syncedAt == now)
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
                _ = try await repository.refresh(now: now)
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

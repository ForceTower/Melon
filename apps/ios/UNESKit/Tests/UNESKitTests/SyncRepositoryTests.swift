import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

/// The live backfill against a stubbed wire and a real in-memory mirror.
struct SyncRepositoryTests {
    private let now = Date(timeIntervalSince1970: 1_790_946_000)

    @Test(arguments: ["running", "queued"])
    func backfillThatNeverSettlesIsRetriedOnTheNextLaunch(messagesStatus: String) async throws {
        let database = try inMemoryDatabase()
        let polls = LockIsolated(0)

        await withDependencies {
            $0.database = database
            $0.continuousClock = InstantClock()
            $0.date = .constant(now)
            $0.apiClient.send = { request in
                switch request.path {
                case "api/sync/onboarding-status":
                    polls.withValue { $0 += 1 }
                    return Wire.onboarding(messages: messagesStatus)
                case "api/sync/semesters": return Wire.semesterList
                case "api/sync/semesters/sem1": return Wire.semesterPayload
                case "api/sync/messages": return Wire.messages
                default: throw APIError.invalidResponse
                }
            }
        } operation: {
            await #expect(throws: (any Error).self) {
                try await SyncRepository.liveValue.backfillMirror()
            }
        }

        #expect(polls.value == 60)
        let mirror = MirrorStore(writer: database)
        #expect(try await mirror.isBackfillMirrorComplete() == false)

        try await withDependencies {
            $0.database = database
            $0.continuousClock = InstantClock()
            $0.date = .constant(now)
            $0.apiClient.send = { request in
                switch request.path {
                case "api/sync/onboarding-status": return Wire.onboarding(messages: "done")
                case "api/sync/semesters": return Wire.semesterList
                case "api/sync/semesters/sem1": return Wire.semesterPayload
                case "api/sync/messages": return Wire.messages
                default: throw APIError.invalidResponse
                }
            }
        } operation: {
            try await SyncRepository.liveValue.backfillMirror()
        }

        #expect(try await mirror.isBackfillMirrorComplete())
        #expect(try await mirror.messagesOverview(now: now).messages.map(\.id) == ["m1"])
    }
}

/// Sleeps return at once. `ImmediateClock` yields through background-priority
/// tasks on every sleep, which crawls across 60 polls on a loaded machine.
private struct InstantClock: Clock {
    let now = ContinuousClock.now
    let minimumResolution = Duration.zero

    func sleep(until deadline: ContinuousClock.Instant, tolerance: Duration?) async throws {
        try Task.checkCancellation()
    }
}

private enum Wire {
    static func onboarding(messages: String) -> Data {
        Data("""
        {"ok":true,"data":{
          "courseLinked":true,
          "initial":{"status":"done","appliedSemesters":1},
          "semesters":{"status":"done","total":1,"done":1,"failed":0},
          "messages":{"status":"\(messages)"},
          "activeSemesterReady":true
        }}
        """.utf8)
    }

    static let semesterList = Data("""
    {"ok":true,"data":{"semesters":[
      {"id":"sem1","code":"20261","description":"Semestre 2026.1",
       "startDate":"2026-01-01","endDate":"2026-12-31"}
    ]}}
    """.utf8)

    static let semesterPayload = Data("""
    {"ok":true,"data":{
      "semester":{"id":"sem1","code":"20261","description":"Semestre 2026.1",
                  "startDate":"2026-01-01","endDate":"2026-12-31"},
      "disciplines":[],"disciplineOffers":[],"classes":[],"teachers":[],"classTeachers":[],
      "spaces":[],"allocations":[],"studentClasses":[],"studentGrades":[],"lectures":[]
    }}
    """.utf8)

    static let messages = Data("""
    {"ok":true,"data":{"messages":[
      {"id":"m1","subject":"Aviso","content":"Aula cancelada amanhã.","senderName":"Adriana Matos",
       "timestamp":"2026-04-15T18:22:11.123Z","read":false}
    ],"nextCursor":null}}
    """.utf8)
}

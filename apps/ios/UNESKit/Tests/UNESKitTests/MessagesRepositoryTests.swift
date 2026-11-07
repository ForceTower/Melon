import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

/// The live inbox repository over a real in-memory mirror, against a fake
/// server that applies the read and star acks it accepts.
struct MessagesRepositoryTests {
    private let now = Date(timeIntervalSince1970: 1_790_946_000)

    @Test
    func unackedUnstarWinsOverTheServerUntilARefreshAcksIt() async throws {
        let server = LockIsolated(FakeInbox(starred: true))
        let repository = MessagesRepository.liveValue

        try await withDependencies {
            $0.database = try inMemoryDatabase()
            $0.apiClient.send = { request in try server.withValue { try $0.respond(to: request) } }
        } operation: {
            try await repository.refresh(now: now)

            server.withValue { $0.acksFail = true }
            try await repository.setStarred(id: "m1", starred: false)
            try await repository.refresh(now: now)
            #expect(try await repository.cached(now: now)?.messages.first?.starred == false)

            server.withValue { $0.acksFail = false }
            try await repository.refresh(now: now)
            #expect(server.value.starred == false)
            #expect(try await repository.cached(now: now)?.messages.first?.starred == false)

            let attempts = server.value.starAttempts
            try await repository.refresh(now: now)
            #expect(server.value.starAttempts == attempts)
        }
    }

    @Test(arguments: [false, true])
    func failedReadAckIsResentOnTheNextRefresh(markingAll: Bool) async throws {
        let server = LockIsolated(FakeInbox(starred: false))
        let repository = MessagesRepository.liveValue

        try await withDependencies {
            $0.database = try inMemoryDatabase()
            $0.apiClient.send = { request in try server.withValue { try $0.respond(to: request) } }
        } operation: {
            try await repository.refresh(now: now)

            server.withValue { $0.acksFail = true }
            if markingAll {
                try await repository.markAllRead(now: now)
            } else {
                try await repository.markRead(id: "m1", now: now)
            }
            try await repository.refresh(now: now)
            #expect(try await repository.cached(now: now)?.unreadCount == 0)
            #expect(server.value.read == false)

            server.withValue { $0.acksFail = false }
            try await repository.refresh(now: now)
            #expect(server.value.read)

            let attempts = server.value.readAttempts
            try await repository.refresh(now: now)
            #expect(server.value.readAttempts == attempts)
        }
    }
}

/// One message, `m1`, whose read and star flags follow the acks the server
/// accepted.
private struct FakeInbox {
    var read = false
    var starred: Bool
    var acksFail = false
    var readAttempts = 0
    var starAttempts = 0

    private struct ReadBody: Decodable { let ids: [String] }
    private struct StarBody: Decodable { let id: String; let starred: Bool }

    private static let ok = Data(#"{"ok":true,"data":null}"#.utf8)

    mutating func respond(to request: APIRequest) throws -> Data {
        switch (request.method, request.path) {
        case ("GET", "api/sync/messages"):
            return Data("""
            {"ok":true,"data":{"messages":[
              {"id":"m1","source":"app","subject":"Aviso","content":"Oi","senderName":"UNES",
               "timestamp":"2026-10-02T12:30:00.000Z","read":\(read),"starred":\(starred)}
            ],"nextCursor":null}}
            """.utf8)
        case ("POST", "api/sync/messages/read"):
            readAttempts += 1
            guard !acksFail else { throw URLError(.notConnectedToInternet) }
            let body = try JSONDecoder().decode(ReadBody.self, from: request.body ?? Data())
            if body.ids.contains("m1") { read = true }
            return Self.ok
        case ("POST", "api/sync/messages/read-all"):
            readAttempts += 1
            guard !acksFail else { throw URLError(.notConnectedToInternet) }
            read = true
            return Self.ok
        case ("POST", "api/sync/messages/star"):
            starAttempts += 1
            guard !acksFail else { throw URLError(.notConnectedToInternet) }
            let body = try JSONDecoder().decode(StarBody.self, from: request.body ?? Data())
            if body.id == "m1" { starred = body.starred }
            return Self.ok
        default:
            throw APIError.invalidResponse
        }
    }
}

import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

private struct PilotContract: Sendable {
    let data: Data
    let now = Date(timeIntervalSince1970: 1_790_946_000)

    init() throws {
        var root = URL(fileURLWithPath: #filePath)
        for _ in 0..<6 { root.deleteLastPathComponent() }
        data = try Data(contentsOf: root.appending(path: "contracts/v1/pilot.json"))
    }

    func payload(_ key: String) throws -> Data {
        let object = try #require(JSONSerialization.jsonObject(with: data) as? [String: Any])
        return try JSONSerialization.data(withJSONObject: #require(object[key]))
    }

    func decode<T: Decodable>(_ type: T.Type, _ key: String) throws -> T {
        try JSONDecoder().decode(type, from: payload(key))
    }

    func envelope(_ key: String) throws -> Data {
        let payload = try JSONSerialization.jsonObject(with: payload(key))
        return try JSONSerialization.data(withJSONObject: ["ok": true, "data": payload])
    }

    func respond(_ request: APIRequest) throws -> Data {
        switch request.path {
        case "api/auth/login": return try envelope("login")
        case "api/sync/profile": return try envelope("profile")
        case "api/sync/messages": return try envelope("messages")
        case "api/sync/semesters/00000000-0000-4000-8000-000000000004":
            return try envelope("semester")
        case "api/sync/semesters":
            let semester = try #require(JSONSerialization.jsonObject(with: payload("semester")) as? [String: Any])
            return try JSONSerialization.data(withJSONObject: [
                "ok": true, "data": ["semesters": [try #require(semester["semester"])]]
            ])
        default:
            Issue.record("Unexpected pilot request: \(request.method) \(request.path)")
            throw APIError.invalidResponse
        }
    }
}

@MainActor
struct PilotScenarioTests {
    @Test("Shared v1 contract decodes the native client DTOs")
    func sharedWireContract() throws {
        let fixture = try PilotContract()
        struct Metadata: Decodable {
            let version: Int
            let clock: String
            let timezone: String
        }
        let metadata = try JSONDecoder().decode(Metadata.self, from: fixture.data)
        #expect(metadata.version == 1)
        #expect(metadata.timezone == "America/Bahia")
        #expect(ISO8601DateFormatter().date(from: metadata.clock) == fixture.now)
        #if DEBUG
        #expect(ISO8601DateFormatter().date(from: metadata.clock) == DevelopmentScenario.now)
        #endif
        let login = try fixture.decode(LoginResponseDTO.self, "login")
        let profile = try fixture.decode(ProfileDTO.self, "profile")
        let status = try fixture.decode(OnboardingStatusDTO.self, "onboarding")
        let semester = try fixture.decode(SemesterPayloadDTO.self, "semester")
        let messages = try fixture.decode(MessageListDTO.self, "messages")

        #expect(login.domain.user.id == profile.domain.id)
        #expect(login.domain.user.name == "Estudante Exemplo")
        #expect(status.domain.initial.state == .done)
        #expect(status.domain.initial.appliedSemesters == 1)
        #expect(semester.semester.code == "2026.2")
        #expect(semester.snapshot.readyOverview(now: fixture.now).classCount == 1)
        #expect(messages.page.messages.isEmpty)
    }

    @Test("auth.login uses the shared wire response through the live repository")
    func loginScenario() async throws {
        let fixture = try PilotContract()
        let session = try fixture.decode(LoginResponseDTO.self, "login").domain
        let sessions = SessionStore.inMemory()
        let store = TestStore(initialState: LoginFeature.State(username: "scenario", password: "synthetic-only")) {
            LoginFeature()
        } withDependencies: {
            $0.authRepository = .liveValue
            $0.sessionStore = sessions
            $0.sessionInvalidation.clear = {}
            $0.apiClient.send = { request in
                #expect(request.method == "POST")
                #expect(request.authorization == .unauthenticated)
                return try fixture.respond(request)
            }
        }

        await store.send(.submitTapped) { $0.isLoading = true }
        await store.receive(.loginResponse(.success(session))) { $0.isLoading = false }
        await store.receive(.delegate(.loggedIn(username: "scenario", session: session)))
        #expect(sessions.current() == session)
    }

    @Test("home.offline-with-cache preserves the last successful synthetic semester")
    func offlineCacheScenario() async throws {
        let fixture = try PilotContract()
        let database = try inMemoryDatabase()
        let repository = HomeRepository.liveValue
        let online = LockIsolated(true)

        try await withDependencies {
            $0.database = database
            $0.date = .constant(fixture.now)
            $0.apiClient.send = { request in
                guard online.value else { throw URLError(.notConnectedToInternet) }
                return try fixture.respond(request)
            }
        } operation: {
            try await repository.refresh(now: fixture.now)
            let cached = try #require(try await repository.cached(now: fixture.now))
            #expect(cached.overview.disciplines.map(\.name) == ["Algoritmos de Exemplo"])
            #expect(cached.overview.attendance == nil)
            #expect(cached.syncedAt == fixture.now)
            online.setValue(false)

            do {
                try await repository.refresh(now: fixture.now.addingTimeInterval(60))
                Issue.record("Offline refresh unexpectedly succeeded")
            } catch let error as URLError {
                #expect(error.code == .notConnectedToInternet)
            }
            let afterFailure = try await repository.cached(now: fixture.now)
            #expect(afterFailure == cached)
        }
    }
}

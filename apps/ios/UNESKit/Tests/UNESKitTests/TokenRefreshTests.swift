import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

/// Covers the 401 → `api/auth/token/refresh` → retry path in `APIClient.live`.
/// Serialized because the stub transport hangs its handler off shared state.
@Suite(.serialized)
struct TokenRefreshTests {
    private static let baseURL = URL(string: "https://melon.test")!
    private static let expired = Session(
        accessToken: "expired",
        refreshToken: "rotatable",
        user: SessionUser(id: "u1", name: "Mariana", imageUrl: nil)
    )

    // MARK: Helpers

    private static func client() -> (APIClient, StubURLProtocol.Log) {
        let log = StubURLProtocol.Log()
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubURLProtocol.self]
        return (
            APIClient.live(baseURL: baseURL, session: URLSession(configuration: configuration)),
            log
        )
    }

    /// 401s anything bearing `expired`, 200s anything bearing `rotated`, and
    /// answers the refresh with whatever `refresh` returns.
    private static func route(
        log: StubURLProtocol.Log,
        refresh: @escaping @Sendable () -> (Int, Data)
    ) -> @Sendable (URLRequest) -> (Int, Data) {
        { request in
            let path = request.url?.path() ?? ""
            log.record(path)
            if path.hasSuffix("api/auth/token/refresh") {
                return refresh()
            }
            let token = request.value(forHTTPHeaderField: "Authorization")
            return token == "Bearer rotated"
                ? (200, Data(#"{"ok":true,"message":"","data":{}}"#.utf8))
                : (401, Data(#"{"ok":false,"message":"Invalid or expired access token"}"#.utf8))
        }
    }

    private static let rotatedPair = Data(
        #"{"ok":true,"message":"Token refreshed","data":{"accessToken":"rotated","refreshToken":"next"}}"#.utf8
    )

    // MARK: Tests

    @Test
    func rotatesThePairAndRetriesTheRequest() async throws {
        let (apiClient, log) = Self.client()
        let store = SessionStore.inMemory(initial: Self.expired)
        StubURLProtocol.handler.setValue(Self.route(log: log) { (200, Self.rotatedPair) })

        try await withDependencies {
            $0.sessionStore = store
            $0.sessionInvalidation = SessionInvalidation(markInvalid: {}, clear: {})
        } operation: {
            _ = try await apiClient.send(APIRequest(path: "api/me/ping"))
        }

        #expect(log.refreshCount == 1)
        // Both halves rotate, and the user carries over from the old session.
        #expect(store.current()?.accessToken == "rotated")
        #expect(store.current()?.refreshToken == "next")
        #expect(store.current()?.user.id == "u1")
    }

    @Test
    func concurrentUnauthorizedRequestsRefreshOnlyOnce() async throws {
        let (apiClient, log) = Self.client()
        let store = SessionStore.inMemory(initial: Self.expired)
        StubURLProtocol.handler.setValue(Self.route(log: log) { (200, Self.rotatedPair) })

        try await withDependencies {
            $0.sessionStore = store
            $0.sessionInvalidation = SessionInvalidation(markInvalid: {}, clear: {})
        } operation: {
            try await withThrowingTaskGroup(of: Void.self) { group in
                for index in 0..<8 {
                    group.addTask { _ = try await apiClient.send(APIRequest(path: "api/me/ping\(index)")) }
                }
                try await group.waitForAll()
            }
        }

        // The refresh token is burned on first use, so a second rotation would
        // strand the session.
        #expect(log.refreshCount == 1)
        #expect(store.current()?.accessToken == "rotated")
    }

    @Test
    func aRejectedRefreshFlagsTheSessionAndIsNotRetried() async throws {
        let (apiClient, log) = Self.client()
        let store = SessionStore.inMemory(initial: Self.expired)
        let invalidated = LockIsolated(0)
        StubURLProtocol.handler.setValue(Self.route(log: log) {
            (400, Data(#"{"ok":false,"message":"Invalid refresh token"}"#.utf8))
        })

        await withDependencies {
            $0.sessionStore = store
            $0.sessionInvalidation = SessionInvalidation(
                markInvalid: { invalidated.withValue { $0 += 1 } },
                clear: {}
            )
        } operation: {
            await #expect(throws: APIError.server(status: 401, message: "Invalid or expired access token")) {
                _ = try await apiClient.send(APIRequest(path: "api/me/ping"))
            }
            // The latch keeps a structurally dead session from re-attempting
            // the refresh on every subsequent request.
            await #expect(throws: APIError.self) {
                _ = try await apiClient.send(APIRequest(path: "api/me/ping"))
            }
        }

        #expect(log.refreshCount == 1)
        #expect(invalidated.value == 1)
        // The session is kept so the re-auth sheet can swap tokens in place.
        #expect(store.current()?.accessToken == "expired")
    }

    @Test
    func aServerErrorOnRefreshStaysRetryable() async throws {
        let (apiClient, log) = Self.client()
        let store = SessionStore.inMemory(initial: Self.expired)
        let invalidated = LockIsolated(0)
        StubURLProtocol.handler.setValue(Self.route(log: log) {
            (503, Data(#"{"ok":false,"message":"Service Unavailable"}"#.utf8))
        })

        await withDependencies {
            $0.sessionStore = store
            $0.sessionInvalidation = SessionInvalidation(
                markInvalid: { invalidated.withValue { $0 += 1 } },
                clear: {}
            )
        } operation: {
            await #expect(throws: APIError.self) {
                _ = try await apiClient.send(APIRequest(path: "api/me/ping"))
            }
            await #expect(throws: APIError.self) {
                _ = try await apiClient.send(APIRequest(path: "api/me/ping"))
            }
        }

        // A 5xx is the server's problem — the pair is untouched, so the next
        // 401 tries again and nothing is flagged.
        #expect(log.refreshCount == 2)
        #expect(invalidated.value == 0)
    }

    @Test
    func anEmptyRefreshTokenIsTerminalWithoutACall() async throws {
        let (apiClient, log) = Self.client()
        // Sessions adopted from the legacy app can carry an empty refresh token.
        let store = SessionStore.inMemory(initial: Session(
            accessToken: "expired",
            refreshToken: "",
            user: SessionUser(id: "u1", name: "Mariana", imageUrl: nil)
        ))
        let invalidated = LockIsolated(0)
        StubURLProtocol.handler.setValue(Self.route(log: log) { (200, Self.rotatedPair) })

        await withDependencies {
            $0.sessionStore = store
            $0.sessionInvalidation = SessionInvalidation(
                markInvalid: { invalidated.withValue { $0 += 1 } },
                clear: {}
            )
        } operation: {
            await #expect(throws: APIError.self) {
                _ = try await apiClient.send(APIRequest(path: "api/me/ping"))
            }
        }

        #expect(log.refreshCount == 0)
        #expect(invalidated.value == 1)
    }

    @Test
    func unauthenticatedRequestsNeverRefresh() async throws {
        let (apiClient, log) = Self.client()
        let store = SessionStore.inMemory(initial: Self.expired)
        StubURLProtocol.handler.setValue(Self.route(log: log) { (200, Self.rotatedPair) })

        await withDependencies {
            $0.sessionStore = store
            $0.sessionInvalidation = SessionInvalidation(markInvalid: {}, clear: {})
        } operation: {
            await #expect(throws: APIError.self) {
                _ = try await apiClient.send(
                    APIRequest(path: "api/auth/login", authorization: .unauthenticated)
                )
            }
        }

        // A 401 from login means bad credentials, not a stale token.
        #expect(log.refreshCount == 0)
    }
}

// MARK: - Stub transport

/// Answers every request from a closure so `APIClient.live` can be driven
/// end to end, including its retry.
final class StubURLProtocol: URLProtocol {
    /// Counts the requests the client actually put on the wire.
    final class Log: Sendable {
        private let paths = LockIsolated<[String]>([])

        func record(_ path: String) { paths.withValue { $0.append(path) } }
        var refreshCount: Int { paths.value.filter { $0.hasSuffix("api/auth/token/refresh") }.count }
    }

    static let handler = LockIsolated<(@Sendable (URLRequest) -> (Int, Data))?>(nil)

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let handler = Self.handler.value else {
            client?.urlProtocol(self, didFailWithError: URLError(.unsupportedURL))
            return
        }
        let (status, data) = handler(request)
        let response = HTTPURLResponse(
            url: request.url!,
            statusCode: status,
            httpVersion: "HTTP/1.1",
            headerFields: ["Content-Type": "application/json"]
        )!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: data)
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}

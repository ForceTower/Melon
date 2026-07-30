import ComposableArchitecture
import Foundation

private let log = Log.scoped("APIClient")

/// Production origin of the Melon API (`apps/api`).
enum MelonAPI {
    /// Debug builds honor a `debug_api_base_url` UserDefaults override so a
    /// simulator can point at a local api/proxy:
    /// `xcrun simctl spawn booted defaults write dev.forcetower.unes.ios debug_api_base_url http://localhost:3333`
    /// (delete the key to go back to production).
    static var baseURL: URL {
        #if DEBUG
        if let override = UserDefaults.standard.string(forKey: "debug_api_base_url"),
           let url = URL(string: override) {
            return url
        }
        #endif
        return URL(string: "https://melon.forcetower.dev")!
    }
}

struct APIRequest: Sendable {
    var method = "GET"
    var path: String
    var query: [URLQueryItem] = []
    var body: Data?
    var authorization: APIAuthorization = .session
}

/// How a request authenticates against apps/api.
enum APIAuthorization: Equatable, Sendable {
    /// Bearer token from the persisted session — the default.
    case session
    /// No Authorization header (login, passkey handshake).
    case unauthenticated
    /// Explicit token, for the window where a token exists but no session
    /// does yet (legacy-app migration).
    case bearer(String)
}

@DependencyClient
struct APIClient: Sendable {
    var send: @Sendable (_ request: APIRequest) async throws -> Data
}

/// Every apps/api response is wrapped in `{ ok, message, data, error }`.
private struct APIEnvelope<T: Decodable>: Decodable {
    let ok: Bool
    let message: String?
    let data: T?
}

extension APIClient {
    func get<T: Decodable>(
        _ type: T.Type = T.self,
        from path: String,
        query: [URLQueryItem] = [],
        authorization: APIAuthorization = .session
    ) async throws -> T {
        try Self.unwrap(await send(APIRequest(path: path, query: query, authorization: authorization)))
    }

    func post<T: Decodable>(
        _ type: T.Type = T.self,
        to path: String,
        query: [URLQueryItem] = [],
        body: some Encodable & Sendable,
        authorization: APIAuthorization = .session
    ) async throws -> T {
        let request = APIRequest(
            method: "POST",
            path: path,
            query: query,
            body: try JSONEncoder().encode(body),
            authorization: authorization
        )
        return try Self.unwrap(await send(request))
    }

    /// POST without a payload on either side (e.g. `api/me/ping`).
    func post(to path: String) async throws {
        _ = try await send(APIRequest(method: "POST", path: path))
    }

    /// POST whose response carries no data payload (e.g. token registration).
    func post(to path: String, query: [URLQueryItem] = [], body: some Encodable & Sendable) async throws {
        _ = try await send(APIRequest(
            method: "POST",
            path: path,
            query: query,
            body: try JSONEncoder().encode(body)
        ))
    }

    func patch<T: Decodable>(
        _ type: T.Type = T.self,
        at path: String,
        body: some Encodable & Sendable
    ) async throws -> T {
        let request = APIRequest(
            method: "PATCH",
            path: path,
            body: try JSONEncoder().encode(body)
        )
        return try Self.unwrap(await send(request))
    }

    /// PATCH whose response carries no data payload.
    func patch(at path: String, query: [URLQueryItem] = [], body: some Encodable & Sendable) async throws {
        _ = try await send(APIRequest(
            method: "PATCH",
            path: path,
            query: query,
            body: try JSONEncoder().encode(body)
        ))
    }

    /// DELETE whose response carries no data payload.
    func delete(_ path: String, query: [URLQueryItem] = []) async throws {
        _ = try await send(APIRequest(method: "DELETE", path: path, query: query))
    }

    /// DELETE with a request body (e.g. push identifier removal).
    func delete(_ path: String, body: some Encodable & Sendable) async throws {
        _ = try await send(APIRequest(
            method: "DELETE",
            path: path,
            body: try JSONEncoder().encode(body)
        ))
    }

    private static func unwrap<T: Decodable>(_ data: Data) throws -> T {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let envelope = try decoder.decode(APIEnvelope<T>.self, from: data)
        guard envelope.ok, let payload = envelope.data else {
            throw APIError.emptyEnvelope
        }
        return payload
    }
}

extension APIClient: DependencyKey {
    static let liveValue = APIClient.live()
    static let testValue = APIClient()
}

extension DependencyValues {
    var apiClient: APIClient {
        get { self[APIClient.self] }
        set { self[APIClient.self] = newValue }
    }
}

extension APIClient {
    static func live(
        baseURL: URL = MelonAPI.baseURL,
        session: URLSession = .shared
    ) -> APIClient {
        let refresher = TokenRefresher(baseURL: baseURL, session: session)
        return APIClient(send: { apiRequest in
            @Dependency(\.sessionStore) var sessionStore

            var url = baseURL.appending(path: apiRequest.path)
            if !apiRequest.query.isEmpty {
                url.append(queryItems: apiRequest.query)
            }

            /// Builds and fires the request from scratch so the retry below
            /// picks up whatever token the refresh just persisted.
            func fire() async throws -> (data: Data, http: HTTPURLResponse, sentToken: String?) {
                var request = URLRequest(url: url)
                request.httpMethod = apiRequest.method
                request.setValue(MachineIdentity.id, forHTTPHeaderField: "X-Machine-Id")
                if let body = apiRequest.body {
                    request.httpBody = body
                    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                }
                let sentToken: String? = switch apiRequest.authorization {
                case .session: sessionStore.current()?.accessToken
                case .unauthenticated: nil
                case let .bearer(token): token
                }
                if let sentToken {
                    request.setValue("Bearer \(sentToken)", forHTTPHeaderField: "Authorization")
                }

                let (data, response) = try await session.data(for: request)
                guard let http = response as? HTTPURLResponse else {
                    log.warn("request failed method=\(apiRequest.method) path=\(apiRequest.path): invalid response")
                    throw APIError.invalidResponse
                }
                return (data, http, sentToken)
            }

            log.debug("request start method=\(apiRequest.method) path=\(apiRequest.path)")
            var attempt = try await fire()

            // The API answers 401 for an expired token and an invalid one
            // alike, so trying the refresh is the only way to tell them apart.
            if attempt.http.statusCode == 401,
               apiRequest.authorization == .session,
               let stale = attempt.sentToken,
               await refresher.refresh(stale: stale) {
                log.info("retrying with a refreshed token method=\(apiRequest.method) path=\(apiRequest.path)")
                attempt = try await fire()
            }

            let (data, http, _) = attempt
            // 412 is the sync family's "no valid upstream credential" gate —
            // raise the banner now rather than waiting for the next poll.
            if http.statusCode == 412, apiRequest.authorization == .session {
                @Dependency(\.credentialInvalidation) var credentialInvalidation
                log.warn("precondition failed path=\(apiRequest.path): upstream credentials need re-auth")
                credentialInvalidation.markInvalid()
            }
            guard 200..<300 ~= http.statusCode else {
                let message = try? JSONDecoder().decode(ErrorBody.self, from: data).message
                log.warn("request failed method=\(apiRequest.method) path=\(apiRequest.path) status=\(http.statusCode)")
                throw APIError.server(status: http.statusCode, message: message)
            }
            log.debug("request ok method=\(apiRequest.method) path=\(apiRequest.path) status=\(http.statusCode)")
            return data
        })
    }
}

/// Serializes token rotation against `api/auth/token/refresh`. The endpoint
/// burns the refresh token on first use and hands back a new pair, so two
/// concurrent refreshes would rotate past each other and strand the session.
private actor TokenRefresher {
    private let baseURL: URL
    private let session: URLSession
    private var inFlight: Task<Bool, Never>?
    /// Access token whose pair the server already rejected. Without this latch
    /// a structurally dead session re-attempts the refresh on every request.
    private var burnedAccessToken: String?

    init(baseURL: URL, session: URLSession) {
        self.baseURL = baseURL
        self.session = session
    }

    /// Returns true when a usable access token is waiting in the session store.
    func refresh(stale: String) async -> Bool {
        @Dependency(\.sessionStore) var sessionStore

        guard let current = sessionStore.current() else { return false }
        // Another caller rotated the pair while this request sat on its 401.
        guard current.accessToken == stale else { return true }
        guard burnedAccessToken != stale else { return false }

        if let inFlight { return await inFlight.value }
        let task = Task { await rotate(from: current) }
        inFlight = task
        let rotated = await task.value
        inFlight = nil
        return rotated
    }

    private func rotate(from current: Session) async -> Bool {
        @Dependency(\.sessionStore) var sessionStore
        @Dependency(\.sessionInvalidation) var sessionInvalidation

        // Sessions adopted from the legacy app can carry an empty refresh token.
        guard !current.refreshToken.isEmpty else {
            log.warn("refresh skipped: session has no refresh token")
            burn(current.accessToken, using: sessionInvalidation)
            return false
        }

        var request = URLRequest(url: baseURL.appending(path: "api/auth/token/refresh"))
        request.httpMethod = "POST"
        request.setValue(MachineIdentity.id, forHTTPHeaderField: "X-Machine-Id")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONEncoder().encode(RefreshRequestDTO(
            accessToken: current.accessToken,
            refreshToken: current.refreshToken
        ))

        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else { return false }
            guard 200..<300 ~= http.statusCode else {
                // 400 covers a spent, revoked or foreign refresh token — the
                // pair is gone and only a fresh login recovers it. A 5xx is
                // the server's problem, so leave the pair retryable.
                if 400..<500 ~= http.statusCode {
                    log.warn("refresh rejected status=\(http.statusCode): session needs a new login")
                    burn(current.accessToken, using: sessionInvalidation)
                } else {
                    log.warn("refresh failed status=\(http.statusCode), staying retryable")
                }
                return false
            }

            let envelope = try JSONDecoder().decode(APIEnvelope<RefreshResponseDTO>.self, from: data)
            guard envelope.ok, let rotated = envelope.data else {
                log.warn("refresh returned an empty envelope, staying retryable")
                return false
            }
            try sessionStore.save(Session(
                accessToken: rotated.accessToken,
                refreshToken: rotated.refreshToken,
                user: current.user
            ))
            sessionInvalidation.clear()
            log.info("token refreshed userId=\(current.user.id)")
            return true
        } catch is CancellationError {
            return false
        } catch {
            // Transport or decode failure — deliberately not burned so the next
            // 401 tries again.
            log.warn("refresh failed, staying retryable", error: error)
            return false
        }
    }

    private func burn(_ accessToken: String, using invalidation: SessionInvalidation) {
        burnedAccessToken = accessToken
        invalidation.markInvalid()
    }
}

private struct ErrorBody: Decodable {
    let message: String?
}

import ComposableArchitecture
import Foundation

private let log = Log.scoped("APIClient")

/// Production origin of the Melon API (`apps/api`).
enum MelonAPI {
    static let baseURL = URL(string: "https://melon.forcetower.dev")!
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
        body: some Encodable & Sendable,
        authorization: APIAuthorization = .session
    ) async throws -> T {
        let request = APIRequest(
            method: "POST",
            path: path,
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
    func post(to path: String, body: some Encodable & Sendable) async throws {
        _ = try await send(APIRequest(method: "POST", path: path, body: try JSONEncoder().encode(body)))
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
        APIClient(send: { apiRequest in
            @Dependency(\.sessionStore) var sessionStore

            var url = baseURL.appending(path: apiRequest.path)
            if !apiRequest.query.isEmpty {
                url.append(queryItems: apiRequest.query)
            }
            var request = URLRequest(url: url)
            request.httpMethod = apiRequest.method
            if let body = apiRequest.body {
                request.httpBody = body
                request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            }
            switch apiRequest.authorization {
            case .session:
                if let token = sessionStore.current()?.accessToken {
                    request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                }
            case .unauthenticated:
                break
            case let .bearer(token):
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            }

            log.debug("request start method=\(apiRequest.method) path=\(apiRequest.path)")
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else {
                log.warn("request failed method=\(apiRequest.method) path=\(apiRequest.path): invalid response")
                throw APIError.invalidResponse
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

private struct ErrorBody: Decodable {
    let message: String?
}

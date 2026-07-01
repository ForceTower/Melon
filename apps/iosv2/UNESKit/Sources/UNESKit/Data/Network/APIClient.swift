import ComposableArchitecture
import Foundation

struct APIRequest: Sendable {
    var method = "GET"
    var path: String
    var body: Data?
    var authenticated = true
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
    func get<T: Decodable>(_ type: T.Type = T.self, from path: String) async throws -> T {
        try Self.unwrap(await send(APIRequest(path: path)))
    }

    func post<T: Decodable>(
        _ type: T.Type = T.self,
        to path: String,
        body: some Encodable & Sendable,
        authenticated: Bool = true
    ) async throws -> T {
        let request = APIRequest(
            method: "POST",
            path: path,
            body: try JSONEncoder().encode(body),
            authenticated: authenticated
        )
        return try Self.unwrap(await send(request))
    }

    /// POST without a payload on either side (e.g. `api/me/ping`).
    func post(to path: String) async throws {
        _ = try await send(APIRequest(method: "POST", path: path))
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
        baseURL: URL = URL(string: "https://melon.forcetower.dev")!,
        session: URLSession = .shared
    ) -> APIClient {
        APIClient(send: { apiRequest in
            @Dependency(\.sessionStore) var sessionStore

            var request = URLRequest(url: baseURL.appending(path: apiRequest.path))
            request.httpMethod = apiRequest.method
            if let body = apiRequest.body {
                request.httpBody = body
                request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            }
            if apiRequest.authenticated, let token = sessionStore.current()?.accessToken {
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            }

            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else { throw APIError.invalidResponse }
            guard 200..<300 ~= http.statusCode else {
                let message = try? JSONDecoder().decode(ErrorBody.self, from: data).message
                throw APIError.server(status: http.statusCode, message: message)
            }
            return data
        })
    }
}

private struct ErrorBody: Decodable {
    let message: String?
}

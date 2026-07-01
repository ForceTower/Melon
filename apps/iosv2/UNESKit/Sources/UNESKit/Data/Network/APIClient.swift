import ComposableArchitecture
import Foundation

@DependencyClient
struct APIClient: Sendable {
    var data: @Sendable (_ path: String) async throws -> Data
}

extension APIClient {
    func get<T: Decodable>(_ type: T.Type = T.self, from path: String) async throws -> T {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try decoder.decode(T.self, from: await data(path))
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
        APIClient(data: { path in
            let url = baseURL.appending(path: path)
            var request = URLRequest(url: url)
            request.httpMethod = "GET"
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else { throw APIError.invalidResponse }
            guard 200..<300 ~= http.statusCode else { throw APIError.badStatus(http.statusCode) }
            return data
        })
    }
}

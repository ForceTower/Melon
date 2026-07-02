import ComposableArchitecture
import Foundation

extension EnrollmentRepository: DependencyKey {
    static let liveValue = EnrollmentRepository(
        window: {
            @Dependency(\.apiClient) var apiClient
            return try await mapFailure {
                let dto = try await apiClient.get(EnrollmentWindowResponseDTO.self, from: "api/enrollment/window")
                return dto.domain
            }
        },
        offers: {
            @Dependency(\.apiClient) var apiClient
            return try await mapFailure {
                let dto = try await apiClient.get(EnrollmentOffersResponseDTO.self, from: "api/enrollment/offers")
                return dto.domain
            }
        },
        submit: { selections in
            @Dependency(\.apiClient) var apiClient
            try await mapFailure {
                _ = try await apiClient.post(
                    EnrollmentSubmitResponseDTO.self,
                    to: "api/enrollment/submit",
                    body: EnrollmentSubmitRequestDTO(selections)
                )
            }
        }
    )
}

private func mapFailure<T>(_ work: () async throws -> T) async throws -> T {
    do {
        return try await work()
    } catch {
        throw EnrollmentFailure(error)
    }
}

extension EnrollmentFailure {
    fileprivate init(_ error: any Error) {
        switch error {
        case APIError.server(401, _):
            self = .sessionExpired
        case let APIError.server(_, message):
            self = .server(message)
        case is URLError:
            self = .network
        default:
            self = .server(nil)
        }
    }
}

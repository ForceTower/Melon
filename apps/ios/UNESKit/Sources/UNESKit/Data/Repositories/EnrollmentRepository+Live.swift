import ComposableArchitecture
import Foundation

private let log = Log.scoped("EnrollmentRepository")

extension EnrollmentRepository: DependencyKey {
    static let liveValue = EnrollmentRepository(
        window: {
            @Dependency(\.apiClient) var apiClient
            log.debug("window start")
            do {
                let result = try await mapFailure {
                    let dto = try await apiClient.get(EnrollmentWindowResponseDTO.self, from: "api/enrollment/window")
                    return dto.domain
                }
                log.info("window ok available=\(result != nil)")
                return result
            } catch let failure as EnrollmentFailure {
                switch failure {
                case .sessionExpired:
                    log.warn("window unauthorized")
                case .network:
                    log.warn("window transport failure")
                case let .server(message):
                    log.warn("window server failure message=\(message ?? "<none>")")
                }
                throw failure
            }
        },
        offers: {
            @Dependency(\.apiClient) var apiClient
            log.debug("offers start")
            do {
                let result = try await mapFailure {
                    let dto = try await apiClient.get(EnrollmentOffersResponseDTO.self, from: "api/enrollment/offers")
                    return dto.domain
                }
                log.info("offers ok count=\(result.count)")
                return result
            } catch let failure as EnrollmentFailure {
                switch failure {
                case .sessionExpired:
                    log.warn("offers unauthorized")
                case .network:
                    log.warn("offers transport failure")
                case let .server(message):
                    log.warn("offers server failure message=\(message ?? "<none>")")
                }
                throw failure
            }
        },
        submit: { selections in
            @Dependency(\.apiClient) var apiClient
            log.debug("submit start picks=\(selections.count)")
            do {
                try await mapFailure {
                    _ = try await apiClient.post(
                        EnrollmentSubmitResponseDTO.self,
                        to: "api/enrollment/submit",
                        body: EnrollmentSubmitRequestDTO(selections)
                    )
                }
                log.info("submit ok picks=\(selections.count)")
            } catch let failure as EnrollmentFailure {
                switch failure {
                case .sessionExpired:
                    log.warn("submit unauthorized")
                case .network:
                    log.warn("submit transport failure")
                case let .server(message):
                    log.warn("submit server failure message=\(message ?? "<none>")")
                }
                throw failure
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

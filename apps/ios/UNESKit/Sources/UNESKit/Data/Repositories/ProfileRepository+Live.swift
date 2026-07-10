import ComposableArchitecture
import Foundation

private let log = Log.scoped("ProfileRepository")

extension ProfileRepository: DependencyKey {
    static let liveValue = ProfileRepository(
        current: {
            @Dependency(\.apiClient) var apiClient
            log.debug("current start")
            do {
                let dto = try await apiClient.get(ProfileDTO.self, from: "api/sync/profile")
                log.info("current ok userId=\(dto.user.id)")
                return dto.domain
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("current unauthorized")
                case let APIError.server(status, message):
                    log.warn("current server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("current 2xx envelope had null data")
                case is URLError:
                    log.warn("current transport failure", error: error)
                default:
                    log.error("current failed", error: error)
                }
                throw error
            }
        }
    )
}

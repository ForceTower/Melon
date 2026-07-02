import ComposableArchitecture
import Foundation

private let log = Log.scoped("SettingsRepository")

extension SettingsRepository: DependencyKey {
    static let liveValue = SettingsRepository(
        account: {
            @Dependency(\.apiClient) var apiClient
            log.debug("account start")
            do {
                let dto = try await apiClient.get(SettingsProfileDTO.self, from: "api/sync/profile")
                log.info("account ok userId=\(dto.user.id)")
                return dto.domain
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("account unauthorized")
                case let APIError.server(status, message):
                    log.warn("account server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("account 2xx envelope had null data")
                case is URLError:
                    log.warn("account transport failure", error: error)
                default:
                    log.error("account failed", error: error)
                }
                throw error
            }
        },
        credentials: {
            @Dependency(\.apiClient) var apiClient
            log.debug("credentials start")
            do {
                let dto = try await apiClient.get(CredentialsDTO.self, from: "api/me/credentials")
                log.info("credentials ok present=\(dto.credentials != nil)")
                return dto.credentials.map {
                    AccountCredentials(username: $0.username, password: $0.password)
                }
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("credentials unauthorized")
                case let APIError.server(status, message):
                    log.warn("credentials server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("credentials 2xx envelope had null data")
                case is URLError:
                    log.warn("credentials transport failure", error: error)
                default:
                    log.error("credentials failed", error: error)
                }
                throw error
            }
        },
        update: { change in
            @Dependency(\.apiClient) var apiClient
            log.debug("update start field=\(change.field)")
            do {
                let dto = try await apiClient.patch(
                    UpdateSettingsDTO.self,
                    at: "api/me/settings",
                    body: UserSettingsPatchDTO(change)
                )
                log.info("update ok field=\(change.field)")
                return dto.settings.domain
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("update unauthorized")
                case let APIError.server(status, message):
                    log.warn("update server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("update 2xx envelope had null data")
                case is URLError:
                    log.warn("update transport failure", error: error)
                default:
                    log.error("update failed", error: error)
                }
                throw error
            }
        }
    )
}

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
            @Dependency(\.sessionStore) var sessionStore
            @Dependency(\.credentialStatusRepository) var credentialStatusRepository
            if let stored = sessionStore.upstreamCredentials() {
                log.debug("credentials from device")
                return stored
            }
            // Nothing entered on this device, as after a passkey sign-in: show the username alone.
            let health = try await credentialStatusRepository.current()
            log.debug("credentials username-only present=\(health.username != nil)")
            return health.username.map { AccountCredentials(username: $0, password: nil) }
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

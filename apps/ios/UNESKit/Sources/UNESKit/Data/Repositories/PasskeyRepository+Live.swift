import ComposableArchitecture
import Foundation

private let log = Log.scoped("PasskeyRepository")

extension PasskeyRepository: DependencyKey {
    static let liveValue = PasskeyRepository(
        credentials: {
            @Dependency(\.apiClient) var apiClient
            log.debug("credentials start")
            do {
                let dto = try await apiClient.get(PasskeyCredentialsDTO.self, from: "api/passkey/credentials")
                log.info("credentials ok count=\(dto.credentials.count)")
                return dto.domain
            } catch {
                logFailure("credentials", error)
                throw error
            }
        },
        registrationOptions: {
            @Dependency(\.apiClient) var apiClient
            log.debug("registrationOptions start")
            do {
                let dto = try await apiClient.post(
                    PasskeyRegistrationOptionsDTO.self,
                    to: "api/passkey/register/options",
                    body: EmptyBody()
                )
                log.info("registrationOptions ok rpId=\(dto.rp.id)")
                return dto.domain
            } catch {
                logFailure("registrationOptions", error)
                throw error
            }
        },
        register: { attestation, deviceName in
            @Dependency(\.apiClient) var apiClient
            log.debug("register start")
            do {
                try await apiClient.post(
                    to: "api/passkey/register/verify",
                    body: PasskeyRegisterRequestDTO(attestation: attestation, deviceName: deviceName)
                )
                log.info("register ok")
            } catch {
                logFailure("register", error)
                throw error
            }
        },
        rename: { id, deviceName in
            @Dependency(\.apiClient) var apiClient
            log.debug("rename start")
            do {
                try await apiClient.patch(
                    at: "api/passkey/credentials",
                    query: [URLQueryItem(name: "id", value: id)],
                    body: PasskeyRenameRequestDTO(deviceName: deviceName)
                )
                log.info("rename ok")
            } catch {
                logFailure("rename", error)
                throw error
            }
        },
        delete: { id in
            @Dependency(\.apiClient) var apiClient
            log.debug("delete start")
            do {
                try await apiClient.delete(
                    "api/passkey/credentials",
                    query: [URLQueryItem(name: "id", value: id)]
                )
                log.info("delete ok")
            } catch {
                logFailure("delete", error)
                throw error
            }
        }
    )

    private static func logFailure(_ op: String, _ error: any Error) {
        switch error {
        case APIError.server(401, _):
            log.warn("\(op) unauthorized")
        case let APIError.server(status, message):
            log.warn("\(op) server \(status) message=\(message ?? "<none>")")
        case APIError.emptyEnvelope:
            log.warn("\(op) 2xx envelope had null data")
        case is URLError:
            log.warn("\(op) transport failure", error: error)
        default:
            log.error("\(op) failed", error: error)
        }
    }
}

/// Empty JSON payload for a POST whose route takes no body.
private struct EmptyBody: Encodable, Sendable {}

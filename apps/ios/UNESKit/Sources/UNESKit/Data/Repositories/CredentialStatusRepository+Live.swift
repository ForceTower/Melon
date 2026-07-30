import ComposableArchitecture
import Foundation

private let log = Log.scoped("CredentialStatus")

private struct CredentialStatusDTO: Decodable {
    let credentials: Health

    struct Health: Decodable {
        let status: String
        let username: String?
    }
}

private struct ReauthRequestDTO: Encodable {
    let password: String
    let captchaToken: String?
}

/// `errorData` on a portal-path rejection: the portal can't tell a wrong
/// password from a missing captcha, so it reports whether one was even sent.
private struct ReauthErrorDTO: Decodable {
    let error: Payload?

    struct Payload: Decodable {
        let captchaRequired: Bool?
    }
}

extension CredentialStatusRepository: DependencyKey {
    static let liveValue = CredentialStatusRepository(
        current: {
            @Dependency(\.apiClient) var apiClient
            do {
                let dto: CredentialStatusDTO = try await apiClient.get(from: "api/me/status")
                let health = CredentialHealth(
                    status: CredentialStatus(rawValue: dto.credentials.status) ?? .ok,
                    username: dto.credentials.username
                )
                log.debug("status ok credentials=\(health.status.rawValue)")
                return health
            } catch {
                log.warn("status poll failed", error: error)
                throw error
            }
        },
        reauthenticate: { password, captchaToken in
            @Dependency(\.apiClient) var apiClient
            log.info("reauth submit captcha=\(captchaToken != nil)")
            do {
                _ = try await apiClient.send(APIRequest(
                    method: "POST",
                    path: "api/me/credentials",
                    body: try JSONEncoder().encode(
                        ReauthRequestDTO(password: password, captchaToken: captchaToken)
                    )
                ))
                log.info("reauth ok")
            } catch {
                throw ReauthFailure(error)
            }
        }
    )
}

extension ReauthFailure {
    fileprivate init(_ error: any Error) {
        switch error {
        case let APIError.server(400, message):
            log.warn("reauth rejected: \(message ?? "<none>")")
            self = .invalidPassword
        case APIError.server(503, _):
            log.warn("reauth upstream unavailable")
            self = .upstreamUnavailable
        case let APIError.server(status, message):
            log.warn("reauth server \(status) message=\(message ?? "<none>")")
            self = .server(message)
        case is URLError:
            log.warn("reauth transport failure", error: error)
            self = .network
        default:
            log.error("reauth failed", error: error)
            self = .server(nil)
        }
    }
}

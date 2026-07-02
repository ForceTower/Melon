import ComposableArchitecture
import Foundation

private let log = Log.scoped("AuthRepository")

extension AuthRepository: DependencyKey {
    static let liveValue = AuthRepository(
        login: { username, password in
            @Dependency(\.apiClient) var apiClient
            @Dependency(\.sessionStore) var sessionStore

            log.info("login attempt username=\(username)")
            do {
                let dto: LoginResponseDTO = try await apiClient.post(
                    to: "api/auth/login",
                    body: LoginRequestDTO(username: username, password: password),
                    authenticated: false
                )
                let session = dto.domain
                try sessionStore.save(session)
                log.info("login ok userId=\(session.user.id)")
                return session
            } catch {
                throw AuthError(loginFailure: error)
            }
        },
        beginPasskeyLogin: {
            @Dependency(\.apiClient) var apiClient

            log.info("beginPasskeyLogin attempt")
            do {
                let dto: PasskeyOptionsResponseDTO = try await apiClient.post(
                    to: "api/passkey/authenticate/options",
                    body: PasskeyOptionsRequestDTO(username: nil),
                    authenticated: false
                )
                let challenge = dto.domain
                log.info("beginPasskeyLogin ok sessionId=\(challenge.sessionId)")
                return challenge
            } catch {
                throw AuthError(passkeyFailure: error)
            }
        },
        completePasskeyLogin: { sessionId, assertion in
            @Dependency(\.apiClient) var apiClient
            @Dependency(\.sessionStore) var sessionStore

            log.info("completePasskeyLogin attempt sessionId=\(sessionId)")
            do {
                let dto: LoginResponseDTO = try await apiClient.post(
                    to: "api/passkey/authenticate/verify",
                    body: PasskeyVerifyRequestDTO(sessionId: sessionId, assertion: assertion),
                    authenticated: false
                )
                let session = dto.domain
                try sessionStore.save(session)
                log.info("completePasskeyLogin ok userId=\(session.user.id)")
                return session
            } catch {
                throw AuthError(passkeyFailure: error)
            }
        }
    )
}

extension AuthError {
    fileprivate init(loginFailure error: any Error) {
        switch error {
        case APIError.server(400, _):
            log.warn("login failed: invalid credentials")
            self = .invalidCredentials
        case let APIError.server(status, message):
            log.warn("login failed: server \(status) message=\(message ?? "<none>")")
            self = .server(message)
        case is URLError:
            log.warn("login failed: transport", error: error)
            self = .network
        default:
            log.error("login failed: envelope deserialization", error: error)
            self = .server(nil)
        }
    }

    fileprivate init(passkeyFailure error: any Error) {
        switch error {
        case let APIError.server(status, message):
            log.warn("passkey failed: server \(status) message=\(message ?? "<none>")")
            self = .server(message)
        case is URLError:
            log.warn("passkey failed: transport", error: error)
            self = .network
        default:
            log.error("passkey failed: envelope deserialization", error: error)
            self = .server(nil)
        }
    }
}

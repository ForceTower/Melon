import ComposableArchitecture
import Foundation

extension AuthRepository: DependencyKey {
    static let liveValue = AuthRepository(
        login: { username, password in
            @Dependency(\.apiClient) var apiClient
            @Dependency(\.sessionStore) var sessionStore

            do {
                let dto: LoginResponseDTO = try await apiClient.post(
                    to: "api/auth/login",
                    body: LoginRequestDTO(username: username, password: password),
                    authenticated: false
                )
                let session = dto.domain
                try sessionStore.save(session)
                return session
            } catch {
                throw AuthError(loginFailure: error)
            }
        },
        beginPasskeyLogin: {
            @Dependency(\.apiClient) var apiClient

            do {
                let dto: PasskeyOptionsResponseDTO = try await apiClient.post(
                    to: "api/passkey/authenticate/options",
                    body: PasskeyOptionsRequestDTO(username: nil),
                    authenticated: false
                )
                return dto.domain
            } catch {
                throw AuthError(passkeyFailure: error)
            }
        },
        completePasskeyLogin: { sessionId, assertion in
            @Dependency(\.apiClient) var apiClient
            @Dependency(\.sessionStore) var sessionStore

            do {
                let dto: LoginResponseDTO = try await apiClient.post(
                    to: "api/passkey/authenticate/verify",
                    body: PasskeyVerifyRequestDTO(sessionId: sessionId, assertion: assertion),
                    authenticated: false
                )
                let session = dto.domain
                try sessionStore.save(session)
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
            self = .invalidCredentials
        case let APIError.server(_, message):
            self = .server(message)
        case is URLError:
            self = .network
        default:
            self = .server(nil)
        }
    }

    fileprivate init(passkeyFailure error: any Error) {
        switch error {
        case let APIError.server(_, message):
            self = .server(message)
        case is URLError:
            self = .network
        default:
            self = .server(nil)
        }
    }
}

import Foundation

// MARK: - api/auth/login

struct LoginRequestDTO: Encodable {
    let username: String
    let password: String
}

struct LoginResponseDTO: Decodable {
    let accessToken: String
    let refreshToken: String
    let user: UserDTO

    struct UserDTO: Decodable {
        let id: String
        let name: String
        let imageUrl: String?
    }
}

extension LoginResponseDTO {
    var domain: Session {
        Session(
            accessToken: accessToken,
            refreshToken: refreshToken,
            user: SessionUser(id: user.id, name: user.name, imageUrl: user.imageUrl)
        )
    }
}

// MARK: - api/auth/token/refresh

/// The endpoint validates the expired access token's signature to recover the
/// subject, so both halves of the pair have to be sent.
struct RefreshRequestDTO: Encodable {
    let accessToken: String
    let refreshToken: String
}

/// The refresh response carries no `user` — the caller keeps the one it has.
struct RefreshResponseDTO: Decodable {
    let accessToken: String
    let refreshToken: String
}

// MARK: - api/passkey/authenticate

struct PasskeyOptionsRequestDTO: Encodable {
    let username: String?
}

struct PasskeyOptionsResponseDTO: Decodable {
    let sessionId: String
    let options: OptionsDTO

    struct OptionsDTO: Decodable {
        let challenge: String
        let rpId: String
        let allowCredentials: [AllowedCredentialDTO]?
    }

    struct AllowedCredentialDTO: Decodable {
        let id: String
    }
}

extension PasskeyOptionsResponseDTO {
    var domain: PasskeyChallenge {
        PasskeyChallenge(
            sessionId: sessionId,
            challenge: options.challenge,
            rpId: options.rpId,
            allowedCredentialIds: options.allowCredentials?.map(\.id) ?? []
        )
    }
}

struct PasskeyVerifyRequestDTO: Encodable {
    let sessionId: String
    let response: ResponseDTO

    struct ResponseDTO: Encodable {
        let id: String
        let rawId: String
        var type = "public-key"
        let authenticatorAttachment: String?
        let clientDataJSON: String
        let authenticatorData: String
        let signature: String
        let userHandle: String?
    }

    init(sessionId: String, assertion: PasskeyAssertion) {
        self.sessionId = sessionId
        self.response = ResponseDTO(
            id: assertion.id,
            rawId: assertion.rawId,
            authenticatorAttachment: assertion.authenticatorAttachment,
            clientDataJSON: assertion.clientDataJSON,
            authenticatorData: assertion.authenticatorData,
            signature: assertion.signature,
            userHandle: assertion.userHandle
        )
    }
}

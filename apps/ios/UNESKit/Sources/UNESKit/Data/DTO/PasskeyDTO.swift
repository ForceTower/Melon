import Foundation

// MARK: - GET api/passkey/credentials

struct PasskeyCredentialsDTO: Decodable {
    let credentials: [CredentialDTO]

    struct CredentialDTO: Decodable {
        let id: String
        let deviceName: String?
        let deviceType: String
        let createdAt: Date
    }
}

extension PasskeyCredentialsDTO {
    var domain: [PasskeyCredential] {
        credentials.map {
            PasskeyCredential(
                id: $0.id,
                deviceName: $0.deviceName,
                isSynced: $0.deviceType == "multiDevice",
                createdAt: $0.createdAt
            )
        }
    }
}

// MARK: - POST api/passkey/register/options

struct PasskeyRegistrationOptionsDTO: Decodable {
    let challenge: String
    let rp: RP
    let user: User
    let pubKeyCredParams: [Param]
    let excludeCredentials: [Credential]?

    struct RP: Decodable {
        let id: String
        let name: String
    }

    struct User: Decodable {
        let id: String
        let name: String
        let displayName: String
    }

    struct Param: Decodable {
        let alg: Int
        let type: String
    }

    struct Credential: Decodable {
        let id: String
        let type: String
    }
}

extension PasskeyRegistrationOptionsDTO {
    var domain: PasskeyRegistrationOptions {
        PasskeyRegistrationOptions(
            challenge: challenge,
            rpId: rp.id,
            userId: user.id,
            userName: user.name,
            userDisplayName: user.displayName,
            excludedCredentialIds: excludeCredentials?.map(\.id) ?? [],
            algorithms: pubKeyCredParams.map(\.alg)
        )
    }
}

// MARK: - POST api/passkey/register/verify

/// Flat wire format expected by the server, which reshapes it into the nested
/// W3C `RegistrationResponseJSON` before verifying.
struct PasskeyRegisterRequestDTO: Encodable {
    let response: ResponseDTO
    let deviceName: String?

    struct ResponseDTO: Encodable {
        let id: String
        let rawId: String
        var type = "public-key"
        let authenticatorAttachment: String?
        let clientDataJSON: String
        let attestationObject: String
    }

    init(attestation: PasskeyAttestation, deviceName: String?) {
        self.response = ResponseDTO(
            id: attestation.id,
            rawId: attestation.rawId,
            authenticatorAttachment: attestation.authenticatorAttachment,
            clientDataJSON: attestation.clientDataJSON,
            attestationObject: attestation.attestationObject
        )
        self.deviceName = deviceName
    }
}

// MARK: - PATCH api/passkey/credentials?id=…

struct PasskeyRenameRequestDTO: Encodable {
    let deviceName: String
}

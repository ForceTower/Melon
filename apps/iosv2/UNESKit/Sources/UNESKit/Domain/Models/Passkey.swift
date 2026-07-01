import Foundation

/// WebAuthn assertion challenge from `api/passkey/authenticate/options`.
/// All binary fields are base64url strings, as sent by the server.
struct PasskeyChallenge: Equatable, Sendable {
    let sessionId: String
    let challenge: String
    let rpId: String
    let allowedCredentialIds: [String]
}

/// Signed assertion produced by the platform authenticator, base64url-encoded
/// for `api/passkey/authenticate/verify`.
struct PasskeyAssertion: Equatable, Sendable {
    let id: String
    let rawId: String
    let authenticatorAttachment: String?
    let clientDataJSON: String
    let authenticatorData: String
    let signature: String
    let userHandle: String?
}

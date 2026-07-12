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

// MARK: - Registration & management

/// A registered passkey, as listed by `api/passkey/credentials`.
struct PasskeyCredential: Equatable, Sendable, Identifiable {
    let id: String
    /// User-facing label captured at registration; nil for older credentials.
    let deviceName: String?
    /// `multiDevice` credentials roam through iCloud Keychain; `singleDevice`
    /// ones (e.g. a security key) stay bound to their hardware.
    let isSynced: Bool
    let createdAt: Date
}

/// Where a new passkey is stored — selects which system authenticator the OS
/// prompts for.
enum PasskeyTarget: Equatable, Sendable {
    /// A platform passkey on this device, backed by Face ID / Touch ID and
    /// synced through iCloud Keychain.
    case thisDevice
    /// A roaming security key (USB / NFC), bound to the hardware.
    case securityKey
}

/// WebAuthn creation options from `api/passkey/register/options`. Binary
/// fields are base64url strings, as sent by the server.
struct PasskeyRegistrationOptions: Equatable, Sendable {
    let challenge: String
    let rpId: String
    let userId: String
    let userName: String
    let userDisplayName: String
    let excludedCredentialIds: [String]
    /// COSE algorithm identifiers the relying party accepts (e.g. -7 = ES256).
    let algorithms: [Int]
}

/// Attestation produced by the platform authenticator, base64url-encoded for
/// `api/passkey/register/verify`.
struct PasskeyAttestation: Equatable, Sendable {
    let id: String
    let rawId: String
    let authenticatorAttachment: String?
    let clientDataJSON: String
    let attestationObject: String
}

extension PasskeyCredential {
    func renamed(to deviceName: String) -> PasskeyCredential {
        PasskeyCredential(id: id, deviceName: deviceName, isSynced: isSynced, createdAt: createdAt)
    }

    static let preview = PasskeyCredential(
        id: "pk_preview",
        deviceName: "iPhone 15 Pro",
        isSynced: true,
        createdAt: Date(timeIntervalSince1970: 1_738_540_800)
    )
}

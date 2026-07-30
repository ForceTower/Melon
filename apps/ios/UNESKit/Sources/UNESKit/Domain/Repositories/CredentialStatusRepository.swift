import ComposableArchitecture
import Foundation

/// Health of the SAGRES credentials the server syncs with. Distinct from the
/// Melon session: the app can be perfectly signed in while the portal password
/// it stored has stopped working, in which case everything keeps serving from
/// the mirror and nothing new ever arrives.
enum CredentialStatus: String, Equatable, Sendable, Codable {
    case ok
    case invalid
    /// No credentials on file (passkey-only accounts). Reported for
    /// completeness; the app doesn't surface it.
    case none
}

struct CredentialHealth: Equatable, Sendable, Codable {
    var status: CredentialStatus
    /// The SAGRES username, shown read-only in the re-auth sheet. The account
    /// itself can't be changed there.
    var username: String?
}

/// Reason a re-auth submission failed, so the sheet can pick between "wrong
/// password" and "try again later".
enum ReauthFailure: Error, Equatable {
    case invalidPassword
    /// The portal fails identically for a wrong password and a missing
    /// captcha, so the HTML path can only say "login failed".
    case captchaRequired
    case upstreamUnavailable
    case network
    case server(String?)
}

@DependencyClient
struct CredentialStatusRepository: Sendable {
    /// Polled on every foreground pulse alongside the semester pull.
    var current: @Sendable () async throws -> CredentialHealth
    /// Submits a new portal password. The username comes from the stored row
    /// server-side, so it isn't sent.
    var reauthenticate: @Sendable (_ password: String, _ captchaToken: String?) async throws -> Void
}

extension CredentialStatusRepository: TestDependencyKey {
    static let testValue = CredentialStatusRepository()

    static let previewValue = CredentialStatusRepository(
        current: { CredentialHealth(status: .ok, username: "20191234") },
        reauthenticate: { _, _ in }
    )
}

extension DependencyValues {
    var credentialStatusRepository: CredentialStatusRepository {
        get { self[CredentialStatusRepository.self] }
        set { self[CredentialStatusRepository.self] = newValue }
    }
}

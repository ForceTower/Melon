import ComposableArchitecture
import Foundation

/// Sibling of `SessionInvalidation`, and deliberately a separate latch: the
/// Melon session and the stored SAGRES password fail independently, and the
/// user fixes them in different ways.
extension CredentialInvalidation {
    static let storageKey = "credentials_invalid"
    /// Persisted alongside the flag so the sheet can show the account name on
    /// a cold start with no network. No periods — `@Shared(.appStorage)`
    /// degrades to notification-center observation for keys KVO can't take.
    static let usernameKey = "upstream_username"
}

/// Raised when the server reports the stored portal password no longer works.
/// Sync is paused server-side; the app keeps working on mirrored data.
@DependencyClient
struct CredentialInvalidation: Sendable {
    /// The polled answer from `api/me/status`, applied wholesale.
    var apply: @Sendable (CredentialHealth) -> Void
    /// A sync-family 412 — the flag without a username to go with it.
    var markInvalid: @Sendable () -> Void
    var clear: @Sendable () -> Void
    /// Logout: forgets the flag and the portal username with it, so the next
    /// account starts clean.
    var reset: @Sendable () -> Void
}

/// Writes go through `defaultAppStorage`, the store the features'
/// `@Shared(.appStorage)` reads observe.
extension CredentialInvalidation: DependencyKey {
    static let liveValue = CredentialInvalidation(
        apply: { health in
            @Dependency(\.defaultAppStorage) var defaults
            if let username = health.username {
                defaults.set(username, forKey: usernameKey)
            }
            switch health.status {
            case .invalid: defaults.set(true, forKey: storageKey)
            case .ok: defaults.set(false, forKey: storageKey)
            // Passkey-only accounts with nothing on file — not a problem to
            // nag about, and not a reason to clear a flag either.
            case .none: break
            case .unknown: break
            }
        },
        markInvalid: {
            @Dependency(\.defaultAppStorage) var defaults
            defaults.set(true, forKey: storageKey)
        },
        clear: {
            @Dependency(\.defaultAppStorage) var defaults
            defaults.set(false, forKey: storageKey)
        },
        reset: {
            @Dependency(\.defaultAppStorage) var defaults
            defaults.removeObject(forKey: storageKey)
            defaults.removeObject(forKey: usernameKey)
        }
    )

    static let testValue = CredentialInvalidation()

    static let previewValue = CredentialInvalidation(apply: { _ in }, markInvalid: {}, clear: {}, reset: {})
}

extension DependencyValues {
    var credentialInvalidation: CredentialInvalidation {
        get { self[CredentialInvalidation.self] }
        set { self[CredentialInvalidation.self] = newValue }
    }
}

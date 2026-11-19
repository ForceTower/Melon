import Foundation

enum AuthError: Error, Equatable {
    case invalidCredentials
    case network
    case passkeyUnavailable
    case cancelled
    case differentAccount
    case server(String?)

    /// User-facing message; `nil` when the failure should be silent (user cancelled).
    var message: String? {
        switch self {
        case .invalidCredentials: String.localized(.dataErrorInvalidCredentials)
        case .network: String.localized(.dataErrorNetwork)
        case .passkeyUnavailable: String.localized(.dataErrorPasskeyUnavailable)
        case .cancelled: nil
        case .differentAccount: String.localized(.dataErrorDifferentAccount)
        case .server: String.localized(.dataErrorSignInFailed)
        }
    }
}

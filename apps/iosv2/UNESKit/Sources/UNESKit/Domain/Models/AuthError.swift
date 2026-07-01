import Foundation

enum AuthError: Error, Equatable {
    case invalidCredentials
    case network
    case passkeyUnavailable
    case cancelled
    case server(String?)

    /// User-facing message; `nil` when the failure should be silent (user cancelled).
    var message: String? {
        switch self {
        case .invalidCredentials: "Usuário ou senha incorretos. Confira suas credenciais do SAGRES."
        case .network: "Sem conexão. Verifique sua internet e tente de novo."
        case .passkeyUnavailable: "Passkey não é suportada neste dispositivo."
        case .cancelled: nil
        case .server: "Não foi possível entrar agora. Tente novamente em instantes."
        }
    }
}

import Observation
@preconcurrency import Umbrella

@Observable
final class LoginViewModel {
    var studentId = ""
    var password = ""
    var showPassword = false
    var isLoading = false
    var errorMessage: String?

    private let loginUseCase: AuthLoginUseCase?

    init(loginUseCase: AuthLoginUseCase?) {
        self.loginUseCase = loginUseCase
    }

    var canSubmit: Bool {
        !studentId.isEmpty && !password.isEmpty && !isLoading
    }

    func submit() async -> Bool {
        guard canSubmit, let loginUseCase else { return false }

        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        let outcome: CommonOutcome<KotlinUnit, AuthLoginError>
        do {
            outcome = try await loginUseCase.invoke(username: studentId, password: password)
        } catch is CancellationError {
            return false
        } catch {
            errorMessage = Self.unexpectedMessage
            return false
        }

        switch onEnum(of: outcome) {
        case .ok:
            return true
        case .err(let wrapper):
            errorMessage = wrapper.error.map(Self.describe) ?? Self.unexpectedMessage
            return false
        }
    }

    private static let unexpectedMessage = "Algo deu errado. Tente novamente."

    private static func describe(_ error: any AuthLoginError) -> String {
        switch onEnum(of: error) {
        case .kind(let kind):
            switch kind {
            case .noConnection:
                return "Sem conexão. Verifique sua internet."
            case .invalidCredentials:
                return "Usuário ou senha incorretos."
            case .unexpected:
                return unexpectedMessage
            }
        case .server(let server):
            return server.message ?? "Erro no servidor."
        }
    }
}

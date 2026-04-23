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
    private let log = Log.scoped("LoginViewModel")

    init(loginUseCase: AuthLoginUseCase?) {
        self.loginUseCase = loginUseCase
    }

    var canSubmit: Bool {
        !studentId.isEmpty && !password.isEmpty && !isLoading
    }

    func submit() async -> SessionUser? {
        guard canSubmit, let loginUseCase else { return nil }

        log.info("login submit for student=\(studentId)")
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        let outcome: CommonOutcome<SessionUser, AuthLoginError>
        do {
            outcome = try await loginUseCase.invoke(username: studentId, password: password)
        } catch is CancellationError {
            log.info("login cancelled for student=\(studentId)")
            return nil
        } catch {
            log.error("login threw unexpectedly for student=\(studentId)", error: error)
            errorMessage = Self.unexpectedMessage
            return nil
        }

        switch onEnum(of: outcome) {
        case .ok(let user):
            log.info("login ok for student=\(studentId)")
            return user.value
        case .err(let wrapper):
            let rendered = wrapper.error.map { String(describing: $0) } ?? "<nil>"
            log.warn("login failed for student=\(studentId) err=\(rendered)")
            errorMessage = wrapper.error.map(Self.describe) ?? Self.unexpectedMessage
            return nil
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

import AuthenticationServices
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
    private let beginPasskeyLogin: AuthBeginPasskeyLoginUseCase?
    private let completePasskeyLogin: AuthCompletePasskeyLoginUseCase?
    private let log = Log.scoped("LoginViewModel")

    init(
        loginUseCase: AuthLoginUseCase?,
        beginPasskeyLogin: AuthBeginPasskeyLoginUseCase?,
        completePasskeyLogin: AuthCompletePasskeyLoginUseCase?
    ) {
        self.loginUseCase = loginUseCase
        self.beginPasskeyLogin = beginPasskeyLogin
        self.completePasskeyLogin = completePasskeyLogin
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

    func loginWithPasskey(anchor: ASPresentationAnchor) async -> SessionUser? {
        guard let beginPasskeyLogin, let completePasskeyLogin else { return nil }

        log.info("passkey login start")
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        // 1. Fetch challenge from server.
        let challenge: AuthPasskeyChallenge
        switch await beginPasskey(useCase: beginPasskeyLogin) {
        case .success(let value):
            challenge = value
        case .failure(let message):
            errorMessage = message
            return nil
        case .none:
            return nil
        }

        // 2. Drive ASAuthorization on the platform.
        let assertion: AuthPasskeyAssertion
        do {
            assertion = try await PasskeyAuthenticator().assert(challenge: challenge, anchor: anchor)
        } catch PasskeyError.cancelled {
            log.info("passkey cancelled by user")
            return nil
        } catch let passkeyError as PasskeyError {
            log.warn("passkey native flow failed: \(passkeyError.errorDescription ?? "<nil>")")
            errorMessage = passkeyError.errorDescription ?? Self.unexpectedMessage
            return nil
        } catch {
            log.error("passkey native flow threw", error: error)
            errorMessage = Self.unexpectedMessage
            return nil
        }

        // 3. Submit assertion to server, persist the session.
        let outcome: CommonOutcome<SessionUser, AuthLoginError>
        do {
            outcome = try await completePasskeyLogin.invoke(sessionId: challenge.sessionId, assertion: assertion)
        } catch is CancellationError {
            return nil
        } catch {
            log.error("passkey complete threw unexpectedly", error: error)
            errorMessage = Self.unexpectedMessage
            return nil
        }

        switch onEnum(of: outcome) {
        case .ok(let user):
            log.info("passkey ok")
            return user.value
        case .err(let wrapper):
            let rendered = wrapper.error.map { String(describing: $0) } ?? "<nil>"
            log.warn("passkey failed err=\(rendered)")
            errorMessage = wrapper.error.map(Self.describe) ?? Self.unexpectedMessage
            return nil
        }
    }

    private enum BeginResult {
        case success(AuthPasskeyChallenge)
        case failure(String)
        case none
    }

    private func beginPasskey(useCase: AuthBeginPasskeyLoginUseCase) async -> BeginResult {
        let outcome: CommonOutcome<AuthPasskeyChallenge, AuthLoginError>
        do {
            outcome = try await useCase.invoke(username: nil)
        } catch is CancellationError {
            return .none
        } catch {
            log.error("passkey begin threw unexpectedly", error: error)
            return .failure(Self.unexpectedMessage)
        }

        switch onEnum(of: outcome) {
        case .ok(let challenge):
            guard let value = challenge.value else { return .failure(Self.unexpectedMessage) }
            return .success(value)
        case .err(let wrapper):
            return .failure(wrapper.error.map(Self.describe) ?? Self.unexpectedMessage)
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

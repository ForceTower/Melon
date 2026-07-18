import ComposableArchitecture
import Foundation

@Reducer
struct LoginFeature {
    static let forgotPasswordURL = URL(string: "https://academico.uefs.br/PortalSagres/Acesso.aspx")!

    @ObservableState
    struct State: Equatable {
        var username = ""
        var password = ""
        var showPassword = false
        var isLoading = false
        var errorMessage: String?

        var canSubmit: Bool {
            !username.isEmpty && !password.isEmpty && !isLoading
        }
    }

    enum Action: BindableAction, Equatable {
        case task
        case binding(BindingAction<State>)
        case toggleShowPassword
        case forgotPasswordTapped
        case submitTapped
        case passkeyTapped
        case loginResponse(Result<Session, AuthError>)
        case passkeyResponse(Result<Session, AuthError>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            /// `username` is the typed SAGRES user; nil for passkey logins.
            case loggedIn(username: String?, session: Session)
        }
    }

    @Dependency(\.authRepository) var authRepository
    @Dependency(\.passkeyClient) var passkeyClient
    @Dependency(\.openURL) var openURL
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("LoginFeature")

    var body: some ReducerOf<Self> {
        BindingReducer()

        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(Screens.login)
                return .none

            case .binding:
                return .none

            case .toggleShowPassword:
                state.showPassword.toggle()
                return .none

            case .forgotPasswordTapped:
                return .run { _ in await openURL(Self.forgotPasswordURL) }

            case .submitTapped:
                guard state.canSubmit else { return .none }
                analytics.selectContent(contentType: ContentTypes.cta, itemId: "login_credentials")
                log.info("login submit for student=\(state.username)")
                state.isLoading = true
                state.errorMessage = nil
                return .run { [username = state.username, password = state.password] send in
                    await send(.loginResponse(Result {
                        try await authRepository.login(username: username, password: password)
                    }.mapToAuthError()))
                }

            case .passkeyTapped:
                guard !state.isLoading else { return .none }
                analytics.selectContent(contentType: ContentTypes.cta, itemId: "login_passkey")
                log.info("passkey login start")
                state.isLoading = true
                state.errorMessage = nil
                return .run { send in
                    await send(.passkeyResponse(Result {
                        let challenge = try await authRepository.beginPasskeyLogin()
                        let assertion = try await passkeyClient.assert(challenge)
                        return try await authRepository.completePasskeyLogin(
                            sessionId: challenge.sessionId,
                            assertion: assertion
                        )
                    }.mapToAuthError()))
                }

            case let .loginResponse(.success(session)):
                state.isLoading = false
                log.info("login ok for student=\(state.username)")
                return .send(.delegate(.loggedIn(username: state.username, session: session)))

            case let .passkeyResponse(.success(session)):
                state.isLoading = false
                log.info("passkey ok")
                return .send(.delegate(.loggedIn(username: nil, session: session)))

            case let .loginResponse(.failure(error)), let .passkeyResponse(.failure(error)):
                state.isLoading = false
                state.errorMessage = error.message
                log.warn("login/passkey failed username=\(state.username) err=\(String(describing: error))")
                return .none

            case .delegate:
                return .none
            }
        }
    }
}

extension Result where Success == Session, Failure == any Error {
    fileprivate func mapToAuthError() -> Result<Session, AuthError> {
        mapError { error in
            error as? AuthError ?? .server(nil)
        }
    }
}

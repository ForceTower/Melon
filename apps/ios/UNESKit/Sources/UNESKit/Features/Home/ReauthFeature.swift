import ComposableArchitecture
import Foundation

/// The "update your portal password" sheet. Distinct from the session-expired
/// sheet: the Melon session is fine here, it's the stored SAGRES password the
/// server syncs with that stopped working. The username is fixed — this can't
/// re-link a different account.
@Reducer
struct ReauthFeature {
    @ObservableState
    struct State: Equatable {
        /// From `api/me/status`; shown read-only.
        var username: String?
        var password = ""
        var showPassword = false
        var isLoading = false
        var errorMessage: String?
        /// Set when the portal answered "login failed" without a captcha token
        /// — retrying is only worth offering once one can be produced.
        var needsCaptcha = false

        var canSubmit: Bool { !password.isEmpty && !isLoading }
    }

    enum Action: BindableAction, Equatable {
        case task
        case binding(BindingAction<State>)
        case toggleShowPassword
        case submitTapped
        case closeTapped
        case reauthResponse(Result<Bool, ReauthFailure>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case succeeded
        }
    }

    @Dependency(\.credentialStatusRepository) var credentialStatusRepository
    @Dependency(\.credentialInvalidation) var credentialInvalidation
    @Dependency(\.analytics) var analytics
    @Dependency(\.dismiss) var dismiss

    private let log = Log.scoped("ReauthFeature")

    var body: some ReducerOf<Self> {
        BindingReducer()

        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(Screens.reauth)
                return .none

            case .binding:
                return .none

            case .toggleShowPassword:
                state.showPassword.toggle()
                return .none

            case .closeTapped:
                return .run { _ in await dismiss() }

            case .submitTapped:
                guard state.canSubmit else { return .none }
                analytics.selectContent(contentType: ContentTypes.cta, itemId: "reauth_submit")
                log.info("reauth submit")
                state.isLoading = true
                state.errorMessage = nil
                return .run { [password = state.password] send in
                    await send(.reauthResponse(Result {
                        try await credentialStatusRepository.reauthenticate(password, nil)
                        return true
                    }.mapToReauthFailure()))
                }

            case .reauthResponse(.success):
                state.isLoading = false
                log.info("reauth ok, sync resumes server-side")
                credentialInvalidation.clear()
                return .send(.delegate(.succeeded))

            case let .reauthResponse(.failure(failure)):
                state.isLoading = false
                state.needsCaptcha = failure == .captchaRequired
                state.errorMessage = failure.message
                log.warn("reauth failed err=\(String(describing: failure))")
                return .none

            case .delegate:
                return .none
            }
        }
    }
}

extension ReauthFailure {
    var message: String {
        switch self {
        case .invalidPassword, .captchaRequired: .localized(.reauthErrorInvalid)
        case .upstreamUnavailable: .localized(.reauthErrorUnavailable)
        case .network: .localized(.reauthErrorNetwork)
        case let .server(message): message ?? .localized(.reauthErrorUnavailable)
        }
    }
}

extension Result where Success == Bool, Failure == any Error {
    fileprivate func mapToReauthFailure() -> Result<Bool, ReauthFailure> {
        mapError { error in
            error as? ReauthFailure ?? .server(nil)
        }
    }
}

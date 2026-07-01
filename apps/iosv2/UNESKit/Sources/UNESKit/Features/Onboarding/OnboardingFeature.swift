import ComposableArchitecture

@Reducer
struct OnboardingFeature {
    @ObservableState
    struct State: Equatable {
        var splash = true
        var session: Session?
        var path = StackState<Path.State>()
    }

    @Reducer
    enum Path {
        case intro(IntroFeature)
        case login(LoginFeature)
        case sync(SyncFeature)
        case ready(ReadyFeature)
    }

    enum Action: Equatable {
        case task
        case splashFinished
        case exploreTapped
        case loginTapped
        case path(StackActionOf<Path>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case finished
        }
    }

    @Dependency(\.continuousClock) var clock

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                guard state.splash else { return .none }
                return .run { send in
                    try await clock.sleep(for: .seconds(2.6))
                    await send(.splashFinished, animation: .easeInOut(duration: 0.45))
                }

            case .splashFinished:
                state.splash = false
                return .none

            case .exploreTapped:
                state.path.append(.intro(IntroFeature.State()))
                return .none

            case .loginTapped:
                state.path.append(.login(LoginFeature.State()))
                return .none

            case .path(.element(id: _, action: .intro(.delegate(.login)))):
                state.path.append(.login(LoginFeature.State()))
                return .none

            case let .path(.element(id: _, action: .login(.delegate(.loggedIn(username, session))))):
                state.session = session
                state.path.append(.sync(SyncFeature.State(greeting: username ?? "estudante")))
                return .none

            case let .path(.element(id: _, action: .sync(.delegate(.done(profile, overview))))):
                let name = firstName(of: profile?.name ?? state.session?.user.name) ?? "estudante"
                state.path.append(.ready(ReadyFeature.State(userName: name, overview: overview ?? .empty)))
                return .none

            case .path(.element(id: _, action: .sync(.delegate(.authFailed)))):
                // Sync hit a 401 — back to login with an explanation.
                state.path.removeLast()
                if let id = state.path.ids.last, case .login(var login) = state.path[id: id] {
                    login.errorMessage = "Sua sessão expirou. Entre novamente."
                    state.path[id: id] = .login(login)
                }
                return .none

            case .path(.element(id: _, action: .ready(.delegate(.enter)))):
                return .send(.delegate(.finished))

            case .path, .delegate:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    private func firstName(of name: String?) -> String? {
        name?.split(separator: " ").first.map(String.init)
    }
}

extension OnboardingFeature.Path.State: Equatable {}
extension OnboardingFeature.Path.Action: Equatable {}

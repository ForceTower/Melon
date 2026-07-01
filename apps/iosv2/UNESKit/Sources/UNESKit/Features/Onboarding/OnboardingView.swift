import ComposableArchitecture
import SwiftUI

struct OnboardingView: View {
    @Bindable var store: StoreOf<OnboardingFeature>

    var body: some View {
        Group {
            if store.splash {
                // The stack mounts only after the splash so Welcome's staggered
                // entrance plays on screen, not hidden behind the overlay.
                SplashView()
                    .transition(.opacity)
            } else {
                navigationStack
                    .transition(.opacity)
            }
        }
        .task { await store.send(.task).finish() }
    }

    private var navigationStack: some View {
        NavigationStack(path: $store.scope(state: \.path, action: \.path)) {
            WelcomeView(
                onExplore: { store.send(.exploreTapped) },
                onLogin: { store.send(.loginTapped) }
            )
        } destination: { store in
            switch store.case {
            case let .intro(store):
                IntroView(store: store)
            case let .login(store):
                LoginView(store: store)
            case let .sync(store):
                SyncView(store: store)
            case let .ready(store):
                ReadyView(store: store)
            }
        }
        .tint(UNESColor.accent)
    }
}

#Preview {
    OnboardingView(
        store: Store(initialState: OnboardingFeature.State()) {
            OnboardingFeature()
        }
    )
}

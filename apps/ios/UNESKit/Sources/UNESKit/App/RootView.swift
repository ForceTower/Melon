import ComposableArchitecture
import SwiftUI

public struct RootView: View {
    @State private var store: StoreOf<RootFeature>
    /// Booting straight into the shell: `LaunchCover` carries the launch
    /// image over it while it mounts. Onboarding boots continue the launch
    /// image in `SplashView` instead, so they never need the cover.
    @State private var launchCover: Bool

    public init() {
        let initial = RootFeature.State.bootstrap()
        _store = State(initialValue: Store(initialState: initial) { RootFeature() })
        _launchCover = State(initialValue: initial.isConnected)
    }

    public var body: some View {
        ZStack {
            shell
                .animation(.easeInOut(duration: 0.4), value: phase)
            if launchCover {
                LaunchCover { launchCover = false }
            }
        }
        .task { await store.send(.task).finish() }
    }

    @ViewBuilder
    private var shell: some View {
        switch store.state {
        case .onboarding:
            if let store = store.scope(state: \.onboarding, action: \.onboarding) {
                OnboardingView(store: store)
                    .transition(.opacity)
            }
        case .connected:
            if let store = store.scope(state: \.connected, action: \.connected) {
                AppView(store: store)
                    .transition(.opacity)
            }
        case .farewell:
            if let store = store.scope(state: \.farewell, action: \.farewell) {
                FarewellView(store: store)
                    .transition(.opacity)
            }
        }
    }

    /// Which of the three shells is on screen — the crossfade trigger.
    private var phase: String {
        switch store.state {
        case .onboarding: "onboarding"
        case .connected: "connected"
        case .farewell: "farewell"
        }
    }
}

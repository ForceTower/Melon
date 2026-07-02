import ComposableArchitecture
import SwiftUI

public struct RootView: View {
    @State private var store = Store(initialState: RootFeature.State.bootstrap()) {
        RootFeature()
    }

    public init() {}

    public var body: some View {
        Group {
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
        .animation(.easeInOut(duration: 0.4), value: phase)
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

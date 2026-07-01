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
            }
        }
        .animation(.easeInOut(duration: 0.4), value: isOnboarding)
    }

    private var isOnboarding: Bool {
        if case .onboarding = store.state { return true }
        return false
    }
}

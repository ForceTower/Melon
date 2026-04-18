import SwiftUI

private enum AppRoute {
    case onboarding
    case connected
}

struct ContentView: View {
    @State private var route: AppRoute = .onboarding

    var body: some View {
        ZStack {
            switch route {
            case .onboarding:
                OnboardingFlow(onComplete: {
                    withAnimation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.5)) {
                        route = .connected
                    }
                })
                .transition(
                    .asymmetric(
                        insertion: .opacity,
                        removal: .opacity.combined(with: .move(edge: .leading))
                    )
                )
            case .connected:
                ConnectedView()
                    .transition(
                        .asymmetric(
                            insertion: .opacity.combined(with: .move(edge: .trailing)),
                            removal: .opacity
                        )
                    )
            }
        }
        .statusBarHidden(false)
    }
}

#Preview {
    ContentView()
}

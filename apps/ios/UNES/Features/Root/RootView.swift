import SwiftUI

private enum RootDestination {
    case splash
    case onboarding
    case connected
}

@Observable
final class RootViewModel {
    fileprivate var destination: RootDestination = .splash

    func splashFinished() {
        // TODO: inspect session/credentials and route to .connected when the
        // user is already signed in. Hardcoded to .onboarding for now.
        destination = .onboarding
    }

    func onboardingCompleted() {
        destination = .connected
    }
}

struct RootView: View {
    @State private var viewModel = RootViewModel()

    var body: some View {
        ZStack {
            switch viewModel.destination {
            case .splash:
                SplashView {
                    withAnimation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.5)) {
                        viewModel.splashFinished()
                    }
                }
                .transition(.opacity)
            case .onboarding:
                OnboardingFlow(onComplete: {
                    withAnimation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.5)) {
                        viewModel.onboardingCompleted()
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
    RootView()
}

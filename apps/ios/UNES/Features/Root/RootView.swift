import SwiftUI
@preconcurrency import Umbrella

private enum RootDestination {
    case splash
    case onboarding
    case connected
}

@Observable
final class RootViewModel {
    fileprivate var destination: RootDestination = .splash

    func splashFinished(hasSession: Bool) {
        destination = hasSession ? .connected : .onboarding
    }

    func onboardingCompleted() {
        destination = .connected
    }

    func userLoggedOut() {
        destination = .onboarding
    }
}

struct RootView: View {
    let sessionStore: SessionSessionStore
    let onboarding: OnboardingFactory
    let overview: OverviewFactory
    let scheduleFocused: ScheduleFocusedFactory
    let disciplines: DisciplinesFactory
    let messages: MessagesFactory
    let me: MeFactory

    @State private var viewModel = RootViewModel()

    var body: some View {
        ZStack {
            switch viewModel.destination {
            case .splash:
                SplashView(sessionStore: sessionStore) { hasSession in
                    withAnimation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.5)) {
                        viewModel.splashFinished(hasSession: hasSession)
                    }
                }
                .transition(.opacity)
            case .onboarding:
                OnboardingFlow(
                    factory: onboarding,
                    onComplete: {
                        withAnimation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.5)) {
                            viewModel.onboardingCompleted()
                        }
                    }
                )
                .transition(
                    .asymmetric(
                        insertion: .opacity,
                        removal: .opacity.combined(with: .move(edge: .leading))
                    )
                )
            case .connected:
                ConnectedView(
                    overview: overview,
                    scheduleFocused: scheduleFocused,
                    disciplines: disciplines,
                    messages: messages,
                    me: me,
                    onLoggedOut: {
                        withAnimation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.5)) {
                            viewModel.userLoggedOut()
                        }
                    }
                )
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

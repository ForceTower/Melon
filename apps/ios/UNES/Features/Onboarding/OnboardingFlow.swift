import SwiftUI

enum OnboardingStep: Int, CaseIterable {
    case splash, welcome, intro, login, sync, ready
}

@Observable
final class OnboardingState {
    var step: OnboardingStep = .splash
    var studentId: String = ""
    var userName: String = "Mariana"

    func go(to next: OnboardingStep) {
        step = next
    }
}

struct OnboardingFlow: View {
    var onComplete: () -> Void = {}

    @State private var state = OnboardingState()

    var body: some View {
        ZStack {
            switch state.step {
            case .splash:
                SplashView { state.go(to: .welcome) }
                    .transition(screenTransition)
            case .welcome:
                WelcomeView(
                    onPrimary: { state.go(to: .intro) },
                    onSecondary: { state.go(to: .login) }
                )
                .transition(screenTransition)
            case .intro:
                IntroCarouselView(
                    onDone: { state.go(to: .login) },
                    onBack: { state.go(to: .welcome) }
                )
                .transition(screenTransition)
            case .login:
                LoginView(
                    onSubmit: { id in
                        state.studentId = id
                        state.go(to: .sync)
                    },
                    onBack: { state.go(to: .intro) }
                )
                .transition(screenTransition)
            case .sync:
                SyncView(
                    userId: state.studentId.isEmpty ? "estudante" : state.studentId,
                    onDone: { state.go(to: .ready) }
                )
                .transition(screenTransition)
            case .ready:
                ReadyView(
                    userName: state.userName,
                    onEnter: onComplete
                )
                .transition(screenTransition)
            }
        }
        .animation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.4), value: state.step)
    }

    private var screenTransition: AnyTransition {
        .asymmetric(
            insertion: .opacity.combined(with: .move(edge: .trailing)),
            removal:   .opacity.combined(with: .move(edge: .leading))
        )
    }
}

#Preview {
    OnboardingFlow()
}

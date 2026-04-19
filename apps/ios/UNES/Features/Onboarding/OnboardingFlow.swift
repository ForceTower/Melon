import SwiftUI
import Umbrella

private enum OnboardingRoute: Hashable {
    case intro
    case login
    case sync(studentId: String)
    case ready(userName: String)
}

struct OnboardingFlow: View {
    var onComplete: () -> Void = {}

    @Environment(\.umbrella) private var umbrella
    @State private var path: [OnboardingRoute] = []

    var body: some View {
        NavigationStack(path: $path) {
            WelcomeView(
                onPrimary: { path.append(.intro) },
                onSecondary: { path.append(.login) }
            )
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: OnboardingRoute.self, destination: destination)
        }
    }

    @ViewBuilder
    private func destination(for route: OnboardingRoute) -> some View {
        switch route {
        case .intro:
            IntroCarouselView(
                onDone: { path.append(.login) },
                onBack: pop
            )
            .toolbar(.hidden, for: .navigationBar)
        case .login:
            LoginView(
                loginUseCase: umbrella?.loginUseCase,
                onSubmit: { id in path.append(.sync(studentId: id)) }
            )
        case .sync(let studentId):
            SyncView(
                userId: studentId.isEmpty ? "estudante" : studentId,
                onDone: { path.append(.ready(userName: "Mariana")) }
            )
            .toolbar(.hidden, for: .navigationBar)
            .navigationBarBackButtonHidden()
        case .ready(let userName):
            ReadyView(userName: userName, onEnter: onComplete)
                .toolbar(.hidden, for: .navigationBar)
                .navigationBarBackButtonHidden()
        }
    }

    private func pop() {
        guard !path.isEmpty else { return }
        path.removeLast()
    }
}

#Preview {
    OnboardingFlow()
}

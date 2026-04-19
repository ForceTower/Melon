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
                onDone: { path.append(.login) }
            )
        case .login:
            LoginView(
                loginUseCase: umbrella?.loginUseCase,
                onSubmit: { id in path.append(.sync(studentId: id)) }
            )
        case .sync(let name):
            let firstName = String(name.split(separator: " ").first ?? "")
            syncDestination(name: name, displayName: firstName.isEmpty ? "estudante" : firstName)
        case .ready(let userName):
            let firstName = String(userName.split(separator: " ").first ?? "")
            ReadyView(userName: firstName, onEnter: onComplete)
                .toolbar(.hidden, for: .navigationBar)
                .navigationBarBackButtonHidden()
        }
    }

    @ViewBuilder
    private func syncDestination(name: String, displayName: String) -> some View {
        if let umbrella {
            let viewModel = SyncViewModel(
                graph: umbrella,
                onDone: { path.append(.ready(userName: name)) },
                // Auth broke mid-onboarding (rare). Bounce back to login by
                // resetting the stack — the user re-authenticates from scratch.
                onAuthFailed: { path.removeAll() }
            )
            SyncView(name: displayName, viewModel: viewModel)
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

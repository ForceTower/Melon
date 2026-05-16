import SwiftUI

private enum OnboardingRoute: Hashable {
    case intro
    case login
    case sync(studentId: String)
    case ready(userName: String)
}

struct OnboardingFlow: View {
    let factory: OnboardingFactory
    var onComplete: () -> Void = {}

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
            factory.makeLogin { id in path.append(.sync(studentId: id)) }
        case .sync(let name):
            let firstName = String(name.split(separator: " ").first ?? "")
            factory.makeSync(
                displayName: firstName.isEmpty ? "estudante" : firstName,
                onDone: { path.append(.ready(userName: name)) },
                // Auth broke mid-onboarding (rare). Bounce back to login by
                // resetting the stack — the user re-authenticates from scratch.
                onAuthFailed: { path.removeAll() }
            )
            .toolbar(.hidden, for: .navigationBar)
            .navigationBarBackButtonHidden()
        case .ready(let userName):
            let firstName = String(userName.split(separator: " ").first ?? "")
            factory.makeReady(userName: firstName, onEnter: onComplete)
                .toolbar(.hidden, for: .navigationBar)
                .navigationBarBackButtonHidden()
        }
    }
}

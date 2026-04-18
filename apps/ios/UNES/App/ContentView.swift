import SwiftUI

struct ContentView: View {
    var body: some View {
        OnboardingFlow()
            .preferredColorScheme(.light)
            .statusBarHidden(false)
    }
}

#Preview {
    ContentView()
}

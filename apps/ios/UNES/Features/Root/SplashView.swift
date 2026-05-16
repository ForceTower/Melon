import SwiftUI
import Umbrella

struct SplashView: View {
    let onDone: (Bool) -> Void

    @State private var viewModel: SplashViewModel
    @State private var wordmarkShown = false
    @State private var dotShown = false
    @State private var captionShown = false
    @State private var creditShown = false

    init(sessionStore: SessionSessionStore?, onDone: @escaping (Bool) -> Void) {
        _viewModel = State(initialValue: SplashViewModel(sessionStore: sessionStore))
        self.onDone = onDone
    }

    var body: some View {
        ZStack {
            UNESColor.darkBg.ignoresSafeArea()
            MeshGradientView(variant: .warm, intensity: 1).ignoresSafeArea()

            VStack(spacing: 18) {
                Spacer()

                HStack(alignment: .firstTextBaseline, spacing: 2) {
                    Text("unes")
                        .font(UNESFont.serif(88))
                        .tracking(-3.5)
                        .foregroundStyle(UNESColor.surfaceLight)
                    Circle()
                        .fill(UNESColor.amber)
                        .frame(width: 10, height: 10)
                        .scaleEffect(dotShown ? 1 : 0)
                        .opacity(dotShown ? 1 : 0)
                        .offset(y: -6)
                }
                .opacity(wordmarkShown ? 1 : 0)
                .offset(y: wordmarkShown ? 0 : 12)

                Text("Universidade · Notas · Semestre")
                    .font(UNESFont.mono(11))
                    .tracking(2)
                    .foregroundStyle(Color.white.opacity(0.55))
                    .textCase(.uppercase)
                    .opacity(captionShown ? 1 : 0)

                Spacer()
            }

            VStack {
                Spacer()
                HStack(spacing: 6) {
                    Text("para")
                        .foregroundStyle(Color.white.opacity(0.45))
                    Text("UEFS")
                        .fontWeight(.medium)
                        .foregroundStyle(Color.white.opacity(0.8))
                    Text("· Feira de Santana")
                        .foregroundStyle(Color.white.opacity(0.45))
                }
                .font(UNESFont.sans(13))
                .opacity(creditShown ? 1 : 0)
                .padding(.bottom, 16)
            }
        }
        .onAppear {
            withAnimation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 1.1)) { wordmarkShown = true }
            withAnimation(.spring(response: 0.5, dampingFraction: 0.55).delay(0.8)) { dotShown = true }
            withAnimation(.easeOut(duration: 0.8).delay(0.6)) { captionShown = true }
            withAnimation(.easeOut(duration: 0.6).delay(1.2)) { creditShown = true }

            Task {
                async let dwell: Void = Task.sleep(for: .seconds(2))
                let hasSession = await viewModel.hasStoredSession()
                _ = try? await dwell
                onDone(hasSession)
            }
        }
    }
}

#Preview {
    SplashView(sessionStore: nil, onDone: { _ in })
}

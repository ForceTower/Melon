import SwiftUI

struct SplashView: View {
    let onDone: () -> Void

    @State private var wordmarkShown = false
    @State private var dotShown = false
    @State private var captionShown = false
    @State private var creditShown = false

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

            DispatchQueue.main.asyncAfter(deadline: .now() + 2.6) { onDone() }
        }
    }
}

#Preview {
    SplashView(onDone: {})
}

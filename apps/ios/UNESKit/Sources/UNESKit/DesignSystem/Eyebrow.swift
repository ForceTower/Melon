import SwiftUI

/// Uppercase accent label above titles, optionally led by the pulsing live dot.
struct Eyebrow: View {
    var text: String
    var color: Color = UNESColor.accent
    var live = false

    var body: some View {
        HStack(spacing: 7) {
            if live { LiveDot() }
            Text(text)
                .textCase(.uppercase)
                .font(.system(size: 13, weight: .bold))
                .tracking(0.78)
        }
        .foregroundStyle(color)
    }
}

/// Green dot pulsing an expanding, fading ring.
struct LiveDot: View {
    var size: CGFloat = 7
    var color: Color = UNESColor.liveGreen

    @State private var pulsing = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Circle()
            .fill(color)
            .frame(width: size, height: size)
            .background {
                Circle()
                    .fill(color.opacity(pulsing ? 0 : 0.35))
                    .scaleEffect(pulsing ? 2.4 : 1)
            }
            .onAppear {
                guard !reduceMotion else { return }
                withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
                    pulsing = true
                }
            }
    }
}

#Preview {
    VStack(alignment: .leading, spacing: 16) {
        Eyebrow(text: "UEFS · SAGRES")
        Eyebrow(text: "Preparando seu semestre", live: true)
        Eyebrow(text: "Conectado", color: UNESColor.successGreen, live: true)
    }
    .padding(28)
}

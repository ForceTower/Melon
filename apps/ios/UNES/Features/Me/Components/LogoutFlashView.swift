import SwiftUI

/// Transient overlay played between the logout confirmation and the
/// logged-out screen. Mirrors `LogoutFlash` in `screens-me.jsx` — a ~0.9s
/// fade-in / hold / fade-out of a destructive icon tile with a subtle
/// scale-rotate intro, under a monospaced "encerrando sessão…" caption.
struct LogoutFlashView: View {
    @State private var envelopeOpacity: Double = 0
    @State private var iconScale: CGFloat = 0.85
    @State private var iconRotation: Double = -6
    @State private var iconOpacity: Double = 0

    var body: some View {
        ZStack {
            UNESColor.surface
                .ignoresSafeArea()

            VStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(MeColors.signOut.opacity(0.12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .strokeBorder(MeColors.signOut.opacity(0.3), lineWidth: 1)
                        )
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(UNESColor.accent)
                }
                .frame(width: 44, height: 44)
                .scaleEffect(iconScale)
                .rotationEffect(.degrees(iconRotation))
                .opacity(iconOpacity)

                Text("encerrando sessão…")
                    .font(UNESFont.mono(10))
                    .tracking(1.8)
                    .textCase(.uppercase)
                    .foregroundStyle(UNESColor.ink3)
            }
        }
        .opacity(envelopeOpacity)
        .onAppear(perform: run)
    }

    private func run() {
        // Envelope: fade in fast, fade out slow — mirrors the JSX keyframe
        // 0 → 1 (0–15%) → 1 (–85%) → 0 (–100%) across a 0.9s window.
        withAnimation(.easeOut(duration: 0.15)) {
            envelopeOpacity = 1
        }
        withAnimation(.easeIn(duration: 0.15).delay(0.75)) {
            envelopeOpacity = 0
        }

        // Icon: scale 0.85 / rot -6° → 1.08 / 0° (pop) → 1.0 / 0° → 0.92 / 0°.
        withAnimation(.timingCurve(0.4, 0, 0.2, 1, duration: 0.27)) {
            iconScale = 1.08
            iconRotation = 0
            iconOpacity = 1
        }
        withAnimation(.easeOut(duration: 0.36).delay(0.27)) {
            iconScale = 1.0
        }
        withAnimation(.easeIn(duration: 0.27).delay(0.63)) {
            iconScale = 0.92
            iconOpacity = 0.4
        }
    }
}

#Preview {
    LogoutFlashView()
}

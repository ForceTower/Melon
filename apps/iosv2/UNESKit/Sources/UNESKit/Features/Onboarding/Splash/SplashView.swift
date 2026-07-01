import SwiftUI

struct SplashView: View {
    var body: some View {
        ZStack {
            UNESColor.darkBg
            MeshView(variant: .warm)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.2), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.55), location: 1),
                ],
                angle: 160
            )

            VStack(spacing: 20) {
                appIcon
                    .popIn(duration: 0.8, from: 0.7, offsetY: 8, overshoot: 1.3)
                wordmark
                    .fadeUp(delay: 0.25, duration: 0.7)
            }

            VStack {
                Spacer()
                footer
                    .fadeUp(delay: 0.9, duration: 0.6)
                    .padding(.bottom, 46)
            }
        }
        .ignoresSafeArea()
    }

    private var appIcon: some View {
        ZStack {
            MeshView(variant: .warm)
            Text("u")
                .font(.system(size: 40, weight: .heavy))
                .foregroundStyle(.white)
                .shadow(color: .black.opacity(0.25), radius: 4, y: 2)
        }
        .frame(width: 76, height: 76)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(.white.opacity(0.18), lineWidth: 0.5)
        }
        .shadow(color: .black.opacity(0.4), radius: 17, y: 12)
    }

    private var wordmark: some View {
        VStack(spacing: 14) {
            HStack(alignment: .bottom, spacing: 3) {
                Text("unes")
                    .font(.system(size: 46, weight: .heavy))
                    .tracking(-2.3)
                    .foregroundStyle(UNESColor.paper)
                Circle()
                    .fill(UNESColor.accent)
                    .frame(width: 9, height: 9)
                    .padding(.bottom, 6)
                    .popIn(delay: 0.7, duration: 0.5, from: 0, overshoot: 1.6)
            }
            Text("Universidade · Notas · Semestre")
                .font(.system(size: 12.5, weight: .semibold))
                .tracking(0.25)
                .foregroundStyle(UNESColor.paper.opacity(0.6))
        }
    }

    private var footer: some View {
        VStack(spacing: 16) {
            SpinnerRing(
                size: 26,
                color: UNESColor.paper.opacity(0.75),
                trackColor: UNESColor.paper.opacity(0.2)
            )
            (
                Text("para ").foregroundStyle(UNESColor.paper.opacity(0.5))
                    + Text("UEFS").fontWeight(.semibold).foregroundStyle(UNESColor.paper.opacity(0.82))
                    + Text(" · Feira de Santana").foregroundStyle(UNESColor.paper.opacity(0.5))
            )
            .font(.system(size: 13, weight: .medium))
        }
    }
}

#Preview {
    SplashView()
}

import SwiftUI

struct WelcomeView: View {
    var onExplore: () -> Void
    var onLogin: () -> Void

    var body: some View {
        ZStack {
            backdrop
                .ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                Eyebrow(text: "Boas vindas ao UNES", color: .white.opacity(0.92))
                    .fadeUp(delay: 0.1, duration: 0.6)

                Spacer()

                title
                    .fadeUp(delay: 0.22, duration: 0.75)

                Text("Horários, notas, recados da coordenação e turmas da UEFS. Tudo conectado à sua matrícula.")
                    .font(.system(size: 17))
                    .tracking(-0.17)
                    .lineSpacing(3.5)
                    .foregroundStyle(UNESColor.paper.opacity(0.78))
                    .frame(maxWidth: 320, alignment: .leading)
                    .padding(.top, 20)
                    .fadeUp(delay: 0.36, duration: 0.75)

                VStack(spacing: 10) {
                    Button(action: onExplore) {
                        UNESButtonLabel(text: "Conhecer o app")
                    }
                    .buttonStyle(.unesLight)

                    Button(action: onLogin) {
                        Text("Já tenho matrícula").tracking(-0.17)
                    }
                    .buttonStyle(.unesGlass)
                }
                .padding(.top, 22)
                .fadeUp(delay: 0.5, duration: 0.75)
            }
            .padding(.horizontal, 28)
            .padding(.top, 44)
            .padding(.bottom, 12)
        }
        .hiddenNavigationBar()
    }

    private var backdrop: some View {
        ZStack {
            UNESColor.darkBg
            MeshView(variant: .warm)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.35), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.15), location: 0.42),
                    .init(color: UNESColor.scrim.opacity(0.72), location: 1),
                ],
                angle: 180
            )
        }
    }

    private var title: some View {
        VStack(alignment: .leading, spacing: -9) {
            titleLine("Seu semestre,", color: UNESColor.paper)
            titleLine("num só", color: UNESColor.accent)
            titleLine("lugar.", color: UNESColor.paper)
        }
    }

    private func titleLine(_ text: String, color: Color) -> some View {
        Text(text)
            .font(.system(size: 52, weight: .heavy))
            .tracking(-2.34)
            .foregroundStyle(color)
    }
}

#Preview {
    NavigationStack {
        WelcomeView(onExplore: {}, onLogin: {})
    }
}

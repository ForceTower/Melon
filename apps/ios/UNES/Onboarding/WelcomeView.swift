import SwiftUI

struct WelcomeView: View {
    let onPrimary: () -> Void
    let onSecondary: () -> Void

    var body: some View {
        ZStack {
            UNESColor.darkBg.ignoresSafeArea()
            MeshGradientView(variant: .warm).ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 43)

                Text("◦ Boas vindas ao UNES")
                    .font(UNESFont.mono(11))
                    .tracking(2.2)
                    .textCase(.uppercase)
                    .foregroundStyle(Color.white.opacity(0.6))
                    .fadeUpOnAppear(delay: 0.1)

                Spacer()

                VStack(alignment: .leading, spacing: 0) {
                    Text("Seu semestre,")
                        .foregroundStyle(UNESColor.surface)
                    Text("num só")
                        .italic()
                        .foregroundStyle(UNESColor.amber)
                    Text("lugar.")
                        .foregroundStyle(UNESColor.surface)
                }
                .font(UNESFont.serif(54))
                .tracking(-1.3)
                .lineSpacing(-3)
                .fadeUpOnAppear(delay: 0.25)

                Text("Horários, notas, recados da coordenação e turmas da UEFS — tudo conectado à sua matrícula.")
                    .font(UNESFont.sans(17))
                    .tracking(-0.17)
                    .lineSpacing(3)
                    .foregroundStyle(Color.white.opacity(0.72))
                    .frame(maxWidth: 320, alignment: .leading)
                    .padding(.top, 22)
                    .fadeUpOnAppear(delay: 0.45)

                Spacer().frame(height: 30)

                VStack(spacing: 10) {
                    PrimaryButton(
                        title: "Conhecer o app",
                        background: UNESColor.surface,
                        foreground: UNESColor.ink,
                        action: onPrimary
                    )
                    GlassButton(title: "Já tenho matrícula", action: onSecondary)
                }
                .fadeUpOnAppear(delay: 0.65)
            }
            .padding(.horizontal, 28)
            .padding(.bottom, 16)
        }
    }
}

#Preview {
    WelcomeView(onPrimary: {}, onSecondary: {})
}

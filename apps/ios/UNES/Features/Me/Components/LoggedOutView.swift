import SwiftUI

/// Post-logout "goodbye" screen. Mirrors `LoggedOutView` in
/// `screens-me.jsx`: a rose-mesh halo bleeding over the surface, a serif
/// farewell with the user's first name in italic accent, a local-data
/// reassurance card, and a single CTA that sends the user back to
/// `WelcomeView` via `onSignIn`.
struct LoggedOutView: View {
    let firstName: String
    let onSignIn: () -> Void

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface
                .ignoresSafeArea()

            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .rose, intensity: 0.38)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: UNESColor.surface, location: 1.0),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 360)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            content
        }
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 0) {
            Spacer(minLength: 0)

            Text("◦ sessão encerrada")
                .font(UNESFont.sans(12, weight: .medium))
                .tracking(1.44)
                .textCase(.uppercase)
                .foregroundStyle(UNESColor.ink3)
                .padding(.bottom, 10)
                .fadeUpOnAppear(delay: 0.05, distance: 12, duration: 0.55)

            (
                Text("Até logo,\n")
                    .foregroundStyle(UNESColor.ink)
                + Text(firstNameStylized)
                    .font(UNESFont.serif(40, italic: true))
                    .foregroundStyle(UNESColor.accent)
                + Text(".")
                    .foregroundStyle(UNESColor.ink)
            )
            .font(UNESFont.serif(40))
            .tracking(-0.8)
            .lineSpacing(-6)
            .padding(.bottom, 14)
            .fadeUpOnAppear(delay: 0.12, distance: 12, duration: 0.6)

            Text("Seus lembretes e preferências ficaram guardados aqui no aparelho. Quando quiser voltar, a sua matrícula te espera.")
                .font(UNESFont.sans(14))
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink2)
                .frame(maxWidth: 280, alignment: .leading)
                .padding(.bottom, 24)
                .fadeUpOnAppear(delay: 0.22, distance: 12, duration: 0.55)

            localDataCard
                .padding(.bottom, 20)
                .fadeUpOnAppear(delay: 0.3, distance: 12, duration: 0.55)

            PrimaryButton(
                title: "Entrar novamente",
                background: UNESColor.ink,
                foreground: UNESColor.surface,
                action: onSignIn
            )
            .fadeUpOnAppear(delay: 0.4, distance: 12, duration: 0.55)

            Text("◦ UNES V\(Bundle.main.appVersion) ◦")
                .font(UNESFont.mono(9))
                .tracking(1.26)
                .textCase(.uppercase)
                .foregroundStyle(UNESColor.ink4)
                .frame(maxWidth: .infinity)
                .padding(.top, 14)
                .fadeUpOnAppear(delay: 0.5, distance: 8, duration: 0.55)

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 28)
    }

    private var firstNameStylized: String {
        firstName
    }

    private var localDataCard: some View {
        HStack(spacing: 11) {
            Circle()
                .fill(MeColors.okGreen)
                .frame(width: 8, height: 8)
                .overlay(
                    Circle()
                        .strokeBorder(MeColors.okGreen.opacity(0.2), lineWidth: 3)
                        .scaleEffect(1.75)
                )

            VStack(alignment: .leading, spacing: 2) {
                Text("Dados locais preservados")
                    .font(UNESFont.sans(12, weight: .medium))
                    .foregroundStyle(UNESColor.ink)
                Text("lembretes, temas e atalhos ficam salvos")
                    .font(UNESFont.mono(9))
                    .tracking(0.54)
                    .foregroundStyle(UNESColor.ink4)
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(UNESColor.card)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(UNESColor.cardLine, lineWidth: 1)
        )
    }
}

#Preview {
    LoggedOutView(firstName: "Mariana", onSignIn: {})
}

import SwiftUI

/// The dark-mesh credential vault: identity up top, the SAGRES login pair
/// below, password masked behind a Face ID reveal that expires on its own.
struct SettingsCredentialsHero: View {
    var name: String
    var email: String?
    var credentials: AccountCredentials?
    var isRevealed: Bool
    var copied: SettingsFeature.CredentialField?
    var onReveal: () -> Void
    var onCopy: (SettingsFeature.CredentialField) -> Void

    var body: some View {
        ZStack {
            UNESColor.darkBg
            MeshView(variant: .cool)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.15), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.66), location: 1),
                ],
                angle: 155
            )

            VStack(spacing: 16) {
                statusRow
                identityRow
                credentialsBox
            }
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 18, trailing: 20))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.28), radius: 20, y: 18)
    }

    private var statusRow: some View {
        HStack {
            HStack(spacing: 7) {
                LiveDot()
                Text("Credenciais")
                    .textCase(.uppercase)
                    .font(.system(size: 12, weight: .semibold))
                    .tracking(0.2)
            }
            .foregroundStyle(.white.opacity(0.9))

            Spacer()

            Text(isRevealed ? "visível · oculta em 30s" : "criptografada · Face ID")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(.white.opacity(0.55))
        }
    }

    private var identityRow: some View {
        HStack(spacing: 14) {
            avatar
            VStack(alignment: .leading, spacing: 3) {
                Text(name)
                    .font(.system(size: 20, weight: .bold))
                    .tracking(-0.6)
                    .foregroundStyle(.white)
                    .lineLimit(1)
                if let email {
                    Text(email)
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundStyle(.white.opacity(0.6))
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            revealButton
        }
    }

    private var avatar: some View {
        Text(initial)
            .font(.system(size: 24, weight: .bold))
            .tracking(-0.72)
            .foregroundStyle(.white)
            .frame(width: 52, height: 52)
            .background(
                LinearGradient.css(
                    stops: [
                        .init(color: UNESColor.amber, location: 0),
                        .init(color: UNESColor.coral, location: 0.55),
                        .init(color: UNESColor.magenta, location: 1),
                    ],
                    angle: 135
                ),
                in: Circle()
            )
            .shadow(color: UNESColor.coral.opacity(0.4), radius: 11, y: 8)
    }

    private var initial: String {
        name.first.map { String($0).uppercased() } ?? "•"
    }

    private var revealButton: some View {
        Button(action: onReveal) {
            HStack(spacing: 6) {
                Image(systemName: isRevealed ? "eye.slash" : "eye")
                    .font(.system(size: 12, weight: .semibold))
                Text(isRevealed ? "Ocultar" : "Ver")
                    .font(.system(size: 12.5, weight: .semibold))
                    .tracking(-0.13)
            }
            .foregroundStyle(isRevealed ? UNESColor.darkBg : .white)
            .padding(EdgeInsets(top: 9, leading: 13, bottom: 9, trailing: 13))
            .background(
                isRevealed ? Color.white : .white.opacity(0.16),
                in: RoundedRectangle(cornerRadius: 14, style: .continuous)
            )
        }
        .buttonStyle(TilePressStyle())
        .disabled(credentials == nil)
        .opacity(credentials == nil ? 0.4 : 1)
    }

    private var credentialsBox: some View {
        VStack(spacing: 0) {
            field(
                label: "Usuário",
                value: credentials?.username ?? "—",
                field: .username,
                canCopy: credentials != nil
            )
            Rectangle()
                .fill(.white.opacity(0.1))
                .frame(height: 1)
            field(
                label: "Senha",
                value: passwordDisplay,
                field: .password,
                canCopy: credentials != nil && isRevealed
            )
        }
        .background(.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(.white.opacity(0.12))
        }
    }

    private var passwordDisplay: String {
        guard let password = credentials?.password else { return "—" }
        return isRevealed ? password : String(repeating: "•", count: password.count)
    }

    private func field(
        label: String,
        value: String,
        field: SettingsFeature.CredentialField,
        canCopy: Bool
    ) -> some View {
        HStack(spacing: 12) {
            Text(label)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(.white.opacity(0.55))
                .frame(width: 68, alignment: .leading)

            Text(value)
                .font(.system(size: 14, weight: .medium))
                .tracking(0.14)
                .monospacedDigit()
                .foregroundStyle(.white)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)

            Button {
                onCopy(field)
            } label: {
                HStack(spacing: 4) {
                    if copied == field {
                        Image(systemName: "checkmark")
                            .font(.system(size: 11, weight: .semibold))
                        Text("copiado")
                            .font(.system(size: 11.5, weight: .semibold))
                    } else {
                        Image(systemName: "doc.on.doc")
                            .font(.system(size: 12.5, weight: .medium))
                    }
                }
                .foregroundStyle(.white)
                .opacity(canCopy ? 0.85 : 0.3)
            }
            .buttonStyle(.plain)
            .disabled(!canCopy)
        }
        .padding(EdgeInsets(top: 11, leading: 14, bottom: 11, trailing: 14))
    }
}

#Preview {
    VStack(spacing: 20) {
        SettingsCredentialsHero(
            name: "Mariana Nogueira",
            email: "mariana.n@uefs.br",
            credentials: .preview,
            isRevealed: false,
            copied: nil,
            onReveal: {},
            onCopy: { _ in }
        )
        SettingsCredentialsHero(
            name: "Mariana Nogueira",
            email: "mariana.n@uefs.br",
            credentials: .preview,
            isRevealed: true,
            copied: .username,
            onReveal: {},
            onCopy: { _ in }
        )
    }
    .padding(16)
    .frame(maxHeight: .infinity)
    .background(UNESColor.surface)
}

import SwiftUI

/// Body of the "Sair da conta?" bottom sheet. Hosted by `MeView` through a
/// native `.sheet(...)` presentation — the scrim, blur, drag indicator, and
/// dismissal gesture are all provided by SwiftUI. Mirrors the `LogoutSheet`
/// component in `screens-me.jsx`.
struct LogoutConfirmationSheet: View {
    let identity: ProfileIdentity
    let onCancel: () -> Void
    let onConfirm: (_ keepData: Bool) -> Void
    /// Parent-owned measured height so the sheet's detent can track content
    /// instead of reserving a fixed block. Without this the detent renders
    /// taller than the VStack and the buttons sit above an empty void.
    var measuredHeight: Binding<CGFloat>? = nil

    @State private var keepData: Bool = true

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(.bottom, 14)

            accountCard
                .padding(.bottom, 12)

            description
                .padding(.bottom, 14)

            keepDataToggle
                .padding(.bottom, 14)

            footer
        }
        .padding(.horizontal, 20)
        .padding(.top, 30)
        .padding(.bottom, 22)
        .background(
            GeometryReader { proxy in
                Color.clear
                    .preference(key: SheetHeightKey.self, value: proxy.size.height)
            }
        )
        .onPreferenceChange(SheetHeightKey.self) { height in
            measuredHeight?.wrappedValue = height
        }
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 13, style: .continuous)
                    .fill(MeColors.signOut.opacity(0.12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 13, style: .continuous)
                            .strokeBorder(MeColors.signOut.opacity(0.25), lineWidth: 1)
                    )
                Image(systemName: "rectangle.portrait.and.arrow.right")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(UNESColor.accent)
            }
            .frame(width: 42, height: 42)

            VStack(alignment: .leading, spacing: 3) {
                Text("Sair da conta?")
                    .font(UNESFont.serif(22))
                    .tracking(-0.33)
                    .foregroundStyle(UNESColor.ink)

                Text("id · \(identity.enrollment)")
                    .font(UNESFont.mono(10))
                    .tracking(0.8)
                    .textCase(.uppercase)
                    .foregroundStyle(UNESColor.ink3)
            }

            Spacer(minLength: 0)
        }
    }

    private var accountCard: some View {
        HStack(spacing: 12) {
            avatar

            VStack(alignment: .leading, spacing: 2) {
                Text(identity.name)
                    .font(UNESFont.sans(13, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                    .truncationMode(.tail)

                Text("usuario · \(identity.username)")
                    .font(UNESFont.mono(9.5))
                    .tracking(0.57)
                    .foregroundStyle(UNESColor.ink4)
                    .lineLimit(1)
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 13)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(UNESColor.card)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(UNESColor.cardLine, lineWidth: 1)
        )
    }

    private var avatar: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        stops: [
                            .init(color: UNESColor.amber,   location: 0.0),
                            .init(color: UNESColor.coral,   location: 0.55),
                            .init(color: UNESColor.magenta, location: 1.0),
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            Text(identity.avatarInitial)
                .font(UNESFont.serif(18))
                .foregroundStyle(UNESColor.surfaceLight)
        }
        .frame(width: 36, height: 36)
    }

    private var description: some View {
        Text("Você precisará entrar com o seu nome de usuario novamente para ver horários, notas e mensagens da coordenação.")
            .font(UNESFont.sans(12.5))
            .lineSpacing(3)
            .foregroundStyle(UNESColor.ink2)
            .padding(.horizontal, 2)
    }

    private var keepDataToggle: some View {
        Toggle(isOn: $keepData) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Manter dados no dispositivo")
                    .font(UNESFont.sans(12.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink)
                Text("lembretes, temas e atalhos ficam salvos")
                    .font(UNESFont.mono(9))
                    .tracking(0.54)
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .tint(MeColors.okGreen)
        .padding(.horizontal, 13)
        .padding(.vertical, 11)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(UNESColor.surface2)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(UNESColor.line, lineWidth: 1)
        )
    }

    private var footer: some View {
        HStack(spacing: 8) {
            Button(action: onCancel) {
                Text("Cancelar")
                    .font(UNESFont.sans(13.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(UNESColor.surface2)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .strokeBorder(UNESColor.line, lineWidth: 1)
                    )
            }
            .buttonStyle(PressScaleStyle())

            Button {
                onConfirm(keepData)
            } label: {
                HStack(spacing: 7) {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                        .font(.system(size: 12, weight: .semibold))
                    Text("Sair agora")
                        .font(UNESFont.sans(13.5, weight: .semibold))
                }
                .foregroundStyle(UNESColor.surfaceLight)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(MeColors.signOut)
                )
                .shadow(color: MeColors.signOut.opacity(0.35), radius: 12, x: 0, y: 10)
            }
            .buttonStyle(PressScaleStyle())
            .layoutPriority(1)
        }
    }
}

private struct SheetHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        LogoutConfirmationSheet(
            identity: MeFixtures.identity,
            onCancel: {},
            onConfirm: { _ in }
        )
    }
}

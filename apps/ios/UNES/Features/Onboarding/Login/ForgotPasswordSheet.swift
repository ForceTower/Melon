import SwiftUI

/// Body of the "Esqueci minha senha" bottom sheet shown from `LoginView`.
/// UNES never holds the user's password — recovery is done through the
/// official UEFS portal — so this sheet explains the situation and bounces
/// the user out to the academic portal.
struct ForgotPasswordSheet: View {
    let onOpenPortal: () -> Void
    /// Parent-owned measured height so the sheet's detent can track content.
    /// Same pattern as `LogoutConfirmationSheet` / `AboutSheet`.
    var measuredHeight: Binding<CGFloat>? = nil

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(.bottom, 18)

            description
                .padding(.bottom, 16)

            stepsCard
                .padding(.bottom, 18)

            openPortalButton
                .padding(.bottom, 10)

            cancelButton
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 22)
        .background(
            GeometryReader { proxy in
                Color.clear
                    .preference(key: ForgotPasswordSheetHeightKey.self, value: proxy.size.height)
            }
        )
        .onPreferenceChange(ForgotPasswordSheetHeightKey.self) { height in
            measuredHeight?.wrappedValue = height
        }
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 12) {
            iconTile

            VStack(alignment: .leading, spacing: 3) {
                Text("Esqueci minha senha")
                    .font(UNESFont.serif(22))
                    .tracking(-0.33)
                    .foregroundStyle(UNESColor.ink)

                Text("RECUPERAÇÃO · PORTAL UEFS")
                    .font(UNESFont.mono(10))
                    .tracking(0.8)
                    .foregroundStyle(UNESColor.ink3)
            }

            Spacer(minLength: 0)

            closeButton
        }
    }

    private var iconTile: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 13, style: .continuous)
                .fill(UNESColor.accent.opacity(0.12))
                .overlay(
                    RoundedRectangle(cornerRadius: 13, style: .continuous)
                        .strokeBorder(UNESColor.accent.opacity(0.25), lineWidth: 1)
                )
            Image(systemName: "lock.rotation")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(UNESColor.accent)
        }
        .frame(width: 42, height: 42)
    }

    private var closeButton: some View {
        Button(action: { dismiss() }) {
            Image(systemName: "xmark")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(UNESColor.ink2)
                .frame(width: 30, height: 30)
                .background(Circle().fill(UNESColor.surface2))
        }
        .buttonStyle(.plain)
    }

    private var description: some View {
        Text("O UNES não gerencia sua senha. A recuperação é feita direto no portal acadêmico da UEFS — depois de redefinir por lá, volte aqui e entre normalmente.")
            .font(UNESFont.sans(13))
            .lineSpacing(3)
            .foregroundStyle(UNESColor.ink2)
            .padding(.horizontal, 2)
    }

    private var stepsCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            stepRow(number: "1", text: "Abra o Portal Acadêmico")
            divider
            stepRow(number: "2", text: "Use o link \"Esqueci minha senha\"")
            divider
            stepRow(number: "3", text: "Defina a nova senha e volte ao UNES")
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

    private func stepRow(number: String, text: String) -> some View {
        HStack(alignment: .center, spacing: 12) {
            Text(number)
                .font(UNESFont.mono(11, weight: .semibold))
                .foregroundStyle(UNESColor.ink3)
                .frame(width: 22, height: 22)
                .background(Circle().fill(UNESColor.surface2))

            Text(text)
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink)

            Spacer(minLength: 0)
        }
        .padding(.vertical, 2)
    }

    private var divider: some View {
        Rectangle()
            .fill(UNESColor.line)
            .frame(height: 1)
    }

    private var openPortalButton: some View {
        Button(action: onOpenPortal) {
            HStack(spacing: 8) {
                Text("Abrir portal da UEFS")
                    .font(UNESFont.sans(15, weight: .semibold))
                    .tracking(-0.07)
                Image(systemName: "arrow.up.right")
                    .font(.system(size: 12, weight: .semibold))
            }
            .foregroundStyle(UNESColor.surface)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(UNESColor.ink)
            )
            .shadow(color: Color.black.opacity(0.20), radius: 12, x: 0, y: 12)
        }
        .buttonStyle(PressScaleStyle())
    }

    private var cancelButton: some View {
        Button(action: { dismiss() }) {
            Text("Voltar")
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
    }
}

private struct ForgotPasswordSheetHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        ForgotPasswordSheet(onOpenPortal: {})
    }
}

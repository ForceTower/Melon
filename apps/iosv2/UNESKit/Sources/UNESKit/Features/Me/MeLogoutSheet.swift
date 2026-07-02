import SwiftUI

/// The logout confirmation: keep-my-data toggle plus the destructive action.
struct MeLogoutSheet: View {
    var userName: String?
    var onCancel: () -> Void
    var onConfirm: (_ keepData: Bool) -> Void

    @State private var keepData = true
    /// Measured content height so the sheet hugs it instead of a fixed detent.
    @State private var height: CGFloat = 320

    private static let destructiveRed = Color(hex: 0xE5453A)

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(.bottom, 14)
            Text("Você precisará entrar com as suas informações novamente para ver a matrícula, notas e mensagens.")
                .font(.system(size: 13.5, weight: .medium))
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink2)
                .padding(EdgeInsets(top: 2, leading: 2, bottom: 14, trailing: 2))
            keepDataToggle
                .padding(.bottom, 14)
            actions
        }
        .padding(EdgeInsets(top: 24, leading: 18, bottom: 12, trailing: 18))
        .onGeometryChange(for: CGFloat.self) { proxy in
            proxy.size.height
        } action: { measured in
            height = measured
        }
        .presentationBackground(UNESColor.surface)
        .presentationDetents([.height(height)])
        .presentationDragIndicator(.visible)
        .presentationCornerRadiusCompat(30)
    }

    private var header: some View {
        HStack(spacing: 12) {
            Image(systemName: "rectangle.portrait.and.arrow.right")
                .font(.system(size: 18, weight: .medium))
                .foregroundStyle(UNESColor.readable(0xE85D4E))
                .frame(width: 42, height: 42)
                .background(UNESColor.coral.opacity(0.14), in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 13, style: .continuous)
                        .strokeBorder(UNESColor.coral.opacity(0.25))
                }

            VStack(alignment: .leading, spacing: 2) {
                Text("Sair da conta?")
                    .font(.system(size: 22, weight: .bold))
                    .tracking(-0.66)
                    .foregroundStyle(UNESColor.ink)
                if let userName {
                    Text("Sessão · \(userName)")
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var keepDataToggle: some View {
        Toggle(isOn: $keepData) {
            VStack(alignment: .leading, spacing: 1) {
                Text("Manter dados no dispositivo")
                    .font(.system(size: 13.5, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                Text("notas, horários e mensagens ficam salvos")
                    .font(.system(size: 11.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .tint(UNESColor.successGreen)
        .padding(EdgeInsets(top: 11, leading: 13, bottom: 11, trailing: 13))
        .background(UNESColor.surface2)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(UNESColor.line)
        }
    }

    private var actions: some View {
        HStack(spacing: 8) {
            Button(action: onCancel) {
                Text("Cancelar")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(UNESColor.surface2)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .overlay {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .strokeBorder(UNESColor.line)
                    }
            }
            .buttonStyle(TilePressStyle())

            Button {
                onConfirm(keepData)
            } label: {
                HStack(spacing: 7) {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                        .font(.system(size: 13, weight: .semibold))
                    Text("Sair agora")
                        .font(.system(size: 14, weight: .semibold))
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(Self.destructiveRed, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .shadow(color: Self.destructiveRed.opacity(0.35), radius: 12, y: 10)
            }
            .buttonStyle(TilePressStyle())
        }
    }
}

#Preview {
    Color.clear.sheet(isPresented: .constant(true)) {
        MeLogoutSheet(userName: "Mariana Nogueira", onCancel: {}, onConfirm: { _ in })
    }
}

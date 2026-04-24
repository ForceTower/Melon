import SwiftUI
import UIKit

/// Credential vault card — a grouped pair of rows (matrícula + senha) plumbed
/// into a reveal/hide toggle. Password masking runs locally; the copy button
/// is gated on the credentials being visible. Mirrors `CredentialCard` in
/// `screens-settings.jsx`.
struct CredentialCard: View {
    let credentials: SettingsCredentials
    @Binding var revealed: Bool

    var body: some View {
        VStack(spacing: 12) {
            header

            VStack(spacing: 0) {
                CredentialField(label: "matrícula", value: credentials.username, canCopy: true)

                Rectangle()
                    .fill(UNESColor.line)
                    .frame(height: 1)

                CredentialField(
                    label: "senha",
                    value: revealed
                        ? credentials.password
                        : String(repeating: "•", count: credentials.password.count),
                    canCopy: revealed
                )
            }
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(UNESColor.surface2)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(UNESColor.line, lineWidth: 1)
            )
        }
        .padding(.horizontal, 16)
        .padding(.top, 14)
        .padding(.bottom, 12)
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    private var header: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 11, style: .continuous)
                    .fill(SettingsTone.plum.background)
                Image(systemName: "key")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(SettingsTone.plum.foreground)
            }
            .frame(width: 34, height: 34)
            .shadow(color: SettingsTone.plum.background.opacity(0.28), radius: 6, x: 0, y: 4)

            VStack(alignment: .leading, spacing: 2) {
                Text("Credenciais idUFF")
                    .font(UNESFont.sans(13, weight: .semibold))
                    .tracking(-0.07)
                    .foregroundStyle(UNESColor.ink)

                Text(revealed ? "◦ VISÍVEL · OCULTAR EM 30S" : "◦ CRIPTOGRAFADO · FACE ID")
                    .font(UNESFont.mono(9.5))
                    .tracking(0.76)
                    .foregroundStyle(UNESColor.ink4)
            }

            Spacer(minLength: 0)

            Button {
                withAnimation(.easeOut(duration: 0.2)) { revealed.toggle() }
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: revealed ? "eye.slash" : "eye")
                        .font(.system(size: 12, weight: .medium))
                    Text(revealed ? "Ocultar" : "Visualizar")
                        .font(UNESFont.sans(11.5, weight: .medium))
                        .tracking(-0.06)
                }
                .foregroundStyle(revealed ? SettingsTone.plum.foreground : UNESColor.ink)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(
                    Capsule()
                        .fill(revealed ? SettingsTone.plum.background : UNESColor.surface2)
                )
                .overlay(
                    Capsule()
                        .strokeBorder(revealed ? SettingsTone.plum.background : UNESColor.line, lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
        }
    }
}

private struct CredentialField: View {
    let label: String
    let value: String
    let canCopy: Bool

    @State private var copied = false

    var body: some View {
        HStack(spacing: 10) {
            Text(label.uppercased())
                .font(UNESFont.mono(9))
                .tracking(1.08)
                .foregroundStyle(UNESColor.ink4)
                .frame(width: 64, alignment: .leading)

            Text(value)
                .font(UNESFont.mono(13))
                .tracking(0.26)
                .foregroundStyle(UNESColor.ink)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, alignment: .leading)

            Button(action: doCopy) {
                if copied {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 11, weight: .semibold))
                        Text("COPIADO")
                            .font(UNESFont.mono(9))
                            .tracking(0.72)
                    }
                    .foregroundStyle(UNESColor.ink3)
                } else {
                    Image(systemName: "doc.on.doc")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(canCopy ? UNESColor.ink3 : UNESColor.ink4)
                        .opacity(canCopy ? 1 : 0.4)
                }
            }
            .buttonStyle(.plain)
            .disabled(!canCopy)
        }
        .padding(.horizontal, 13)
        .padding(.vertical, 10)
    }

    private func doCopy() {
        guard canCopy else { return }
        UIPasteboard.general.string = value
        withAnimation(.easeOut(duration: 0.15)) { copied = true }
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_400_000_000)
            withAnimation(.easeOut(duration: 0.2)) { copied = false }
        }
    }
}

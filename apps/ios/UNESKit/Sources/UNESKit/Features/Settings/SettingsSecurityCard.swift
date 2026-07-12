import SwiftUI

/// The Segurança row — a plain navigation card into the passkeys manager.
struct SettingsSecurityCard: View {
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 13) {
                Image(systemName: "key.fill")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(width: 30, height: 30)
                    .background(UNESColor.successGreen, in: RoundedRectangle(cornerRadius: 8, style: .continuous))

                VStack(alignment: .leading, spacing: 1) {
                    Text(.settingsPasskeys)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.15)
                        .foregroundStyle(UNESColor.ink)
                    Text(.settingsPasskeysHint)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
        }
        .buttonStyle(TilePressStyle())
    }
}

#Preview {
    SettingsSecurityCard(onTap: {})
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}

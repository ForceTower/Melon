import SwiftUI

/// The Aparência card — the first (and only) writer of the shared theme.
struct SettingsAppearanceCard: View {
    var theme: AppTheme
    var onSelect: (AppTheme) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 11) {
                Image(systemName: "paintpalette")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(width: 30, height: 30)
                    .background(UNESColor.accent, in: RoundedRectangle(cornerRadius: 8, style: .continuous))

                Text("Tema")
                    .font(.system(size: 15, weight: .semibold))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink)
            }

            SettingsSegmented(
                options: [AppTheme.light, .system, .dark].map { ($0, $0.label) },
                selected: theme,
                onSelect: onSelect
            )
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

#Preview {
    SettingsAppearanceCard(theme: .system, onSelect: { _ in })
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}

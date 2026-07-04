import SwiftUI

/// The celebration sheet that slides up when the version-footer easter egg
/// unlocks the secret icons: one row per icon with its story and a "Usar"
/// shortcut, plus a single accent dismiss.
struct SettingsIconUnlockSheet: View {
    var icons: [AppIcon]
    var onUse: (AppIcon) -> Void
    var onDone: () -> Void

    /// Measured content height so the sheet hugs it instead of a fixed detent.
    @State private var height: CGFloat = 320

    var body: some View {
        VStack(spacing: 0) {
            header
                .padding(.bottom, 18)

            VStack(spacing: 10) {
                ForEach(icons) { icon in
                    row(icon)
                }
            }

            doneButton
                .padding(.top, 16)
        }
        .padding(EdgeInsets(top: 26, leading: 20, bottom: 12, trailing: 20))
        .onGeometryChange(for: CGFloat.self) { proxy in
            proxy.size.height
        } action: { measured in
            height = measured
        }
        .presentationBackground(UNESColor.card)
        .presentationDetents([.height(height)])
        .presentationDragIndicator(.visible)
        .presentationCornerRadiusCompat(30)
    }

    private var header: some View {
        VStack(spacing: 6) {
            HStack(spacing: 6) {
                Image(systemName: "sparkles")
                    .font(.system(size: 12, weight: .semibold))
                Text(eyebrow)
                    .font(.system(size: 11.5, weight: .bold))
                    .tracking(0.8)
                    .textCase(.uppercase)
            }
            .foregroundStyle(UNESColor.accent)

            Text(title)
                .font(.system(size: 23, weight: .bold))
                .tracking(-0.69)
                .foregroundStyle(UNESColor.ink)
        }
        .frame(maxWidth: .infinity)
        .multilineTextAlignment(.center)
    }

    // One/Many split instead of plural variations: the "one" wording drops
    // the number entirely, which the catalog compiler rejects in a variation.
    private var eyebrow: String {
        icons.count > 1
            ? String.localized(.settingsAppIconUnlockedEyebrowMany(icons.count))
            : String.localized(.settingsAppIconUnlockedEyebrowOne)
    }

    private var title: String {
        icons.count > 1
            ? String.localized(.settingsAppIconUnlockedTitleMany)
            : String.localized(.settingsAppIconUnlockedTitleOne)
    }

    private func row(_ icon: AppIcon) -> some View {
        HStack(spacing: 14) {
            SettingsAppIconArt(icon: icon, size: 60)
                .shadow(color: Color(hex: 0x141020, opacity: 0.18), radius: 6, y: 4)

            VStack(alignment: .leading, spacing: 2) {
                Text(icon.label)
                    .font(.system(size: 16, weight: .bold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                if let description = icon.secretDescription {
                    Text(description)
                        .font(.system(size: 12.5, weight: .medium))
                        .lineSpacing(2.5)
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button {
                onUse(icon)
            } label: {
                Text(.settingsAppIconUse)
                    .font(.system(size: 12.5, weight: .semibold))
                    .tracking(-0.13)
                    .foregroundStyle(UNESColor.accent)
                    .padding(EdgeInsets(top: 7, leading: 15, bottom: 7, trailing: 15))
                    .overlay {
                        Capsule().strokeBorder(UNESColor.accent)
                    }
            }
            .buttonStyle(TilePressStyle())
        }
        .padding(14)
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var doneButton: some View {
        Button(action: onDone) {
            Text(.commonDone)
                .font(.system(size: 15, weight: .semibold))
                .tracking(-0.15)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 13)
                .background(UNESColor.accent, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
                .shadow(color: .black.opacity(0.14), radius: 10, y: 8)
        }
        .buttonStyle(TilePressStyle())
    }
}

#Preview {
    Color.clear.sheet(isPresented: .constant(true)) {
        SettingsIconUnlockSheet(icons: [.paper], onUse: { _ in }, onDone: {})
    }
}

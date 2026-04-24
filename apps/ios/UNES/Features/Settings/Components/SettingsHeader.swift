import SwiftUI

/// Top chrome of the Settings screen. Mirrors `CfgHeader` in
/// `screens-settings.jsx`, but the custom back pill has been dropped in
/// favour of the system nav bar's native chevron — only the right-aligned
/// sync stamp remains on the top row, followed by the editorial title.
struct SettingsHeader: View {
    let lastSyncLabel: String

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Spacer()

                Text("◦ SINC. \(lastSyncLabel)")
                    .font(UNESFont.mono(9.5))
                    .tracking(1.33)
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(.bottom, 10)

            Text("◦ PREFERÊNCIAS · V4.2.1")
                .font(UNESFont.sans(12, weight: .medium))
                .tracking(1.44)
                .foregroundStyle(UNESColor.ink3)
                .padding(.bottom, 8)

            (
                Text("Config").foregroundStyle(UNESColor.ink)
                + Text("urações").font(UNESFont.serif(40, italic: true)).foregroundStyle(UNESColor.accent)
            )
            .font(UNESFont.serif(40))
            .tracking(-0.8)

            Text("Suas credenciais, o que aparece na notificação de nota e o que te interrompe durante o dia.")
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink3)
                .lineSpacing(2)
                .padding(.top, 10)
                .frame(maxWidth: 290, alignment: .leading)
        }
        .padding(.horizontal, 20)
        .padding(.top, 4)
        .padding(.bottom, 18)
    }
}

/// Section heading that lives above each grouped card. Eyebrow + serif
/// title on the left, monospaced meta chip on the right. Mirrors
/// `CfgSection` in `screens-settings.jsx`.
struct SettingsSectionHeader: View {
    let eyebrow: String
    let title: String
    var meta: String? = nil

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 4) {
                Text("◦ \(eyebrow)")
                    .font(UNESFont.sans(12, weight: .medium))
                    .tracking(1.44)
                    .textCase(.uppercase)
                    .foregroundStyle(UNESColor.ink3)

                Text(title)
                    .font(UNESFont.serif(22))
                    .tracking(-0.33)
                    .foregroundStyle(UNESColor.ink)
            }

            Spacer()

            if let meta {
                Text(meta)
                    .font(UNESFont.mono(9.5))
                    .tracking(0.76)
                    .textCase(.uppercase)
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .padding(.horizontal, 4)
        .padding(.bottom, 10)
    }
}

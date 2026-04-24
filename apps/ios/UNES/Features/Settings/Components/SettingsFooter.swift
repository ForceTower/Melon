import SwiftUI

/// Signature + build metadata stamped at the bottom of the Settings scroll.
/// Mirrors `CfgFooter` in `screens-settings.jsx`.
struct SettingsFooter: View {
    var body: some View {
        VStack(spacing: 6) {
            HStack(spacing: 6) {
                Circle()
                    .fill(UNESColor.accent)
                    .frame(width: 5, height: 5)

                Text("unes")
                    .font(UNESFont.serif(16, italic: true))
                    .tracking(-0.16)
                    .foregroundStyle(UNESColor.ink2)

                Circle()
                    .fill(UNESColor.accent)
                    .frame(width: 5, height: 5)
            }

            Text("V4.2.1 · BUILD 1842 · FEITO EM NITERÓI")
                .font(UNESFont.mono(9))
                .tracking(1.62)
                .foregroundStyle(UNESColor.ink4)

            Text("SINC. E CADÊNCIA NO SERVIDOR · CLIENTE SÓ ESCUTA")
                .font(UNESFont.mono(9))
                .tracking(1.26)
                .foregroundStyle(UNESColor.ink4)
                .padding(.top, 2)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 14)
        .padding(.bottom, 32)
    }
}

import SwiftUI

/// Lock-screen notification mock that reflects the current spoiler setting.
/// Rendered above the spoiler picker so the student sees, before flipping the
/// knob, exactly what a new-grade alert looks like on their device. Mirrors
/// `NotificationPreview` in `screens-settings.jsx`.
struct NotificationPreview: View {
    let spoiler: SpoilerMode

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("◦ PRÉ-VISUALIZAÇÃO · TELA DE BLOQUEIO")
                .font(UNESFont.sans(9, weight: .medium))
                .tracking(1.08)
                .foregroundStyle(UNESColor.ink4)

            HStack(spacing: 11) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(UNESColor.amber)
                    Text("U")
                        .font(UNESFont.serif(15))
                        .foregroundStyle(UNESColor.plum)
                }
                .frame(width: 30, height: 30)

                VStack(alignment: .leading, spacing: 2) {
                    HStack(alignment: .firstTextBaseline) {
                        Text("UNES")
                            .font(UNESFont.sans(10.5, weight: .semibold))
                            .tracking(-0.05)
                            .foregroundStyle(Color(red: 0xF5/255, green: 0xEF/255, blue: 0xE6/255))

                        Spacer(minLength: 6)

                        Text("AGORA")
                            .font(UNESFont.mono(9))
                            .tracking(0.72)
                            .foregroundStyle(Color(red: 0xF5/255, green: 0xEF/255, blue: 0xE6/255).opacity(0.55))
                    }

                    Text(spoiler.previewText)
                        .font(UNESFont.sans(11.5))
                        .tracking(-0.05)
                        .foregroundStyle(Color(red: 0xF5/255, green: 0xEF/255, blue: 0xE6/255))
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 11)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color(red: 0x1C/255, green: 0x16/255, blue: 0x24/255))
            )
            .shadow(color: .black.opacity(0.12), radius: 4, x: 0, y: 4)
        }
        .padding(12)
        .cardSurface(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

import SwiftUI

/// One of the three notification group cards (Mensagens · Notas · Aulas).
/// Holds its own header with the "N/3 ativas" counter and renders a stack of
/// `NotificationToggleRow`s below it. Mirrors `NotifGroupHeader` +
/// `NotifRow` in `screens-settings.jsx`.
struct NotificationGroupCard<Content: View>: View {
    let kicker: String
    let title: String
    let activeCount: Int
    let total: Int
    @ViewBuilder let rows: Content

    var body: some View {
        VStack(spacing: 0) {
            header

            Rectangle()
                .fill(UNESColor.line)
                .frame(height: 1)

            rows
        }
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 2) {
                Text("◦ \(kicker)")
                    .font(UNESFont.mono(8.5, weight: .medium))
                    .tracking(1.19)
                    .textCase(.uppercase)
                    .foregroundStyle(UNESColor.ink4)

                Text(title)
                    .font(UNESFont.serif(17, italic: true))
                    .tracking(-0.17)
                    .foregroundStyle(UNESColor.ink)
            }

            Spacer(minLength: 6)

            Text("\(activeCount)/\(total) ATIVAS")
                .font(UNESFont.mono(9))
                .tracking(0.9)
                .foregroundStyle(UNESColor.ink3)
        }
        .padding(.horizontal, 16)
        .padding(.top, 14)
        .padding(.bottom, 8)
    }
}

/// One row inside a notification group: icon + label + hint + toggle.
/// Last row drops the hairline separator.
struct NotificationToggleRow: View {
    let icon: String
    let tone: SettingsTone
    let label: String
    let hint: String
    @Binding var isOn: Bool
    var showSeparator: Bool = true

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .fill(tone.background)
                    Image(systemName: icon)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(tone.foreground)
                }
                .frame(width: 30, height: 30)
                .shadow(color: tone.background.opacity(0.2), radius: 3, x: 0, y: 3)

                VStack(alignment: .leading, spacing: 2) {
                    Text(label)
                        .font(UNESFont.sans(13.5, weight: .medium))
                        .tracking(-0.07)
                        .foregroundStyle(UNESColor.ink)

                    Text(hint.uppercased())
                        .font(UNESFont.mono(9))
                        .tracking(0.72)
                        .foregroundStyle(UNESColor.ink4)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Toggle("", isOn: $isOn)
                    .labelsHidden()
                    .tint(UNESColor.accent)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)

            if showSeparator {
                Rectangle()
                    .fill(UNESColor.line)
                    .frame(height: 1)
            }
        }
    }
}

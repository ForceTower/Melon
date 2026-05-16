import SwiftUI

/// 158×158pt. Mesh-tinted card with code, short title, and the time/room
/// strip pinned to the bottom — mirrors `IOSSmall` in `screens-widgets.jsx`.
struct NextClassSmallView: View {
    let entry: NextClassEntry
    let theme: WidgetTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 6) {
                LiveDot(color: WidgetColor.amber, size: 5)
                Group {
                    if entry.state == .inClass {
                        Text("agora")
                    } else {
                        eyebrowText(for: entry)
                    }
                }
                .font(WidgetFont.mono(8.5))
                .tracking(1.36)
                .textCase(.uppercase)
                .foregroundStyle(theme.ink3)
            }
            Spacer(minLength: 0)

            VStack(alignment: .leading, spacing: 4) {
                Text(entry.code)
                    .font(WidgetFont.mono(9.5, weight: .semibold))
                    .tracking(1.33)
                    .foregroundStyle(WidgetColor.subjectTeal)
                Text(entry.shortTitle)
                    .font(WidgetFont.serif(24))
                    .tracking(-0.36)
                    .foregroundStyle(theme.ink)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
            }

            Divider()
                .overlay(theme.line)
                .padding(.top, 10)

            HStack {
                Text(entry.startTime)
                    .font(WidgetFont.mono(10))
                Spacer()
                Text(entry.room)
                    .font(WidgetFont.mono(10))
            }
            .padding(.top, 8)
            .foregroundStyle(theme.ink3)
        }
    }
}

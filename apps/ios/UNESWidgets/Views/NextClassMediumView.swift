import SwiftUI

/// 338×158pt. Code pill + full title + topic + room/prof footer.
/// Mirrors `IOSMedium` in `screens-widgets.jsx`.
struct NextClassMediumView: View {
    let entry: NextClassEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                HStack(spacing: 6) {
                    LiveDot(color: WidgetColor.amber, size: 5)
                    Text("em \(formatCountdown(entry.startsIn))")
                        .font(WidgetFont.mono(9.5))
                        .tracking(1.33)
                        .textCase(.uppercase)
                        .foregroundStyle(Color.white.opacity(0.78))
                }
                Spacer()
                Text("\(entry.startTime) – \(entry.endTime)")
                    .font(WidgetFont.mono(9.5))
                    .foregroundStyle(Color.white.opacity(0.55))
            }

            VStack(alignment: .leading, spacing: 6) {
                CodePill(code: entry.code, color: WidgetColor.subjectTeal)
                Text(entry.title)
                    .font(WidgetFont.serif(26))
                    .tracking(-0.39)
                    .foregroundStyle(WidgetColor.surfaceLight)
                    .lineLimit(1)
                    .minimumScaleFactor(0.9)
                if let topic = entry.topic {
                    Text("“\(topic)”")
                        .font(WidgetFont.sans(11))
                        .italic()
                        .foregroundStyle(Color.white.opacity(0.75))
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
            }
            .padding(.top, 14)

            Spacer(minLength: 0)

            Divider()
                .overlay(Color.white.opacity(0.15))

            HStack(spacing: 14) {
                MetaItem(systemImage: "building.2",
                         label: "Sala \(entry.room)",
                         shrinks: false,
                         foreground: Color.white.opacity(0.85))
                Rectangle()
                    .fill(Color.white.opacity(0.2))
                    .frame(width: 1, height: 10)
                MetaItem(systemImage: "person",
                         label: entry.prof,
                         shrinks: true,
                         foreground: Color.white.opacity(0.85))
                Spacer(minLength: 0)
            }
            .padding(.top, 10)
        }
    }
}

import SwiftUI

/// 338×354pt. Adds the today timeline strip and a longer topic line.
/// Mirrors `IOSLarge` in `screens-widgets.jsx`.
struct NextClassLargeView: View {
    let entry: NextClassEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top) {
                HStack(spacing: 6) {
                    LiveDot(color: WidgetColor.amber, size: 6)
                    Text("próxima aula · em \(formatCountdown(entry.startsIn))")
                        .font(WidgetFont.mono(10))
                        .tracking(1.8)
                        .textCase(.uppercase)
                        .foregroundStyle(Color.white.opacity(0.78))
                }
                Spacer()
                Text("\(entry.startTime) – \(entry.endTime)")
                    .font(WidgetFont.mono(10))
                    .foregroundStyle(Color.white.opacity(0.55))
            }

            VStack(alignment: .leading, spacing: 10) {
                CodePill(code: entry.code, color: WidgetColor.subjectTeal, size: .lg)
                Text(entry.title)
                    .font(WidgetFont.serif(34))
                    .tracking(-0.68)
                    .foregroundStyle(WidgetColor.surfaceLight)
                    .lineLimit(2)
                    .minimumScaleFactor(0.85)
                if let topic = entry.topic {
                    HStack(alignment: .top, spacing: 8) {
                        Image(systemName: "text.alignleft")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundStyle(Color.white.opacity(0.55))
                        Text(topic)
                            .font(WidgetFont.sans(12))
                            .italic()
                            .foregroundStyle(Color.white.opacity(0.85))
                            .lineLimit(2)
                            .truncationMode(.tail)
                    }
                }
            }
            .padding(.top, 18)

            TodayStrip(bars: entry.todayBars)
                .padding(.top, 18)

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
                         label: "Prof. \(entry.prof)",
                         shrinks: true,
                         foreground: Color.white.opacity(0.85))
                Spacer(minLength: 0)
            }
            .padding(.top, 12)
        }
    }
}

private struct TodayStrip: View {
    let bars: [NextClassEntry.TodayBar]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("◦ seu dia")
                .font(WidgetFont.mono(9))
                .tracking(1.44)
                .textCase(.uppercase)
                .foregroundStyle(Color.white.opacity(0.6))
            HStack(spacing: 6) {
                ForEach(bars, id: \.self) { bar in
                    TodayCell(bar: bar)
                }
            }
        }
    }
}

private struct TodayCell: View {
    let bar: NextClassEntry.TodayBar

    var body: some View {
        let isNext = bar.state == .next
        let isDone = bar.state == .done

        VStack(alignment: .leading, spacing: 3) {
            Text(bar.code)
                .font(WidgetFont.mono(9, weight: .semibold))
                .tracking(0.9)
                .foregroundStyle(bar.color)
            Text(bar.time)
                .font(WidgetFont.mono(9.5))
                .foregroundStyle(Color.white.opacity(0.75))
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(isNext ? bar.color.opacity(0.19) : Color.white.opacity(0.06))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .stroke(isNext ? bar.color.opacity(0.4) : Color.white.opacity(0.06), lineWidth: 1)
        )
        .opacity(isDone ? 0.4 : 1)
        // `next` is emphasized at flex 2 in the design; mirror that with a
        // layout priority so it grabs roughly double the row.
        .layoutPriority(isNext ? 2 : 1)
    }
}

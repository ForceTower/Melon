import SwiftUI

/// 338×354pt. Adds the today timeline strip and a longer topic line.
/// Mirrors `IOSLarge` in `screens-widgets.jsx`.
struct NextClassLargeView: View {
    let entry: NextClassEntry
    let theme: WidgetTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                HStack(spacing: 6) {
                    LiveDot(color: WidgetColor.amber, size: 6)
                    eyebrowText(for: entry)
                        .font(WidgetFont.mono(10))
                        .tracking(1.4)
                        .textCase(.uppercase)
                        .foregroundStyle(theme.ink3)
                }
                Spacer()
                Text("\(entry.startTime) – \(entry.endTime)")
                    .font(WidgetFont.mono(10))
                    .foregroundStyle(theme.ink4)
            }

            VStack(alignment: .leading, spacing: 10) {
                CodePill(code: entry.code, color: WidgetColor.subjectTeal, size: .lg)
                Text(entry.title)
                    .font(WidgetFont.serif(34))
                    .tracking(-0.68)
                    .foregroundStyle(theme.ink)
                    .lineLimit(2)
                    .minimumScaleFactor(0.85)
                if let topic = entry.topic {
                    HStack(alignment: .top, spacing: 8) {
                        Image(systemName: "text.alignleft")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundStyle(theme.ink3)
                        Text(topic)
                            .font(WidgetFont.sans(12))
                            .italic()
                            .foregroundStyle(theme.ink2)
                            .lineLimit(2)
                            .truncationMode(.tail)
                    }
                }
            }
            .padding(.top, 18)

            TodayStrip(bars: entry.todayBars, theme: theme)
                .padding(.top, 18)

            Spacer(minLength: 0)

            Divider()
                .overlay(theme.line)

            HStack(spacing: 14) {
                MetaItem(systemImage: "building.2",
                         label: "Sala \(entry.room)",
                         shrinks: false,
                         foreground: theme.ink2,
                         iconForeground: theme.ink3)
                Rectangle()
                    .fill(theme.divider)
                    .frame(width: 1, height: 10)
                MetaItem(systemImage: "person",
                         label: "Prof. \(entry.prof)",
                         shrinks: true,
                         foreground: theme.ink2,
                         iconForeground: theme.ink3)
                Spacer(minLength: 0)
            }
            .padding(.top, 12)
        }
    }
}

private struct TodayStrip: View {
    let bars: [NextClassEntry.TodayBar]
    let theme: WidgetTheme

    /// Mirrors the CSS `flex: state === 'next' ? 2 : 1` rule from the design.
    /// `layoutPriority` doesn't proportionally divide width in SwiftUI — it
    /// only breaks ties — so a GeometryReader split is used instead. With
    /// priority alone, the `next` cell devoured the row and the others
    /// collapsed below their intrinsic width, hiding the code/time text.
    private func weight(for bar: NextClassEntry.TodayBar) -> CGFloat {
        bar.state == .next ? 2 : 1
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("◦ seu dia")
                .font(WidgetFont.mono(9))
                .tracking(1.44)
                .textCase(.uppercase)
                .foregroundStyle(theme.ink3)

            GeometryReader { geo in
                let spacing: CGFloat = 6
                let totalSpacing = spacing * CGFloat(max(0, bars.count - 1))
                let totalWeight = bars.reduce(CGFloat(0)) { $0 + weight(for: $1) }
                let unit = totalWeight > 0
                    ? (geo.size.width - totalSpacing) / totalWeight
                    : 0
                HStack(spacing: spacing) {
                    ForEach(bars, id: \.self) { bar in
                        TodayCell(bar: bar, theme: theme)
                            .frame(width: unit * weight(for: bar))
                    }
                }
            }
            .frame(height: 40)
        }
    }
}

private struct TodayCell: View {
    let bar: NextClassEntry.TodayBar
    let theme: WidgetTheme

    var body: some View {
        let isNext = bar.state == .next
        let isDone = bar.state == .done
        let fill = isNext ? bar.color.opacity(0.19) : theme.todayCellBackground
        let stroke = isNext ? bar.color.opacity(0.4) : theme.cardLine

        VStack(alignment: .leading, spacing: 3) {
            Text(bar.code)
                .font(WidgetFont.mono(9, weight: .semibold))
                .tracking(0.9)
                .foregroundStyle(bar.color)
                .lineLimit(1)
                .minimumScaleFactor(0.85)
            Text(bar.time)
                .font(WidgetFont.mono(9.5))
                .foregroundStyle(theme.ink3)
                .lineLimit(1)
                .minimumScaleFactor(0.85)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(fill)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .stroke(stroke, lineWidth: 1)
        )
        .opacity(isDone ? 0.4 : 1)
    }
}

import SwiftUI

/// 338×158pt "Aula em andamento" — same hero as Medium but the eyebrow flips
/// to a count-down to end and the bottom row is replaced by a progress bar.
/// Mirrors `StateInClass` in `screens-widgets.jsx`.
struct InClassMediumView: View {
    let entry: NextClassEntry
    let theme: WidgetTheme

    private var progress: Double {
        guard entry.totalDurationMin > 0 else { return 0 }
        let elapsed = max(0, entry.totalDurationMin - entry.endsIn)
        return min(1, Double(elapsed) / Double(entry.totalDurationMin))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                HStack(spacing: 6) {
                    LiveDot(color: WidgetColor.amber, size: 5)
                    Text("agora · termina em \(formatCountdown(entry.endsIn))")
                        .font(WidgetFont.mono(9.5, weight: .semibold))
                        .tracking(1.52)
                        .textCase(.uppercase)
                        .foregroundStyle(WidgetColor.amber)
                }
                Spacer()
                Text("\(entry.startTime) – \(entry.endTime)")
                    .font(WidgetFont.mono(9.5))
                    .foregroundStyle(theme.ink4)
            }

            VStack(alignment: .leading, spacing: 6) {
                CodePill(code: entry.code, color: WidgetColor.subjectTeal)
                Text(entry.title)
                    .font(WidgetFont.serif(26))
                    .tracking(-0.39)
                    .foregroundStyle(theme.ink)
                    .lineLimit(1)
                    .minimumScaleFactor(0.9)
                if !entry.room.isEmpty {
                    Text(entry.room)
                        .font(WidgetFont.sans(12))
                        .foregroundStyle(theme.ink3)
                        .lineLimit(1)
                }
            }
            .padding(.top, 14)

            Spacer(minLength: 0)

            VStack(alignment: .leading, spacing: 6) {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 3, style: .continuous)
                            .fill(theme.progressTrack)
                        RoundedRectangle(cornerRadius: 3, style: .continuous)
                            .fill(WidgetColor.amber)
                            .frame(width: geo.size.width * progress)
                    }
                }
                .frame(height: 5)

                HStack {
                    Text("\(entry.startTime) · iniciada")
                    Spacer()
                    Text("\(entry.endTime) · final")
                }
                .font(WidgetFont.mono(9.5))
                .tracking(0.38)
                .foregroundStyle(theme.ink3)
            }
        }
    }
}

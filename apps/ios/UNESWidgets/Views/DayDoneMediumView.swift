import SwiftUI

/// 338×158pt "Dia concluído" — flat surface (no mesh), accent-colored
/// "amanhã, hh:mm" callout, dotted count of finished classes. Mirrors
/// `StateDayDone` in `screens-widgets.jsx`. Both themes use the coral
/// accent for the time so the highlight reads regardless of background.
struct DayDoneMediumView: View {
    let entry: NextClassEntry
    let theme: WidgetTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 6) {
                Text("◦ tudo certo por hoje")
                    .font(WidgetFont.mono(9.5))
                    .tracking(1.52)
                    .textCase(.uppercase)
                    .foregroundStyle(theme.ink3)
            }

            VStack(alignment: .leading, spacing: 4) {
                tomorrowLine
                    .font(WidgetFont.serif(26))
                    .tracking(-0.39)
                    .foregroundStyle(theme.ink)
                    .lineLimit(2)
                    .minimumScaleFactor(0.85)

                if let line = entry.dayDoneLine {
                    Text(line)
                        .font(WidgetFont.sans(12))
                        .foregroundStyle(theme.ink3)
                        .lineLimit(1)
                }
            }
            .padding(.top, 14)

            Spacer(minLength: 0)

            Divider()
                .overlay(theme.line)

            HStack {
                Text("\(entry.completedTodayCount) aulas concluídas")
                    .font(WidgetFont.sans(11))
                    .foregroundStyle(theme.ink3)
                Spacer()
                HStack(spacing: 4) {
                    ForEach(0..<entry.completedTodayCount, id: \.self) { _ in
                        Circle()
                            .fill(Color(red: 0x4A / 255, green: 0xA6 / 255, blue: 0x79 / 255))
                            .frame(width: 6, height: 6)
                    }
                }
            }
            .padding(.top, 10)
        }
    }

    /// First line picks up a coral-accented "amanhã, 08:00" segment when the
    /// raw line follows the `… amanhã, HH:MM · …` shape; falls back to plain
    /// text otherwise.
    @ViewBuilder
    private var tomorrowLine: some View {
        if let line = entry.dayDoneLine,
           let timeRange = line.range(of: #"amanhã, \d{2}:\d{2}"#, options: .regularExpression) {
            let accent = String(line[timeRange])
            Text("Sem aulas até ") + Text(accent)
                .foregroundStyle(WidgetColor.coral)
                .italic()
        } else {
            Text("Sem aulas até amanhã")
        }
    }
}

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

                if !entry.title.isEmpty {
                    Text(entry.title)
                        .font(WidgetFont.sans(12))
                        .foregroundStyle(theme.ink3)
                        .lineLimit(1)
                }

                if !entry.room.isEmpty {
                    Text(entry.room)
                        .font(WidgetFont.sans(11))
                        .foregroundStyle(theme.ink4)
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

    /// "Sem aulas até <DAY>, HH:MM" with the day+time portion in coral
    /// italic. Pulls the day+time prefix from `dayDoneLine`, which is
    /// shaped `"<when>, HH:MM · <title> — <room>"` (see WidgetSnapshot
    /// `dayDoneLine`). `<when>` is "amanhã" when the next class is the
    /// calendar day after now, otherwise the weekday name ("segunda" …).
    @ViewBuilder
    private var tomorrowLine: some View {
        if let line = entry.dayDoneLine,
           let separator = line.range(of: " · ") {
            let head = String(line[..<separator.lowerBound])
            Text("Sem aulas até ") + Text(head)
                .foregroundStyle(WidgetColor.coral)
                .italic()
        } else {
            Text("Sem aulas hoje")
        }
    }
}

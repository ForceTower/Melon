import SwiftUI

/// Month section in the agenda variant: month-name title + a vertical stack of
/// `CalAgendaRow`. The header sticks to the top of the scroll viewport (and
/// shows an "agora" pill when this is the current month).
struct CalMonthSection: View {
    let group: CalendarMonthGroup

    private var isCurrent: Bool {
        let cal = Calendar.current
        let comps = cal.dateComponents([.year, .month], from: CalendarMath.today)
        return comps.year == group.year && comps.month == group.month
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(.horizontal, 6)
                .padding(.top, 14)
                .padding(.bottom, 12)

            ForEach(Array(group.events.enumerated()), id: \.element.id) { idx, ev in
                CalAgendaRow(event: ev, isLast: idx == group.events.count - 1)
            }
        }
        .padding(.bottom, 18)
    }

    private var header: some View {
        let monthName = CalendarFormat.monthsLong[group.month - 1]
        let countLabel = group.events.count == 1 ? "evento" : "eventos"
        return HStack(alignment: .lastTextBaseline, spacing: 10) {
            Text(monthName)
                .font(UNESFont.serif(26, italic: true))
                .tracking(-0.52)
                .foregroundStyle(isCurrent ? UNESColor.accent : UNESColor.ink)

            Text("\(String(group.year)) · \(group.events.count) \(countLabel)")
                .font(UNESFont.mono(10))
                .tracking(1)
                .foregroundStyle(UNESColor.ink3)

            if isCurrent {
                Spacer(minLength: 0)
                nowBadge
            }
        }
    }

    private var nowBadge: some View {
        HStack(spacing: 5) {
            Circle()
                .fill(UNESColor.accent)
                .frame(width: 5, height: 5)
                .pulseForever()
            Text("AGORA")
                .font(UNESFont.mono(9.5, weight: .semibold))
                .tracking(1.33)
                .foregroundStyle(UNESColor.accent)
        }
    }
}

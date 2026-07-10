import SwiftUI

/// iOS-calendar-style week strip: one column per day with the weekday
/// letter, the date in a circle (filled with the accent when selected), and
/// up to four dots for that day's class count.
struct ScheduleWeekStrip: View {
    let days: [ScheduleDay]
    let selectedIndex: Int
    let todayIndex: Int?
    var onSelect: (Int) -> Void

    var body: some View {
        HStack(spacing: 2) {
            ForEach(Array(days.enumerated()), id: \.element.id) { index, day in
                column(day, index: index)
            }
        }
        .animation(UNESMotion.ease(0.2, overshoot: 1.2), value: selectedIndex)
    }

    private func column(_ day: ScheduleDay, index: Int) -> some View {
        let isActive = index == selectedIndex
        let isToday = index == todayIndex
        let isWeekend = index >= 5

        return Button {
            onSelect(index)
        } label: {
            VStack(spacing: 6) {
                Text(ScheduleFormat.dayLetters[index])
                    .font(.system(size: 12, weight: .semibold))
                    .tracking(-0.12)
                    .foregroundStyle(isToday ? UNESColor.accent : (isWeekend ? UNESColor.ink4 : UNESColor.ink3))

                Text(verbatim: "\(day.dayNumber)")
                    .font(.system(size: 17, weight: isActive || isToday ? .bold : .medium))
                    .tracking(-0.34)
                    .monospacedDigit()
                    .foregroundStyle(numberColor(isActive: isActive, isToday: isToday, isWeekend: isWeekend))
                    .frame(width: 36, height: 36)
                    .background {
                        if isActive {
                            Circle().fill(UNESColor.accent)
                        }
                    }

                HStack(spacing: 2.5) {
                    ForEach(0..<min(day.classes.count, 4), id: \.self) { _ in
                        Circle()
                            .fill(isActive ? UNESColor.accent : UNESColor.ink4.opacity(0.5))
                            .frame(width: 4, height: 4)
                    }
                }
                .frame(height: 4)
            }
            .padding(EdgeInsets(top: 6, leading: 0, bottom: 8, trailing: 0))
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func numberColor(isActive: Bool, isToday: Bool, isWeekend: Bool) -> Color {
        if isActive { return .white }
        if isToday { return UNESColor.accent }
        return isWeekend ? UNESColor.ink4 : UNESColor.ink
    }
}

#Preview {
    ScheduleWeekStrip(days: ScheduleOverview.preview().days, selectedIndex: 3, todayIndex: 3) { _ in }
        .padding(.horizontal, 12)
        .background(UNESColor.surface)
}

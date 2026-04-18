import SwiftUI

/// Horizontal 7-pill week selector used by the focused-day schedule variant.
/// Each pill shows weekday, date, and class-count dots; the active day is
/// filled with ink, the today marker shows as an accent dot when inactive.
struct WeekSpine: View {
    @Binding var activeIdx: Int
    var entering: Bool

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<7, id: \.self) { i in
                pill(for: i)
                    .frame(maxWidth: .infinity)
                    .fadeUpOnAppear(
                        delay: entering ? 0.12 + Double(i) * 0.04 : 0,
                        distance: 8,
                        duration: 0.4
                    )
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 14)
    }

    private func pill(for i: Int) -> some View {
        let isActive = i == activeIdx
        let isToday  = i == ScheduleFixtures.todayIdx
        let isWeekend = i >= 5
        let count = ScheduleFixtures.week[i].count

        return Button {
            withAnimation(.spring(response: 0.32, dampingFraction: 0.78)) {
                activeIdx = i
            }
        } label: {
            ZStack(alignment: .topTrailing) {
                VStack(spacing: 4) {
                    Text(ScheduleFixtures.daysShort[i].uppercased())
                        .font(UNESFont.sans(10, weight: .medium))
                        .tracking(0.8)
                        .foregroundStyle(pillPrimary(isActive: isActive, isWeekend: isWeekend))
                        .opacity(isActive ? 0.7 : 0.6)
                    Text("\(ScheduleFixtures.dates[i])")
                        .font(UNESFont.serif(20))
                        .tracking(-0.2)
                        .foregroundStyle(pillPrimary(isActive: isActive, isWeekend: isWeekend))
                    HStack(spacing: 2) {
                        ForEach(0..<min(count, 4), id: \.self) { _ in
                            Circle()
                                .fill(isActive ? UNESColor.surface : UNESColor.ink4)
                                .opacity(isActive ? 0.7 : 0.5)
                                .frame(width: 3, height: 3)
                        }
                    }
                    .frame(minHeight: 3)
                    .padding(.top, 3)
                }
                .padding(.vertical, 9)
                .padding(.horizontal, 4)
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(isActive ? UNESColor.ink : Color.clear)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(isActive ? Color.clear : UNESColor.line, lineWidth: 1)
                )

                if isToday && !isActive {
                    Circle()
                        .fill(UNESColor.accent)
                        .frame(width: 5, height: 5)
                        .padding(.top, 4)
                        .padding(.trailing, 6)
                }
            }
        }
        .buttonStyle(.plain)
        .animation(.easeInOut(duration: 0.2), value: activeIdx)
    }

    private func pillPrimary(isActive: Bool, isWeekend: Bool) -> Color {
        if isActive { return UNESColor.surface }
        return isWeekend ? UNESColor.ink4 : UNESColor.ink2
    }
}

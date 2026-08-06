import SwiftUI

/// The agenda under the grid: one inset card per day with a row for each
/// class, so long titles and locations stay readable at full width.
struct ScheduleGridAgendaList: View {
    let days: [ScheduleDay]
    let layout: ScheduleGridLayout
    let todayIndex: Int?
    let nowMinutes: Int
    var onClassTap: (ScheduleClass, Int) -> Void

    var body: some View {
        let populated = layout.dayIndices.filter { !days[$0].classes.isEmpty }
        VStack(spacing: 20) {
            ForEach(Array(populated.enumerated()), id: \.element) { position, dayIndex in
                section(dayIndex: dayIndex, position: position, rowsBefore: rowsBefore(dayIndex, in: populated))
            }
        }
        .padding(EdgeInsets(top: 4, leading: 16, bottom: 28, trailing: 16))
    }

    /// Rows in earlier sections, continuing the entrance stagger across cards.
    private func rowsBefore(_ dayIndex: Int, in populated: [Int]) -> Int {
        populated.prefix { $0 != dayIndex }.reduce(0) { $0 + days[$1].classes.count }
    }

    private func section(dayIndex: Int, position: Int, rowsBefore: Int) -> some View {
        let day = days[dayIndex]
        let isToday = dayIndex == todayIndex
        return VStack(spacing: 0) {
            HStack(alignment: .lastTextBaseline, spacing: 8) {
                Text(ScheduleFormat.dayNames[dayIndex])
                    .textCase(.uppercase)
                    .font(.system(size: 13, weight: .bold))
                    .tracking(0.4)
                    .foregroundStyle(isToday ? UNESColor.accent : UNESColor.ink3)

                if let date = HomeFormat.shortDate(fromDayStamp: day.dayStamp) {
                    Text(date)
                        .font(.system(size: 13, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink4)
                }

                Spacer()
            }
            .padding(EdgeInsets(top: 0, leading: 4, bottom: 7, trailing: 4))
            .fadeUp(delay: 0.34 + Double(rowsBefore + position) * 0.05)

            VStack(spacing: 0) {
                ForEach(Array(day.classes.enumerated()), id: \.element.id) { index, scheduleClass in
                    row(
                        scheduleClass,
                        isToday: isToday,
                        isLast: index == day.classes.count - 1,
                        dayIndex: dayIndex,
                        delay: 0.34 + Double(rowsBefore + index) * 0.05
                    )
                }
            }
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(UNESColor.cardLine, lineWidth: 0.5)
            }
        }
    }

    private func row(
        _ scheduleClass: ScheduleClass,
        isToday: Bool,
        isLast: Bool,
        dayIndex: Int,
        delay: Double
    ) -> some View {
        let state = scheduleClass.state(isToday: isToday, nowMinutes: nowMinutes)
        return Button {
            onClassTap(scheduleClass, dayIndex)
        } label: {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 1) {
                    Text(ScheduleFormat.timeLabel(scheduleClass.startMinute))
                        .font(.system(size: 14, weight: .semibold))
                        .tracking(-0.28)
                        .foregroundStyle(UNESColor.ink)
                    if let end = scheduleClass.endMinute {
                        Text(ScheduleFormat.timeLabel(end))
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(UNESColor.ink4)
                    }
                }
                .monospacedDigit()
                .frame(width: 44, alignment: .leading)

                RoundedRectangle(cornerRadius: 2)
                    .fill(UNESColor.disciplineReadableColor(scheduleClass.colorIndex))
                    .frame(width: 3)

                VStack(alignment: .leading, spacing: 1) {
                    Text(scheduleClass.title)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.3)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                    // Building labels can be whole sentences — wrap, don't elide.
                    Text(ScheduleGridFormat.agendaLocationLine(scheduleClass))
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .fixedSize(horizontal: false, vertical: true)
                    if let teacher = scheduleClass.teacherName {
                        Text(teacher)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                            .lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if state == .now {
                    Text(.commonNow)
                        .textCase(.uppercase)
                        .font(.system(size: 10, weight: .bold))
                        .tracking(0.3)
                        .foregroundStyle(.white)
                        .padding(EdgeInsets(top: 2, leading: 6, bottom: 2, trailing: 6))
                        .background(UNESColor.alertRed, in: RoundedRectangle(cornerRadius: 6, style: .continuous))
                }

                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
                    .opacity(0.7)
            }
            .padding(EdgeInsets(top: 11, leading: 14, bottom: 11, trailing: 14))
            .fixedSize(horizontal: false, vertical: true)
            .contentShape(Rectangle())
        }
        .buttonStyle(.pressableCard)
        .opacity(state == .done ? 0.5 : 1)
        .overlay(alignment: .bottom) {
            if !isLast {
                Rectangle()
                    .fill(UNESColor.line)
                    .frame(height: 0.5)
            }
        }
        .fadeUp(delay: delay)
    }
}

#Preview {
    let overview = ScheduleOverview.preview()
    ScrollView {
        ScheduleGridAgendaList(
            days: overview.days,
            layout: ScheduleGridLayout(days: overview.days),
            todayIndex: 3,
            nowMinutes: 10 * 60 + 52
        ) { _, _ in }
    }
    .background(UNESColor.surface)
}

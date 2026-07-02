import SwiftUI

/// The selected day's session list: class rows with free-time markers
/// between them and, on today, a red now-line threaded through the gaps.
struct ScheduleDayTimeline: View {
    let day: ScheduleDay
    let isToday: Bool
    let nowMinutes: Int
    var onClassTap: (ScheduleClass) -> Void

    var body: some View {
        if day.classes.isEmpty {
            emptyDay
        } else {
            VStack(spacing: 4) {
                ForEach(Array(day.classes.enumerated()), id: \.element.id) { index, cls in
                    if index == nowLineIndex {
                        ScheduleNowLine(minutes: nowMinutes)
                    }
                    if index > 0, let gap = gapMinutes(before: index), gap > 30 {
                        ScheduleGapMarker(minutes: gap)
                    }
                    ScheduleClassRow(cls: cls, state: cls.state(isToday: isToday, nowMinutes: nowMinutes)) {
                        onClassTap(cls)
                    }
                    .slideIn(delay: 0.24 + Double(index) * 0.07)
                }
                if nowLineIndex == day.classes.count {
                    ScheduleNowLine(minutes: nowMinutes)
                }
            }
        }
    }

    /// The slot the now-line occupies — only when now falls in a gap on
    /// today, never while a class is running.
    private var nowLineIndex: Int? {
        guard isToday else { return nil }
        let inClass = day.classes.contains {
            nowMinutes >= $0.startMinute && nowMinutes < ($0.endMinute ?? $0.startMinute)
        }
        guard !inClass else { return nil }
        return day.classes.firstIndex { $0.startMinute > nowMinutes } ?? day.classes.count
    }

    private func gapMinutes(before index: Int) -> Int? {
        let previous = day.classes[index - 1]
        guard let previousEnd = previous.endMinute else { return nil }
        return day.classes[index].startMinute - previousEnd
    }

    private var emptyDay: some View {
        VStack(spacing: 0) {
            ZStack {
                Circle().fill(UNESColor.surface2)
                Image(systemName: "calendar")
                    .font(.system(size: 26, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(width: 64, height: 64)
            .padding(.bottom, 16)

            Text("Dia livre")
                .font(.system(size: 20, weight: .bold))
                .tracking(-0.4)
                .foregroundStyle(UNESColor.ink)
            Text("Nenhuma aula agendada. Aproveite.")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding(EdgeInsets(top: 60, leading: 30, bottom: 40, trailing: 30))
    }
}

/// "── 1h20 livre ──" between two sessions.
private struct ScheduleGapMarker: View {
    let minutes: Int

    var body: some View {
        HStack(spacing: 10) {
            line
            Text("\(ScheduleFormat.durationLabel(minutes)) livre")
                .font(.system(size: 11, weight: .semibold))
                .tracking(0.2)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)
                .fixedSize()
            line
        }
        .padding(EdgeInsets(top: 8, leading: 62, bottom: 8, trailing: 4))
    }

    private var line: some View {
        Rectangle()
            .fill(UNESColor.line)
            .frame(height: 1)
    }
}

/// The red current-time indicator: time label, glowing dot, and a hairline.
private struct ScheduleNowLine: View {
    let minutes: Int

    var body: some View {
        HStack(spacing: 8) {
            Text(HomeFormat.nowLabel(minutes: minutes))
                .font(.system(size: 11, weight: .bold))
                .monospacedDigit()
                .foregroundStyle(UNESColor.alertRed)
                .frame(width: 46, alignment: .trailing)

            Circle()
                .fill(UNESColor.alertRed)
                .frame(width: 9, height: 9)
                .background {
                    Circle()
                        .fill(UNESColor.alertRed.opacity(0.18))
                        .padding(-3)
                }

            RoundedRectangle(cornerRadius: 1)
                .fill(UNESColor.alertRed)
                .frame(height: 2)
        }
        .padding(EdgeInsets(top: 2, leading: 8, bottom: 2, trailing: 4))
    }
}

#Preview {
    ScrollView {
        ScheduleDayTimeline(
            day: ScheduleOverview.preview().days[3],
            isToday: true,
            nowMinutes: 12 * 60 + 40
        ) { _ in }
            .padding(EdgeInsets(top: 20, leading: 12, bottom: 20, trailing: 16))
    }
    .background(UNESColor.surface)
}

#Preview("Dia livre") {
    ScheduleDayTimeline(
        day: ScheduleOverview.preview().days[6],
        isToday: false,
        nowMinutes: 600
    ) { _ in }
        .background(UNESColor.surface)
}

import SwiftUI

/// SEG/TER/… column labels with the day-of-month beneath, today's circled in
/// the accent. Shares the canvas's rail inset so columns line up.
struct ScheduleGridDayHeader: View {
    let days: [ScheduleDay]
    let layout: ScheduleGridLayout
    let todayIndex: Int?

    var body: some View {
        HStack(spacing: 0) {
            Color.clear
                .frame(width: ScheduleGridLayout.railWidth, height: 1)

            ForEach(layout.dayIndices, id: \.self) { dayIndex in
                let isToday = dayIndex == todayIndex
                VStack(spacing: 3) {
                    Text(ScheduleGridFormat.dayAbbreviations[dayIndex])
                        .font(.system(size: 10, weight: .semibold))
                        .tracking(0.4)
                        .foregroundStyle(isToday ? UNESColor.accent : UNESColor.ink4)

                    Text(verbatim: "\(days[dayIndex].dayNumber)")
                        .font(.system(size: 15, weight: isToday ? .bold : .medium))
                        .tracking(-0.3)
                        .monospacedDigit()
                        .foregroundStyle(isToday ? .white : UNESColor.ink)
                        .frame(width: 25, height: 25)
                        .background {
                            if isToday {
                                Circle().fill(UNESColor.accent)
                            }
                        }
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(EdgeInsets(top: 10, leading: 12, bottom: 8, trailing: 12))
    }
}

/// The proportional week canvas: hour rail with a dashed edge, hairlines,
/// one column per visible day with tinted class blocks, and the red now line
/// threading across today.
struct ScheduleGridWeekCanvas: View {
    let days: [ScheduleDay]
    let layout: ScheduleGridLayout
    let todayIndex: Int?
    let nowMinutes: Int
    var onClassTap: (ScheduleClass, Int) -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            rail
            columns
        }
        .padding(.horizontal, 12)
    }

    // MARK: Hour rail

    private var rail: some View {
        ZStack(alignment: .topTrailing) {
            ForEach(layout.hours, id: \.self) { hour in
                Text(verbatim: "\(hour):00")
                    .font(.system(size: 11, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.trailing, 9)
                    .offset(y: layout.y(ofMinute: hour * 60) - 7)
            }
        }
        .frame(width: ScheduleGridLayout.railWidth, height: layout.totalHeight, alignment: .topTrailing)
        .overlay(alignment: .trailing) {
            VerticalLine()
                .stroke(UNESColor.line, style: StrokeStyle(lineWidth: 1, dash: [3, 3]))
                .frame(width: 1)
        }
    }

    // MARK: Day columns

    private var columns: some View {
        ZStack(alignment: .topLeading) {
            ForEach(layout.hours, id: \.self) { hour in
                Rectangle()
                    .fill(UNESColor.line)
                    .frame(height: 0.5)
                    .offset(y: layout.y(ofMinute: hour * 60))
            }

            HStack(spacing: 0) {
                ForEach(Array(layout.dayIndices.enumerated()), id: \.element) { position, dayIndex in
                    column(dayIndex: dayIndex, position: position)
                }
            }
            .frame(height: layout.totalHeight)

            if let todayIndex, layout.dayIndices.contains(todayIndex), layout.containsNow(nowMinutes) {
                nowLine
            }
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
    }

    private func column(dayIndex: Int, position: Int) -> some View {
        let isToday = dayIndex == todayIndex
        return ZStack(alignment: .top) {
            if isToday {
                UNESColor.ink.opacity(0.03)
            }

            ForEach(days[dayIndex].classes) { scheduleClass in
                ScheduleGridEventBlock(
                    scheduleClass: scheduleClass,
                    state: scheduleClass.state(isToday: isToday, nowMinutes: nowMinutes),
                    compact: layout.isCompact,
                    height: layout.blockHeight(for: scheduleClass),
                    delay: entranceDelay(for: scheduleClass, position: position)
                ) {
                    onClassTap(scheduleClass, dayIndex)
                }
                .offset(y: layout.y(ofMinute: scheduleClass.startMinute))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .overlay(alignment: .leading) {
            if position > 0 {
                Rectangle()
                    .fill(UNESColor.line)
                    .frame(width: 0.5)
            }
        }
    }

    /// Blocks pop in left to right, morning before afternoon — the design's
    /// staggered cascade.
    private func entranceDelay(for scheduleClass: ScheduleClass, position: Int) -> Double {
        let dayProgress = Double(scheduleClass.startMinute - layout.startHour * 60)
            / Double((layout.endHour - layout.startHour) * 60)
        return 0.08 + Double(position) * 0.07 + dayProgress * 0.34
    }

    // MARK: Now line

    private var nowLine: some View {
        ZStack(alignment: .leading) {
            Rectangle()
                .fill(UNESColor.alertRed)
                .frame(height: 1.5)
            Circle()
                .fill(UNESColor.alertRed)
                .frame(width: 8, height: 8)
                .offset(x: -3)
        }
        .padding(.leading, -6)
        .offset(y: layout.y(ofMinute: nowMinutes) - 0.75)
        .fadeIn(delay: 0.5, duration: 0.5)
        .allowsHitTesting(false)
        .zIndex(5)
    }
}

/// One tinted class block. Fills with the discipline color while running,
/// fades once done; content lines appear as the block gets taller.
private struct ScheduleGridEventBlock: View {
    let scheduleClass: ScheduleClass
    let state: ScheduleClassState
    let compact: Bool
    let height: CGFloat
    let delay: Double
    var onTap: () -> Void

    @Environment(\.colorScheme) private var colorScheme

    private var isNow: Bool { state == .now }
    private var isDone: Bool { state == .done }
    private var readable: Color { UNESColor.disciplineReadableColor(scheduleClass.colorIndex) }

    var body: some View {
        Button(action: onTap) {
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .background { background }
                .overlay(alignment: .leading) {
                    Rectangle()
                        .fill(isNow ? .white.opacity(0.55) : readable)
                        .frame(width: 3)
                }
                .clipShape(RoundedRectangle(cornerRadius: 7, style: .continuous))
        }
        .buttonStyle(.pressableCard)
        .frame(height: max(height, 8))
        .padding(.horizontal, 1)
        .opacity(isDone ? 0.5 : 1)
        .popIn(delay: delay, duration: 0.42, from: 0.82, overshoot: 1.06)
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(scheduleClass.code)
                .font(.system(size: compact ? 10 : 11, weight: .bold))
                .tracking(-0.11)
                .foregroundStyle(isNow ? .white : readable)
                .lineLimit(1)

            if height > 30 {
                Text(ScheduleGridFormat.locationShort(scheduleClass))
                    .font(.system(size: compact ? 9 : 10, weight: .medium))
                    .foregroundStyle(isNow ? .white.opacity(0.85) : readable.opacity(0.85))
                    .lineLimit(1)
                    .padding(.top, 1)
            }

            // Start over end, stacked — a single "start–end" line ellipsizes
            // in the narrow columns.
            if height > 62 {
                VStack(alignment: .leading, spacing: 1) {
                    Text(ScheduleFormat.timeLabel(scheduleClass.startMinute))
                    if let end = scheduleClass.endMinute {
                        Text(ScheduleFormat.timeLabel(end))
                    }
                }
                .font(.system(size: compact ? 9 : 9.5, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(isNow ? .white.opacity(0.8) : readable.opacity(0.55))
                .lineLimit(1)
                .padding(.top, 2)
            }
        }
        .padding(
            compact
                ? EdgeInsets(top: 4, leading: 6, bottom: 4, trailing: 4)
                : EdgeInsets(top: 5, leading: 8, bottom: 5, trailing: 5)
        )
    }

    @ViewBuilder
    private var background: some View {
        if isNow {
            UNESColor.disciplineColor(scheduleClass.colorIndex)
        } else {
            // The design's color-mix over the surface, so blocks stay opaque
            // above the today-column tint.
            UNESColor.surface
                .overlay(readable.opacity(colorScheme == .dark ? 0.2 : 0.14))
        }
    }
}

private struct VerticalLine: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.midX, y: rect.maxY))
        return path
    }
}

#Preview {
    let overview = ScheduleOverview.preview()
    let layout = ScheduleGridLayout(days: overview.days)
    ScrollView {
        ScheduleGridDayHeader(days: overview.days, layout: layout, todayIndex: 3)
        ScheduleGridWeekCanvas(
            days: overview.days,
            layout: layout,
            todayIndex: 3,
            nowMinutes: 10 * 60 + 52
        ) { _, _ in }
    }
    .background(UNESColor.surface)
}

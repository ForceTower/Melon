import SwiftUI

/// Compact week-at-a-glance: time rows × 5 weekday columns. Tapping any class
/// cell also shifts the highlighted day column, mirroring the design's pick
/// behavior.
struct WeekMatrix: View {
    @Binding var activeIdx: Int
    var onPick: (Int) -> Void

    private let weekdayIdx: [Int] = [0, 1, 2, 3, 4]
    private let timeRailWidth: CGFloat = 44
    private let cellSpacing: CGFloat = 4

    var body: some View {
        VStack(spacing: 4) {
            headerRow
                .padding(.bottom, 4)
            ForEach(ScheduleFixtures.slots, id: \.self) { slot in
                slotRow(slot)
            }
        }
        .padding(.horizontal, 12)
        .padding(.top, 14)
        .padding(.bottom, 12)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(UNESColor.card)
                .overlay(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .strokeBorder(UNESColor.cardLine, lineWidth: 1)
                )
        )
        .shadow(color: Color.black.opacity(0.03), radius: 1, y: 1)
        .padding(.horizontal, 16)
    }

    private var headerRow: some View {
        HStack(spacing: cellSpacing) {
            Color.clear.frame(width: timeRailWidth)
            ForEach(weekdayIdx, id: \.self) { i in
                dayHeader(i).frame(maxWidth: .infinity)
            }
        }
        .padding(.leading, 2)
    }

    @ViewBuilder
    private func dayHeader(_ i: Int) -> some View {
        let isToday = i == ScheduleFixtures.todayIdx
        let isActive = i == activeIdx
        VStack(spacing: 2) {
            Text(ScheduleFixtures.daysShort[i].uppercased())
                .font(UNESFont.sans(9, weight: .semibold))
                .tracking(1.26)
                .foregroundStyle(isActive ? UNESColor.surface.opacity(0.7)
                                           : UNESColor.ink3.opacity(0.7))
            ZStack(alignment: .bottom) {
                Text("\(ScheduleFixtures.dates[i])")
                    .font(UNESFont.serif(14))
                    .tracking(-0.14)
                    .foregroundStyle(isActive ? UNESColor.surface : UNESColor.ink)
                    .padding(.bottom, 2)
                if isToday && !isActive {
                    Circle()
                        .fill(UNESColor.accent)
                        .frame(width: 3, height: 3)
                        .offset(y: 2)
                }
            }
        }
        .padding(.vertical, 4)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(isActive ? UNESColor.ink : Color.clear)
        )
        .animation(.easeInOut(duration: 0.2), value: isActive)
    }

    @ViewBuilder
    private func slotRow(_ slot: ScheduleSlot) -> some View {
        HStack(spacing: cellSpacing) {
            timeRail(slot: slot)
                .frame(width: timeRailWidth)
                .frame(maxHeight: .infinity)
                .overlay(alignment: .trailing) {
                    VerticalDashedLine().frame(width: 1)
                }
            ForEach(weekdayIdx, id: \.self) { dayIdx in
                cell(dayIdx: dayIdx, slot: slot)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .frame(minHeight: 44)
    }

    private func timeRail(slot: ScheduleSlot) -> some View {
        VStack(alignment: .trailing, spacing: 2) {
            Text(slot.start)
                .font(UNESFont.mono(9.5, weight: .semibold))
                .foregroundStyle(UNESColor.ink3)
            Text(slot.end)
                .font(UNESFont.mono(9.5, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .opacity(0.6)
        }
        .padding(.trailing, 4)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .trailing)
    }

    @ViewBuilder
    private func cell(dayIdx: Int, slot: ScheduleSlot) -> some View {
        let cls = ScheduleFixtures.week[dayIdx]
            .first { $0.start == slot.start && $0.end == slot.end }
        let isActive = dayIdx == activeIdx
        let isToday = dayIdx == ScheduleFixtures.todayIdx
        ZStack {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(isActive && cls == nil ? UNESColor.surface2 : Color.clear)
            if let cls {
                MatrixBlock(
                    cls: cls,
                    state: ScheduleFixtures.state(for: cls, isToday: isToday),
                    onPick: { onPick(dayIdx) }
                )
            }
        }
    }
}

// MARK: - Matrix cell

struct MatrixBlock: View {
    @Environment(\.colorScheme) private var scheme
    let cls: ScheduleClass
    let state: ScheduleClassState
    let onPick: () -> Void

    private var isNow:  Bool { state == .now }
    private var isDone: Bool { state == .done }

    var body: some View {
        Button(action: onPick) {
            ZStack(alignment: .topTrailing) {
                RoundedRectangle(cornerRadius: 8, style: .continuous).fill(bgColor)
                HStack(spacing: 0) {
                    Rectangle().fill(cls.color).frame(width: 3)
                    Spacer(minLength: 0)
                }
                VStack(alignment: .leading, spacing: 1) {
                    Text(cls.code)
                        .font(UNESFont.mono(9, weight: .bold))
                        .tracking(0.36)
                        .foregroundStyle(textColor)
                        .lineLimit(1)
                        .truncationMode(.tail)
                    Text(secondaryText)
                        .font(UNESFont.mono(8, weight: .medium))
                        .foregroundStyle(isNow ? Color.white.opacity(0.8) : UNESColor.ink3)
                        .lineLimit(1)
                        .truncationMode(.tail)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                .padding(.leading, 8)
                .padding(.trailing, 5)
                .padding(.vertical, 4)

                if isNow {
                    Circle()
                        .fill(UNESColor.surfaceLight)
                        .frame(width: 5, height: 5)
                        .pulseForever()
                        .padding(3)
                }

                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .strokeBorder(borderColor, lineWidth: 1)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .frame(minHeight: 42)
        }
        .buttonStyle(.plain)
        .opacity(isDone ? 0.55 : 1)
        .shadow(color: isNow ? cls.color.opacity(0.33) : .clear, radius: 7, y: 4)
    }

    private var secondaryText: String {
        if let room = cls.room { return room }
        if let modulo = cls.modulo { return modulo }
        if cls.campus == "Online" { return "online" }
        return "—"
    }

    private var textColor: Color {
        if isNow { return UNESColor.surfaceLight }
        // In dark mode lift dark disciplines (FIS2/LPOO) toward warm white so
        // the code label stays legible at 9pt.
        if scheme == .dark {
            let warm = Color(red: 0xF5 / 255, green: 0xEF / 255, blue: 0xE6 / 255)
            return cls.color.mix(with: warm, by: 0.55, in: scheme)
        }
        return cls.color
    }

    private var bgColor: Color {
        if isNow { return cls.color }
        let f = isDone ? 0.20 : 0.36
        return cls.color.mix(with: UNESColor.surface, by: f, in: scheme)
    }

    private var borderColor: Color {
        if isNow { return .clear }
        let f = isDone ? 0.45 : 0.70
        return cls.color.mix(with: UNESColor.surface, by: f, in: scheme)
    }
}

// MARK: - Dashed separator for the time rail

private struct VerticalDashedLine: View {
    var color: Color = UNESColor.line
    var body: some View {
        GeometryReader { geo in
            Path { p in
                p.move(to: .zero)
                p.addLine(to: CGPoint(x: 0, y: geo.size.height))
            }
            .stroke(color, style: StrokeStyle(lineWidth: 1, dash: [2, 2]))
        }
    }
}

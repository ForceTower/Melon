import SwiftUI

/// One weekday block: italic serif heading + stacked class cards.
struct DaySection: View {
    let dayIdx: Int

    private var classes: [ScheduleClass] { ScheduleFixtures.week[dayIdx] }
    private var isToday: Bool { dayIdx == ScheduleFixtures.todayIdx }

    var body: some View {
        if !classes.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .firstTextBaseline, spacing: 10) {
                    Text(ScheduleFixtures.daysLong[dayIdx])
                        .font(UNESFont.serif(24, italic: true))
                        .tracking(-0.24)
                        .foregroundStyle(isToday ? UNESColor.accent : UNESColor.ink)
                    Text("\(String(format: "%02d", ScheduleFixtures.dates[dayIdx])) abr")
                        .font(UNESFont.mono(10))
                        .tracking(0.8)
                        .foregroundStyle(UNESColor.ink4)
                    Spacer(minLength: 0)
                    if isToday {
                        Text("HOJE")
                            .font(UNESFont.mono(9, weight: .semibold))
                            .tracking(1.26)
                            .foregroundStyle(UNESColor.accent)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 10)

                VStack(spacing: 8) {
                    ForEach(classes) { cls in
                        DayListCard(
                            cls: cls,
                            state: ScheduleFixtures.state(for: cls, isToday: isToday)
                        )
                    }
                }
                .padding(.horizontal, 16)
            }
            .padding(.bottom, 26)
        }
    }
}

/// Compact horizontal card: color rail + time column + content.
struct DayListCard: View {
    let cls: ScheduleClass
    let state: ScheduleClassState

    private var isNow:  Bool { state == .now }
    private var isDone: Bool { state == .done }

    var body: some View {
        HStack(spacing: 0) {
            if !isNow {
                Rectangle()
                    .fill(cls.color)
                    .frame(width: 4)
                    .opacity(isDone ? 0.5 : 1)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text(cls.start)
                    .font(UNESFont.mono(11, weight: .semibold))
                    .foregroundStyle(isNow ? UNESColor.surfaceLight : UNESColor.ink)
                Text(cls.end)
                    .font(UNESFont.mono(10))
                    .foregroundStyle(isNow ? Color.white.opacity(0.65) : UNESColor.ink4)
            }
            .padding(.leading, 12)
            .padding(.vertical, 12)
            .frame(width: 58, alignment: .leading)
            .overlay(alignment: .trailing) {
                Rectangle()
                    .fill(isNow ? Color.white.opacity(0.2) : UNESColor.line)
                    .frame(width: 1)
            }

            VStack(alignment: .leading, spacing: 5) {
                HStack(spacing: 6) {
                    Text(cls.code)
                        .font(UNESFont.mono(9, weight: .semibold))
                        .tracking(1.08)
                        .padding(.horizontal, 5)
                        .padding(.vertical, 2)
                        .background(
                            RoundedRectangle(cornerRadius: 4, style: .continuous)
                                .fill(isNow ? Color.white.opacity(0.2) : cls.color.opacity(0.12))
                        )
                        .foregroundStyle(isNow ? UNESColor.surfaceLight : cls.color)
                    if isDone {
                        Image(systemName: "checkmark")
                            .font(.system(size: 9, weight: .semibold))
                            .foregroundStyle(UNESColor.ink3)
                            .opacity(0.6)
                    }
                    if isNow {
                        Spacer(minLength: 4)
                        HStack(spacing: 4) {
                            Circle()
                                .fill(UNESColor.surfaceLight)
                                .frame(width: 4, height: 4)
                                .pulseForever()
                            Text("AGORA")
                                .font(UNESFont.mono(9, weight: .semibold))
                                .tracking(1.26)
                                .foregroundStyle(UNESColor.surfaceLight)
                        }
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .fill(Color.white.opacity(0.22))
                        )
                    }
                }

                Text(cls.title)
                    .font(UNESFont.serif(16))
                    .tracking(-0.16)
                    .foregroundStyle(isNow ? UNESColor.surfaceLight : UNESColor.ink)
                    .fixedSize(horizontal: false, vertical: true)

                ScheduleLocationRow(
                    cls: cls,
                    style: isNow ? .inverted : .normal(dim: isDone)
                )
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(isNow ? cls.color : UNESColor.card)
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(isNow ? Color.clear : UNESColor.cardLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .opacity(isDone ? 0.55 : 1)
        .shadow(
            color: isNow ? cls.color.opacity(0.2) : Color.black.opacity(0.03),
            radius: isNow ? 12 : 1,
            y: isNow ? 8 : 1
        )
    }
}

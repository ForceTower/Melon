import SwiftUI

struct TodayTimeline: View {
    let items: [OverviewTodayItem]

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .firstTextBaseline) {
                Text("Seu dia")
                    .font(UNESFont.serif(22))
                    .tracking(-0.22)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                Spacer()
                Button {
                    // navigate to full week
                } label: {
                    HStack(spacing: 4) {
                        Text("semana")
                            .font(UNESFont.sans(12, weight: .medium))
                        Image(systemName: "chevron.right")
                            .font(.system(size: 8, weight: .semibold))
                    }
                    .foregroundStyle(UNESColor.ink3)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
            .padding(.bottom, 8)

            VStack(spacing: 0) {
                ForEach(Array(items.enumerated()), id: \.element.id) { i, item in
                    TodayRow(item: item, isLast: i == items.count - 1)
                }
            }
            .padding(.vertical, 4)
        }
        .padding(.bottom, 6)
        .cardSurface(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

private struct TodayRow: View {
    let item: OverviewTodayItem
    let isLast: Bool

    private var isDone: Bool { item.state == .done }
    private var isNow: Bool { item.state == .now }

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            // Time + rail column
            ZStack(alignment: .topLeading) {
                Text(item.time)
                    .font(UNESFont.mono(11, weight: isNow ? .semibold : .regular))
                    .tracking(0.22)
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.top, 12)

                if !isLast {
                    Rectangle()
                        .fill(UNESColor.line)
                        .frame(width: 1)
                        .padding(.leading, 22)
                        .padding(.top, 32)
                        .padding(.bottom, -8)
                }

                TimelineDot(color: item.color, isNow: isNow, isDone: isDone)
                    .padding(.leading, 18)
                    .padding(.top, 30)
            }
            .frame(width: 44, alignment: .topLeading)

            // Content
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text(item.code)
                        .font(UNESFont.mono(10, weight: .semibold))
                        .tracking(1)
                        .foregroundStyle(item.color)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(
                            RoundedRectangle(cornerRadius: 6, style: .continuous)
                                .fill(item.color.opacity(0.1))
                        )
                    if isNow {
                        HStack(spacing: 4) {
                            Circle()
                                .fill(UNESColor.accent)
                                .frame(width: 4, height: 4)
                                .pulseForever()
                            Text("AGORA")
                                .font(UNESFont.mono(9, weight: .semibold))
                                .tracking(1.08)
                                .foregroundStyle(UNESColor.accent)
                        }
                    }
                    Spacer(minLength: 0)
                }

                Text(item.title)
                    .font(UNESFont.sans(15, weight: .medium))
                    .tracking(-0.075)
                    .foregroundStyle(UNESColor.ink)

                if let topic = item.topic {
                    Text("“\(topic)”")
                        .font(UNESFont.sans(12))
                        .italic()
                        .foregroundStyle(UNESColor.ink3)
                } else {
                    Text("sala \(item.room)")
                        .font(UNESFont.sans(12))
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            .padding(.horizontal, 12)
            .padding(.top, 10)
            .padding(.bottom, 12)
        }
        .padding(.horizontal, 16)
        .opacity(isDone ? 0.5 : 1)
    }
}

private struct TimelineDot: View {
    let color: Color
    let isNow: Bool
    let isDone: Bool

    var body: some View {
        ZStack {
            Circle()
                .fill(fillColor)
                .overlay(
                    Circle().strokeBorder(strokeColor, lineWidth: 2)
                )
                .frame(width: 9, height: 9)
                .background(
                    Group {
                        if isNow {
                            Circle()
                                .fill(color.opacity(0.13))
                                .frame(width: 17, height: 17)
                        }
                    }
                )
            if isDone {
                Image(systemName: "checkmark")
                    .font(.system(size: 5, weight: .bold))
                    .foregroundStyle(Color.white)
            }
        }
        .frame(width: 17, height: 17)
    }

    private var fillColor: Color {
        if isNow { return color }
        if isDone { return UNESColor.ink4 }
        return UNESColor.surface3
    }

    private var strokeColor: Color {
        isNow ? color : UNESColor.surface
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        TodayTimeline(items: OverviewFixtures.today).padding(14)
    }
}

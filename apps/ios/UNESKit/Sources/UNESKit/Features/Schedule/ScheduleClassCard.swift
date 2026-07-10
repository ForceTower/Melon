import SwiftUI

/// One timeline row: the start/end time rail on the left and the tappable
/// class card. The card fills with the discipline color while the class is
/// in progress; finished classes fade out.
struct ScheduleClassRow: View {
    let cls: ScheduleClass
    let state: ScheduleClassState
    var onTap: () -> Void

    private var isNow: Bool { state == .now }
    private var isNext: Bool { state == .next }
    private var isDone: Bool { state == .done }
    private var readable: Color { UNESColor.disciplineReadableColor(cls.colorIndex) }

    var body: some View {
        HStack(alignment: .top, spacing: 11) {
            timeRail

            Button(action: onTap) {
                card
            }
            .buttonStyle(.pressableCard)
        }
        .opacity(isDone ? 0.55 : 1)
        .animation(.easeOut(duration: 0.25), value: isDone)
    }

    // MARK: Time rail

    private var timeRail: some View {
        VStack(alignment: .trailing, spacing: 2) {
            Text(ScheduleFormat.timeLabel(cls.startMinute))
                .font(.system(size: 14, weight: isNow || isNext ? .bold : .semibold))
                .tracking(-0.28)
                .monospacedDigit()
                .foregroundStyle(isNow || isNext ? UNESColor.ink : UNESColor.ink2)
            if let end = cls.endMinute {
                Text(ScheduleFormat.timeLabel(end))
                    .font(.system(size: 12, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .frame(width: 44, alignment: .trailing)
        .padding(.top, 16)
    }

    // MARK: Card

    private var card: some View {
        VStack(alignment: .leading, spacing: 0) {
            header

            Text(cls.title)
                .font(.system(size: 18, weight: .bold))
                .tracking(-0.45)
                .lineSpacing(1)
                .multilineTextAlignment(.leading)
                .foregroundStyle(isNow ? .white : UNESColor.ink)
                .padding(.bottom, cls.topic != nil ? 4 : 10)

            if let topic = cls.topic {
                HStack(spacing: 6) {
                    Image(systemName: "text.alignleft")
                        .font(.system(size: 10, weight: .medium))
                        .opacity(0.7)
                    Text(topic)
                        .font(.system(size: 13, weight: .medium))
                        .lineLimit(1)
                }
                .foregroundStyle(isNow ? .white.opacity(0.86) : UNESColor.ink3)
                .padding(.bottom, 10)
            }

            footer
        }
        .padding(EdgeInsets(top: 16, leading: 16, bottom: 15, trailing: 16))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background { cardBackground }
        .overlay(alignment: .leading) {
            if !isNow {
                RoundedRectangle(cornerRadius: 3)
                    .fill(readable)
                    .opacity(isDone ? 0.5 : 1)
                    .frame(width: 4)
                    .padding(.vertical, 14)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(isNow ? .clear : UNESColor.cardLine)
        }
        .shadow(
            color: isNow
                ? UNESColor.disciplineColor(cls.colorIndex).opacity(0.13)
                : Color(hex: 0x141020, opacity: 0.05),
            radius: isNow ? 9 : 8,
            y: 6
        )
        .contentShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    @ViewBuilder
    private var cardBackground: some View {
        if isNow {
            ZStack {
                UNESColor.disciplineColor(cls.colorIndex)
                MeshView(variant: .cool, intensity: 0.9)
                    .opacity(0.5)
            }
        } else {
            UNESColor.card
        }
    }

    private var header: some View {
        HStack(spacing: 8) {
            Text(cls.code)
                .font(.system(size: 10.5, weight: .bold))
                .tracking(0.3)
                .foregroundStyle(isNow ? .white : readable)
                .padding(EdgeInsets(top: 3, leading: 7, bottom: 3, trailing: 7))
                .background(
                    isNow ? Color.white.opacity(0.2) : readable.opacity(0.12),
                    in: RoundedRectangle(cornerRadius: 6, style: .continuous)
                )

            if isDone {
                Image(systemName: "checkmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                    .opacity(0.6)
            }
            if isNow {
                agoraPill
            }
            if isNext {
                Text(.scheduleClassRowNext)
                    .font(.system(size: 10, weight: .bold))
                    .tracking(0.3)
                    .foregroundStyle(.white)
                    .padding(EdgeInsets(top: 2, leading: 7, bottom: 2, trailing: 7))
                    .background(UNESColor.accent, in: RoundedRectangle(cornerRadius: 6, style: .continuous))
            }

            Spacer(minLength: 8)

            if let duration = cls.durationMinutes {
                Text(ScheduleFormat.durationLabel(duration))
                    .font(.system(size: 12, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(isNow ? .white.opacity(0.75) : UNESColor.ink4)
            }
        }
        .padding(.bottom, 7)
    }

    private var agoraPill: some View {
        HStack(spacing: 5) {
            LiveDot(size: 5, color: .white)
            Text(.commonNow)
                .textCase(.uppercase)
                .font(.system(size: 10.5, weight: .bold))
                .tracking(0.42)
        }
        .foregroundStyle(.white)
        .padding(EdgeInsets(top: 3, leading: 8, bottom: 3, trailing: 8))
        .background(.white.opacity(0.22), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private var footer: some View {
        HStack(alignment: .top, spacing: 10) {
            ScheduleLocationRow(cls: cls, inverted: isNow, dim: isDone)
                .frame(maxWidth: .infinity, alignment: .leading)

            if let teacher = cls.teacherName {
                Text(ScheduleFormat.shortTeacherName(teacher))
                    .font(.system(size: 12.5, weight: .medium))
                    .lineLimit(1)
                    .foregroundStyle(isNow ? .white.opacity(0.78) : UNESColor.ink3)
                    .frame(maxWidth: 108, alignment: .trailing)
            }
        }
    }
}

#Preview {
    let classes = ScheduleOverview.preview().days[3].classes
    ScrollView {
        VStack(spacing: 4) {
            ScheduleClassRow(cls: classes[0], state: .done) {}
            ScheduleClassRow(cls: classes[1], state: .now) {}
            ScheduleClassRow(cls: classes[2], state: .next) {}
            ScheduleClassRow(cls: classes[3], state: .later) {}
        }
        .padding(EdgeInsets(top: 20, leading: 12, bottom: 20, trailing: 16))
    }
    .background(UNESColor.surface)
}

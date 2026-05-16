import SwiftUI

/// Vertical "day timeline" card used by the focused-day schedule variant: time
/// rail on the left + rich class card with code chip, serif title, optional
/// topic and location footer. The `now` state fills the card with the
/// discipline color; `done` dims it.
struct FocusedClassBlock: View {
    let cls: ScheduleClass
    let state: ScheduleClassState
    var showGap: Bool = false
    var gapMin: Int = 0

    private var isNow:  Bool { state == .now }
    private var isDone: Bool { state == .done }

    var body: some View {
        VStack(spacing: 0) {
            if showGap && gapMin > 30 {
                gapMarker
            }
            classRow
        }
    }

    // MARK: - Gap marker

    private var gapMarker: some View {
        HStack(spacing: 10) {
            Rectangle().fill(UNESColor.line).frame(height: 1)
            Text(gapLabel)
                .font(UNESFont.mono(10))
                .tracking(1)
                .foregroundStyle(UNESColor.ink4)
                .opacity(0.7)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
            Rectangle().fill(UNESColor.line).frame(height: 1)
        }
        .padding(.leading, 60)
        .padding(.trailing, 6)
        .padding(.vertical, 4)
    }

    private var gapLabel: String {
        let base: String
        let hours = gapMin / 60
        let minutes = gapMin % 60
        if hours > 0 {
            base = minutes > 0 ? "\(hours)h\(String(format: "%02d", minutes))" : "\(hours)h"
        } else {
            base = "\(minutes)min"
        }
        return "\(base) de intervalo".uppercased()
    }

    // MARK: - Row

    private var classRow: some View {
        HStack(alignment: .top, spacing: 10) {
            timeRail
            card
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 2)
        .opacity(isDone ? 0.52 : 1)
        .animation(.easeOut(duration: 0.25), value: isDone)
    }

    private var timeRail: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(cls.start)
                .font(UNESFont.mono(11, weight: .semibold))
                .foregroundStyle(isNow ? UNESColor.ink : UNESColor.ink2)
            Text(cls.end)
                .font(UNESFont.mono(10))
                .foregroundStyle(UNESColor.ink3)
                .opacity(0.55)
        }
        .frame(width: 50, alignment: .leading)
        .padding(.top, 14)
    }

    private var card: some View {
        ZStack(alignment: .topLeading) {
            // color accent rail when not "now"
            if !isNow {
                Rectangle()
                    .fill(cls.color)
                    .frame(width: 3)
                    .cornerRadius(2)
                    .padding(.vertical, 14)
                    .opacity(isDone ? 0.45 : 1)
            }

            VStack(alignment: .leading, spacing: 0) {
                headerRow
                    .padding(.bottom, 6)
                Text(cls.title)
                    .font(UNESFont.serif(19))
                    .tracking(-0.19)
                    .lineSpacing(-1)
                    .foregroundStyle(isNow ? UNESColor.surfaceLight : UNESColor.ink)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.bottom, cls.topic != nil ? 3 : 9)
                if let topic = cls.topic {
                    topicRow(topic)
                        .padding(.bottom, 9)
                }
                footerRow
                    .padding(.top, 2)
            }
            .padding(.vertical, 14)
            .padding(.leading, isNow ? 14 : 18)
            .padding(.trailing, 14)
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
        // Same NOW-vs-idle pattern as `DayListCard`: vibrant discipline color
        // when now, neutral card tint otherwise. On iOS 26+ both become
        // Liquid Glass, with the colored tint preserving the NOW state's pop.
        .cardSurface(
            RoundedRectangle(cornerRadius: 20, style: .continuous),
            fill: isNow ? cls.color : UNESColor.card,
            stroke: isNow ? Color.clear : UNESColor.cardLine
        )
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(
            color: isNow ? cls.color.opacity(0.2) : Color.black.opacity(0.03),
            radius: isNow ? 14 : 1,
            y: isNow ? 10 : 1
        )
    }

    // MARK: - Parts

    private var headerRow: some View {
        HStack(alignment: .center, spacing: 8) {
            Text(cls.code)
                .font(UNESFont.mono(9.5, weight: .semibold))
                .tracking(1.14)
                .padding(.horizontal, 6)
                .padding(.vertical, 3)
                .background(
                    RoundedRectangle(cornerRadius: 5, style: .continuous)
                        .fill(isNow ? Color.white.opacity(0.18) : cls.color.opacity(0.09))
                )
                .foregroundStyle(isNow ? UNESColor.surfaceLight : cls.color)

            if isDone {
                Image(systemName: "checkmark")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                    .opacity(0.6)
            }

            if isNow {
                Spacer(minLength: 4)
                HStack(spacing: 5) {
                    Circle()
                        .fill(UNESColor.surfaceLight)
                        .frame(width: 5, height: 5)
                        .pulseForever()
                    Text("AGORA")
                        .font(UNESFont.mono(9.5, weight: .semibold))
                        .tracking(1.33)
                        .foregroundStyle(UNESColor.surfaceLight)
                }
                .padding(.horizontal, 7)
                .padding(.vertical, 3)
                .background(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(Color.white.opacity(0.22))
                )
            } else {
                Spacer(minLength: 4)
            }

            Text("\(cls.durationMin)min")
                .font(UNESFont.mono(10))
                .foregroundStyle(isNow ? Color.white.opacity(0.75) : UNESColor.ink4)
        }
    }

    private func topicRow(_ topic: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: "text.alignleft")
                .font(.system(size: 9, weight: .regular))
                .foregroundStyle(isNow ? Color.white.opacity(0.85) : UNESColor.ink3)
                .opacity(0.7)
            Text("“\(topic)”")
                .font(UNESFont.sans(12).italic())
                .foregroundStyle(isNow ? Color.white.opacity(0.85) : UNESColor.ink3)
                .lineLimit(1)
                .truncationMode(.tail)
        }
    }

    private var footerRow: some View {
        HStack(alignment: .top, spacing: 10) {
            ScheduleLocationRow(cls: cls, style: isNow ? .inverted : .normal(dim: isDone))
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(cls.prof)
                .font(UNESFont.sans(11))
                .foregroundStyle(isNow ? Color.white.opacity(0.75) : UNESColor.ink3)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: 110, alignment: .trailing)
        }
    }
}

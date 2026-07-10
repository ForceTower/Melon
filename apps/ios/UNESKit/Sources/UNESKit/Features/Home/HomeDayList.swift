import SwiftUI

/// "Seu dia" — the grouped list of today's sessions with a live current-time
/// line threaded between rows.
struct HomeDaySection: View {
    let today: [TodayClass]
    var onSeeSchedule: () -> Void
    var onOpenClass: (TodayClass) -> Void

    var body: some View {
        VStack(spacing: 0) {
            HomeSectionHeader(title: .homeSectionYourDay, action: .homeActionSeeSchedule, onAction: onSeeSchedule)

            TimelineView(.everyMinute) { context in
                card(now: context.date)
            }
        }
    }

    private func card(now: Date) -> some View {
        let calendar = Calendar.current
        let nowMinutes = calendar.component(.hour, from: now) * 60 + calendar.component(.minute, from: now)
        // The live line slots in before the first session yet to start.
        let nowIndex = today.firstIndex { $0.startMinute > nowMinutes } ?? today.count

        return VStack(spacing: 0) {
            if today.isEmpty {
                Text(.homeDayEmpty)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
            } else {
                ForEach(Array(today.enumerated()), id: \.element.id) { index, item in
                    if index == nowIndex {
                        HomeNowLine(minutes: nowMinutes, position: index == 0 ? .cardTop : .betweenRows)
                    }
                    HomeDayRow(
                        item: item,
                        isDone: (item.endMinute ?? item.startMinute) <= nowMinutes,
                        isNext: index == nowIndex,
                        showsDivider: index < today.count - 1 || nowIndex == today.count,
                        onOpen: { onOpenClass(item) }
                    )
                }
                if nowIndex == today.count {
                    HomeNowLine(minutes: nowMinutes, position: .cardBottom)
                }
            }
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

/// Shared "Seu dia" / "Turmas" section heading with a trailing accent action.
struct HomeSectionHeader: View {
    var title: LocalizedStringResource
    var action: LocalizedStringResource?
    var onAction: () -> Void = {}

    var body: some View {
        HStack(alignment: .lastTextBaseline) {
            Text(title)
                .font(.system(size: 22, weight: .bold))
                .tracking(-0.66)
                .foregroundStyle(UNESColor.ink)

            Spacer()

            if let action {
                Button(action: onAction) {
                    Text(action)
                        .font(.system(size: 15, weight: .medium))
                        .tracking(-0.15)
                        .foregroundStyle(UNESColor.accent)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(EdgeInsets(top: 0, leading: 4, bottom: 12, trailing: 4))
    }
}

private struct HomeDayRow: View {
    let item: TodayClass
    let isDone: Bool
    let isNext: Bool
    let showsDivider: Bool
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(spacing: 12) {
                Text(item.startTime)
                    .font(.system(size: 13, weight: isNext ? .bold : .medium))
                    .monospacedDigit()
                    .foregroundStyle(isNext ? UNESColor.ink : UNESColor.ink3)
                    .frame(width: 44, alignment: .trailing)

                RoundedRectangle(cornerRadius: 2)
                    .fill(UNESColor.disciplineColor(item.colorIndex))
                    .frame(width: 4, height: 34)

                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 7) {
                        Text(item.title)
                            .font(.system(size: 16, weight: .semibold))
                            .tracking(-0.32)
                            .strikethrough(isDone)
                            .foregroundStyle(UNESColor.ink)
                            .lineLimit(1)
                        if isNext {
                            Text(.homeDayNextBadge)
                                .font(.system(size: 10, weight: .bold))
                                .tracking(0.3)
                                .foregroundStyle(.white)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(UNESColor.accent, in: RoundedRectangle(cornerRadius: 6))
                        }
                    }
                    Text(subtitle)
                        .font(.system(size: 13.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .lineLimit(1)
                }
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(.leading, 14)
            .padding(.trailing, 12)
            .frame(minHeight: 64)
            .opacity(isDone ? 0.5 : 1)
            .contentShape(Rectangle())
            .overlay(alignment: .bottom) {
                if showsDivider {
                    Rectangle()
                        .fill(UNESColor.line)
                        .frame(height: 0.5)
                        .padding(.leading, 74)
                }
            }
        }
        .buttonStyle(HomeRowButtonStyle())
    }

    private var subtitle: String {
        if let topic = item.topic { return topic }
        return [item.code, item.room.map { String.localized(.commonRoom($0)) }]
            .compactMap(\.self)
            .joined(separator: " · ")
    }
}

/// Rows highlight like table cells while pressed.
struct HomeRowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? UNESColor.surface2 : .clear)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

/// The red "now" indicator: time label, glowing dot, and a hairline across
/// the card — zero-height so it floats between rows. At the card's edges the
/// zero-height trick would let the rounded-corner clip cut it in half, so
/// those positions reserve room on the clipped side.
private struct HomeNowLine: View {
    enum Position {
        case cardTop, betweenRows, cardBottom
    }

    let minutes: Int
    var position: Position = .betweenRows

    var body: some View {
        HStack(spacing: 10) {
            Text(HomeFormat.nowLabel(minutes: minutes))
                .font(.system(size: 11, weight: .bold))
                .monospacedDigit()
                .tracking(-0.11)
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
        .padding(.leading, 12)
        .padding(.trailing, 14)
        .frame(height: 0)
        .padding(.top, position == .cardTop ? 14 : 0)
        .padding(.bottom, position == .cardBottom ? 14 : 0)
        .zIndex(2)
    }
}

#Preview {
    NavigationStack {
        ScrollView {
            HomeDaySection(today: HomeOverview.preview().today, onSeeSchedule: {}, onOpenClass: { _ in })
                .padding(16)
        }
        .background(UNESColor.surface)
    }
}

#Preview("Empty") {
    HomeDaySection(today: [], onSeeSchedule: {}, onOpenClass: { _ in })
        .padding(16)
        .background(UNESColor.surface)
}

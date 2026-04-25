import SwiftUI

/// Single row in the agenda variant: a date gutter (giant day number, weekday)
/// connected to a card by a 1pt vertical rail. Multi-day events show their
/// range under the weekday and italicize the day number.
///
/// Mirrors `CalAgendaRow` in `screens-calendar.jsx`.
struct CalAgendaRow: View {
    let event: CalendarEvent
    let isLast: Bool

    private var category: CalendarCategory { CalendarMath.categorize(event) }
    private var status: CalendarStatus { CalendarMath.status(event) }
    private var isPast: Bool { status == .past }
    private var isActive: Bool { status == .active }
    private var hasRange: Bool { event.end != nil }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            dateGutter
            card
        }
        .padding(.vertical, 2)
        .opacity(isPast ? 0.45 : 1)
    }

    // MARK: - Date gutter

    private var dateGutter: some View {
        let cal = Calendar.current
        let day = cal.component(.day, from: event.start)
        return ZStack(alignment: .top) {
            // Connecting rail. Drawn first so the date label sits over it.
            if !isLast {
                Rectangle()
                    .fill(UNESColor.line)
                    .frame(width: 1)
                    .padding(.top, 44)
                    .frame(maxHeight: .infinity, alignment: .top)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text(String(format: "%02d", day))
                    .font(UNESFont.serif(22, italic: hasRange))
                    .tracking(-0.44)
                    .foregroundStyle(isActive ? category.color : UNESColor.ink)

                weekdayLabel
            }
            .padding(.top, 14)
        }
        .frame(width: 60, alignment: .topLeading)
    }

    private var weekdayLabel: some View {
        let cal = Calendar.current
        let startWeekday = CalendarFormat.weekday(event.start)
        if let end = event.end {
            let endDay = cal.component(.day, from: end)
            let endMonth = cal.component(.month, from: end) - 1
            let startMonth = cal.component(.month, from: event.start) - 1
            let trail: String
            if endMonth != startMonth {
                trail = String(format: " – %02d %@", endDay, CalendarFormat.monthsShort[endMonth])
            } else {
                trail = String(format: " – %02d", endDay)
            }
            return AnyView(
                HStack(spacing: 0) {
                    Text(startWeekday.uppercased())
                        .font(UNESFont.mono(9.5))
                        .tracking(0.95)
                        .foregroundStyle(UNESColor.ink3)
                    Text(trail)
                        .font(UNESFont.mono(9.5))
                        .tracking(0.95)
                        .foregroundStyle(UNESColor.ink4)
                }
            )
        }
        return AnyView(
            Text(startWeekday.uppercased())
                .font(UNESFont.mono(9.5))
                .tracking(0.95)
                .foregroundStyle(UNESColor.ink3)
        )
    }

    // MARK: - Card

    private var card: some View {
        ZStack(alignment: .leading) {
            // Accent rail
            RoundedRectangle(cornerRadius: 2, style: .continuous)
                .fill(category.color.opacity(isPast ? 0.4 : 1))
                .frame(width: 3)
                .padding(.vertical, 14)

            VStack(alignment: .leading, spacing: 5) {
                metaRow
                titleText
                if event.fixed {
                    annualBadge
                }
            }
            .padding(.leading, 8)
            .padding(.vertical, 12)
            .padding(.trailing, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.bottom, 10)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(UNESColor.card)
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .strokeBorder(UNESColor.cardLine, lineWidth: 1)
                )
                .padding(.bottom, 10)
        )
    }

    private var metaRow: some View {
        HStack(spacing: 8) {
            HStack(spacing: 5) {
                CalCategoryGlyph(category: category, color: category.color, size: 11)
                Text(category.label.uppercased())
                    .font(UNESFont.mono(9.5, weight: .semibold))
                    .tracking(1.14)
                    .foregroundStyle(category.color)
            }
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .fill(category.color.opacity(0.10))
            )

            Text("· \(event.scope.label.uppercased())")
                .font(UNESFont.mono(9.5))
                .tracking(0.95)
                .foregroundStyle(UNESColor.ink4)

            if isActive && !event.closed {
                Spacer(minLength: 0)
                openBadge
            } else {
                Spacer(minLength: 0)
            }
        }
    }

    private var openBadge: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(UNESColor.accent)
                .frame(width: 4, height: 4)
                .pulseForever()
            Text("ABERTO")
                .font(UNESFont.mono(9, weight: .semibold))
                .tracking(1.26)
                .foregroundStyle(UNESColor.accent)
        }
    }

    private var titleText: some View {
        Text(event.displayDescription)
            .font(UNESFont.serif(17))
            .tracking(-0.17)
            .lineSpacing(2)
            .foregroundStyle(UNESColor.ink)
            .fixedSize(horizontal: false, vertical: true)
    }

    private var annualBadge: some View {
        HStack(spacing: 5) {
            Image(systemName: "plus")
                .font(.system(size: 9, weight: .medium))
            Text("ANUAL")
                .font(UNESFont.mono(9.5))
                .tracking(0.95)
        }
        .foregroundStyle(UNESColor.ink4)
        .padding(.top, 1)
    }
}

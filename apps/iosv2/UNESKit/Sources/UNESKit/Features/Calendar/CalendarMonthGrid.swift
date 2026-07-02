import SwiftUI

#if canImport(UIKit)
import UIKit
#endif

/// The grid body: the native month calendar in a card — decorated with
/// category dots — and whatever the focused day holds underneath.
struct CalendarMonthGridSection: View {
    let events: [CalendarEvent]
    let selectedDay: Date
    let today: Date
    var onSelectDay: (Date) -> Void
    var onOpen: (CalendarEvent) -> Void

    private var selectedDayEvents: [CalendarEvent] {
        CalendarMath.events(on: selectedDay, in: events)
    }

    private var isTodaySelected: Bool {
        Calendar.current.isDate(selectedDay, inSameDayAs: today)
    }

    var body: some View {
        VStack(spacing: 0) {
            monthCard
                .padding(.bottom, 16)

            HStack(alignment: .lastTextBaseline, spacing: 8) {
                Text(isTodaySelected ? String.localized(.commonToday) : CalendarFormat.dayTitle(selectedDay))
                    .font(.system(size: 19, weight: .bold))
                    .tracking(-0.38)
                    .foregroundStyle(UNESColor.ink)
                Text(CalendarFormat.weekday(selectedDay))
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                Spacer()
            }
            .padding(EdgeInsets(top: 2, leading: 6, bottom: 12, trailing: 6))

            if selectedDayEvents.isEmpty {
                emptyDayCard
            } else {
                ForEach(selectedDayEvents) { event in
                    CalendarEventRow(event: event, today: today, onOpen: onOpen)
                        .padding(.bottom, 9)
                }
            }
        }
        .padding(.horizontal, 16)
    }

    private var monthCard: some View {
        CalendarMonthView(events: events, selectedDay: selectedDay, onSelectDay: onSelectDay)
            .padding(EdgeInsets(top: 6, leading: 12, bottom: 8, trailing: 12))
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }

    private var emptyDayCard: some View {
        Text(.calendarGridEmptyDay)
            .font(.system(size: 13, weight: .medium))
            .foregroundStyle(UNESColor.ink4)
            .frame(maxWidth: .infinity)
            .padding(20)
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
    }
}

#if canImport(UIKit)

/// The system month calendar, decorated with up to three category dots per
/// day and driving single-day selection.
private struct CalendarMonthView: UIViewRepresentable {
    let events: [CalendarEvent]
    let selectedDay: Date
    var onSelectDay: (Date) -> Void

    func makeUIView(context: Context) -> UICalendarView {
        let view = UICalendarView()
        view.calendar = Calendar.current
        view.locale = Locale.autoupdatingCurrent
        view.tintColor = UIColor(UNESColor.accent)
        view.backgroundColor = .clear
        view.delegate = context.coordinator

        let selection = UICalendarSelectionSingleDate(delegate: context.coordinator)
        view.selectionBehavior = selection
        selection.setSelected(dayComponents(of: selectedDay), animated: false)
        view.visibleDateComponents = dayComponents(of: selectedDay)

        context.coordinator.categoriesByDay = Self.categoriesByDay(for: events)
        return view
    }

    func updateUIView(_ view: UICalendarView, context: Context) {
        let coordinator = context.coordinator
        coordinator.onSelectDay = onSelectDay

        if let selection = view.selectionBehavior as? UICalendarSelectionSingleDate {
            let current = selection.selectedDate.flatMap { Calendar.current.date(from: $0) }
            let isCurrent = current.map { Calendar.current.isDate($0, inSameDayAs: selectedDay) } ?? false
            if !isCurrent {
                selection.setSelected(dayComponents(of: selectedDay), animated: false)
            }
        }

        let fresh = Self.categoriesByDay(for: events)
        guard fresh != coordinator.categoriesByDay else { return }
        let stale = Set(coordinator.categoriesByDay.keys).union(fresh.keys)
        coordinator.categoriesByDay = fresh
        view.reloadDecorations(forDateComponents: Array(stale), animated: true)
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UICalendarView, context: Context) -> CGSize? {
        let width = proposal.width ?? uiView.intrinsicContentSize.width
        let fitting = uiView.systemLayoutSizeFitting(
            CGSize(width: width, height: UIView.layoutFittingCompressedSize.height),
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .fittingSizeLevel
        )
        return CGSize(width: width, height: fitting.height)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(onSelectDay: onSelectDay)
    }

    private func dayComponents(of date: Date) -> DateComponents {
        Calendar.current.dateComponents([.year, .month, .day], from: date)
    }

    /// Distinct categories per day, insertion-ordered, capped at three dots.
    static func categoriesByDay(for events: [CalendarEvent]) -> [DateComponents: [CalendarCategory]] {
        let calendar = Calendar.current
        var result: [DateComponents: [CalendarCategory]] = [:]
        for event in events {
            var day = event.start
            while day <= event.endOrStart {
                let key = calendar.dateComponents([.year, .month, .day], from: day)
                if result[key, default: []].contains(event.category) == false, result[key, default: []].count < 3 {
                    result[key, default: []].append(event.category)
                }
                guard let next = calendar.date(byAdding: .day, value: 1, to: day) else { break }
                day = next
            }
        }
        return result
    }

    final class Coordinator: NSObject, UICalendarViewDelegate, UICalendarSelectionSingleDateDelegate {
        var categoriesByDay: [DateComponents: [CalendarCategory]] = [:]
        var onSelectDay: (Date) -> Void

        init(onSelectDay: @escaping (Date) -> Void) {
            self.onSelectDay = onSelectDay
        }

        func calendarView(
            _ calendarView: UICalendarView,
            decorationFor dateComponents: DateComponents
        ) -> UICalendarView.Decoration? {
            let key = DateComponents(
                year: dateComponents.year,
                month: dateComponents.month,
                day: dateComponents.day
            )
            guard let categories = categoriesByDay[key], !categories.isEmpty else { return nil }
            return .customView {
                let stack = UIStackView()
                stack.axis = .horizontal
                stack.spacing = 2.5
                stack.alignment = .center
                for category in categories {
                    let dot = UIView()
                    dot.backgroundColor = UIColor(category.color)
                    dot.layer.cornerRadius = 2.5
                    dot.translatesAutoresizingMaskIntoConstraints = false
                    dot.widthAnchor.constraint(equalToConstant: 5).isActive = true
                    dot.heightAnchor.constraint(equalToConstant: 5).isActive = true
                    stack.addArrangedSubview(dot)
                }
                return stack
            }
        }

        func dateSelection(_ selection: UICalendarSelectionSingleDate, didSelectDate dateComponents: DateComponents?) {
            guard let dateComponents, let date = Calendar.current.date(from: dateComponents) else { return }
            onSelectDay(date)
        }
    }
}

#else

/// The package also builds for macOS so `swift test` runs on the host;
/// the grid is iOS-only chrome there.
private struct CalendarMonthView: View {
    let events: [CalendarEvent]
    let selectedDay: Date
    var onSelectDay: (Date) -> Void

    var body: some View {
        EmptyView()
    }
}

#endif

#Preview {
    ScrollView {
        CalendarMonthGridSection(
            events: .preview(),
            selectedDay: .now,
            today: .now,
            onSelectDay: { _ in },
            onOpen: { _ in }
        )
        .padding(.vertical, 16)
    }
    .background(UNESColor.surface)
}

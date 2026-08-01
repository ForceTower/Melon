import SwiftUI

#if canImport(EventKitUI)
import EventKitUI
#endif

/// Event detail: identity header, display-size countdown, the meta grid and
/// a handoff into the system calendar.
struct CalendarEventSheet: View {
    let event: CalendarEvent
    let today: Date
    var onClose: () -> Void
    var onAddToCalendar: () -> Void = {}
    var onEdit: (PersonalEvent) -> Void = { _ in }
    var onDelete: (PersonalEvent) -> Void = { _ in }

    @State private var isCalendarEditPresented = false

    private var category: CalendarCategory { event.category }
    private var status: CalendarStatus { CalendarMath.status(event, today: today) }
    private var countdown: CalendarCountdown { CalendarMath.countdown(event, today: today) }
    private var range: String { CalendarFormat.dateRange(start: event.start, end: event.end) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header
                countdownStrip
                    .padding(.top, 16)
                metaGrid
                    .padding(.top, 10)
                if let notes = event.personal?.notes, !notes.isEmpty {
                    notesCard(notes)
                        .padding(.top, 10)
                }
                if let discipline = event.personal?.discipline {
                    disciplineCard(discipline)
                        .padding(.top, 10)
                }
                if event.fixed {
                    fixedChip
                        .padding(.top, 10)
                }
                actions
                    .padding(.top, 18)
            }
            .padding(EdgeInsets(top: 24, leading: 18, bottom: 24, trailing: 18))
        }
        .presentationBackground(UNESColor.surface)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        #if canImport(EventKitUI)
        .sheet(isPresented: $isCalendarEditPresented) {
            CalendarEventEditor(event: event) {
                isCalendarEditPresented = false
            }
            .ignoresSafeArea()
        }
        #endif
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 12) {
            Image(systemName: category.icon)
                .font(.system(size: 19, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 42, height: 42)
                .background(category.color, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                .shadow(color: category.color.opacity(0.33), radius: 7, y: 6)

            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: "\(category.label) · \(event.provenanceLabel)")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(category.color)
                Text(event.title)
                    .font(.system(size: 20, weight: .bold))
                    .tracking(-0.6)
                    .lineSpacing(1)
                    .foregroundStyle(UNESColor.ink)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(width: 30, height: 30)
                    .background(UNESColor.surface2, in: Circle())
            }
            .buttonStyle(.plain)
        }
    }

    private var countdownStrip: some View {
        HStack(alignment: .lastTextBaseline, spacing: 9) {
            Text(countdown.number)
                .font(.system(size: 40, weight: .bold))
                .tracking(-1.6)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink)
            if !countdown.tail.isEmpty {
                Text(countdown.tail)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(category.color)
            }
            Spacer(minLength: 12)
            Text(range)
                .font(.system(size: 12, weight: .medium))
                .monospacedDigit()
                .multilineTextAlignment(.trailing)
                .foregroundStyle(UNESColor.ink4)
                .frame(maxWidth: 130, alignment: .trailing)
        }
        .padding(EdgeInsets(top: 15, leading: 16, bottom: 15, trailing: 16))
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
    }

    /// The last two cells swap for the student's own entries: the class tag
    /// and reminder say more there than âmbito and situação.
    private var metaGrid: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 2), spacing: 8) {
            metaCell(label: .localized(.calendarMetaWhen), value: range)
            metaCell(label: .localized(.calendarMetaDuration), value: CalendarFormat.duration(days: event.spanDays))
            if let personal = event.personal {
                metaCell(
                    label: .localized(.calendarPersonalMetaDiscipline),
                    value: personal.discipline?.code ?? .localized(.calendarPersonalDisciplineNone)
                )
                metaCell(label: .localized(.calendarPersonalMetaReminder), value: personal.reminder.detailLabel)
            } else {
                metaCell(label: .localized(.calendarMetaScope), value: event.scope.label)
                metaCell(label: .localized(.calendarMetaStatus), value: situation)
            }
        }
    }

    private func notesCard(_ notes: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "text.alignleft")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .padding(.top, 2)
            Text(notes)
                .font(.system(size: 13.5, weight: .medium))
                .lineSpacing(2)
                .foregroundStyle(UNESColor.ink2)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func disciplineCard(_ discipline: PersonalEvent.DisciplineTag) -> some View {
        HStack(spacing: 10) {
            Circle()
                .fill(category.color)
                .frame(width: 9, height: 9)
            Text(discipline.name)
                .font(.system(size: 13.5, weight: .semibold))
                .tracking(-0.14)
                .foregroundStyle(UNESColor.ink2)
            Spacer(minLength: 0)
        }
        .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private var situation: String {
        switch status {
        case .active: .localized(.calendarStatusActive)
        case .past: .localized(.calendarStatusPast)
        case .future: countdown.phrase.prefix(1).uppercased() + countdown.phrase.dropFirst()
        }
    }

    private func metaCell(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
            Text(value)
                .font(.system(size: 15, weight: .bold))
                .tracking(-0.3)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 11, leading: 13, bottom: 11, trailing: 13))
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private var fixedChip: some View {
        HStack(spacing: 6) {
            Image(systemName: "repeat")
                .font(.system(size: 11, weight: .semibold))
            Text(.calendarEventFixedBadge)
                .font(.system(size: 12, weight: .semibold))
        }
        .foregroundStyle(UNESColor.ink3)
        .padding(EdgeInsets(top: 6, leading: 12, bottom: 6, trailing: 12))
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    /// The student's own entries get edit + delete where the institutional
    /// ones get the system-calendar handoff.
    @ViewBuilder
    private var actions: some View {
        if let personal = event.personal {
            HStack(spacing: 8) {
                Button {
                    onEdit(personal)
                } label: {
                    filledLabel(icon: "pencil", text: .calendarPersonalEditAction)
                }
                .buttonStyle(TilePressStyle())

                Button {
                    onDelete(personal)
                } label: {
                    Image(systemName: "trash")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(UNESColor.alertRed)
                        .frame(width: 52)
                        .frame(maxHeight: .infinity)
                        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .strokeBorder(UNESColor.cardLine)
                        }
                }
                .buttonStyle(TilePressStyle())
            }
            .fixedSize(horizontal: false, vertical: true)
        } else {
            addToCalendarButton
        }
    }

    @ViewBuilder
    private var addToCalendarButton: some View {
        #if canImport(EventKitUI)
        Button {
            onAddToCalendar()
            isCalendarEditPresented = true
        } label: {
            filledLabel(icon: "calendar.badge.plus", text: .calendarEventAddToCalendar)
        }
        .buttonStyle(TilePressStyle())
        #endif
    }

    private func filledLabel(icon: String, text: LocalizedStringResource) -> some View {
        HStack(spacing: 7) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .semibold))
            Text(text)
                .font(.system(size: 14, weight: .semibold))
                .tracking(-0.14)
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 13)
        .background(category.color, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: category.color.opacity(0.27), radius: 12, y: 8)
    }
}

#if canImport(EventKitUI)

/// The system event composer, prefilled with the academic event as an
/// all-day entry. Presenting it needs no calendar permission on iOS 17+.
private struct CalendarEventEditor: UIViewControllerRepresentable {
    let event: CalendarEvent
    var onFinish: () -> Void

    func makeUIViewController(context: Context) -> EKEventEditViewController {
        let store = EKEventStore()
        let entry = EKEvent(eventStore: store)
        entry.title = event.title
        entry.isAllDay = true
        entry.startDate = event.start
        entry.endDate = event.endOrStart
        entry.notes = .localized(.calendarSystemEventNotes)

        let controller = EKEventEditViewController()
        controller.eventStore = store
        controller.event = entry
        controller.editViewDelegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ controller: EKEventEditViewController, context: Context) {
        context.coordinator.onFinish = onFinish
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(onFinish: onFinish)
    }

    final class Coordinator: NSObject, EKEventEditViewDelegate {
        var onFinish: () -> Void

        init(onFinish: @escaping () -> Void) {
            self.onFinish = onFinish
        }

        func eventEditViewController(
            _ controller: EKEventEditViewController,
            didCompleteWith action: EKEventEditViewAction
        ) {
            onFinish()
        }
    }
}

#endif

#Preview {
    let events: [CalendarEvent] = .preview()
    Color.clear.sheet(isPresented: .constant(true)) {
        CalendarEventSheet(
            event: events.first { $0.fixed }!,
            today: .now,
            onClose: {}
        )
    }
}

import SwiftUI

/// The agenda body: events grouped by month, headers pinned while their
/// month scrolls by.
struct CalendarAgendaList: View {
    let groups: [CalendarMonthGroup]
    let today: Date
    var actions: CalendarRowActions

    var body: some View {
        if groups.isEmpty {
            CalendarEmptyState(isPersonalScope: actions.isPersonalScope, onAdd: actions.onAdd)
        } else {
            LazyVStack(spacing: 0, pinnedViews: [.sectionHeaders]) {
                ForEach(groups) { group in
                    Section {
                        ForEach(group.events) { event in
                            CalendarEventRow(event: event, today: today, actions: actions)
                                .padding(.bottom, 9)
                        }
                    } header: {
                        CalendarMonthHeader(group: group, today: today)
                    }
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

/// What a row can do. Only the student's own rows use edit/delete.
struct CalendarRowActions {
    var onOpen: (CalendarEvent) -> Void
    var onEdit: (PersonalEvent) -> Void = { _ in }
    var onDelete: (PersonalEvent) -> Void = { _ in }
    var onAdd: (Date?) -> Void = { _ in }
    var isPersonalScope = false
}

/// "Abril · 2026 · 6 eventos" — the running month gets the accent.
struct CalendarMonthHeader: View {
    let group: CalendarMonthGroup
    let today: Date

    private var isCurrent: Bool {
        let calendar = Calendar.current
        return calendar.component(.year, from: today) == group.year
            && calendar.component(.month, from: today) == group.month
    }

    var body: some View {
        HStack(alignment: .lastTextBaseline, spacing: 9) {
            Text(CalendarFormat.monthTitle(group.month))
                .font(.system(size: 24, weight: .bold))
                .tracking(-0.72)
                .foregroundStyle(isCurrent ? UNESColor.accent : UNESColor.ink)
            Text(verbatim: "\(String(group.year)) · \(CalendarFormat.eventCount(group.events.count))")
                .font(.system(size: 13, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)
            Spacer()
        }
        .padding(EdgeInsets(top: 18, leading: 6, bottom: 12, trailing: 6))
        .background {
            LinearGradient(
                stops: [
                    .init(color: UNESColor.surface, location: 0),
                    .init(color: UNESColor.surface, location: 0.78),
                    .init(color: UNESColor.surface.opacity(0), location: 1),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        }
    }
}

/// One agenda card: date block, tone tile, category line, title, countdown.
/// The student's own entries wear a tinted (rather than filled) tile and
/// carry edit/delete in a long-press menu.
struct CalendarEventRow: View {
    let event: CalendarEvent
    let today: Date
    var actions: CalendarRowActions

    private var category: CalendarCategory { event.category }
    private var status: CalendarStatus { CalendarMath.status(event, today: today) }

    var body: some View {
        Button {
            actions.onOpen(event)
        } label: {
            HStack(spacing: 13) {
                dateBlock
                toneTile
                textColumn
                Image(systemName: event.isPersonal ? "pencil" : "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(EdgeInsets(top: 12, leading: 13, bottom: 12, trailing: 13))
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 7, y: 5)
            .opacity(status == .past ? 0.5 : 1)
        }
        .buttonStyle(.pressableCard)
        .modifier(CalendarPersonalRowMenu(personal: event.personal, actions: actions))
    }

    private var dateBlock: some View {
        VStack(spacing: 3) {
            Text(CalendarFormat.dayNumber(event.start))
                .font(.system(size: 22, weight: .bold))
                .tracking(-0.66)
                .monospacedDigit()
                .foregroundStyle(status == .active ? category.color : UNESColor.ink)
            Text(dateBlockCaption)
                .textCase(.uppercase)
                .font(.system(size: 10.5, weight: .semibold))
                .tracking(0.3)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)
        }
        .frame(width: 46)
    }

    /// "QUI" for a single day, "QUI–20" when the event runs to another one.
    private var dateBlockCaption: String {
        let weekday = CalendarFormat.weekday(event.start)
        guard let end = event.end else { return weekday }
        return "\(weekday)–\(CalendarFormat.dayNumber(end))"
    }

    @ViewBuilder
    private var toneTile: some View {
        let tile = Image(systemName: category.icon)
            .font(.system(size: 16, weight: .medium))
            .frame(width: 38, height: 38)
        if event.isPersonal {
            tile
                .foregroundStyle(category.color)
                .background(category.color.opacity(0.14), in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 11, style: .continuous)
                        .strokeBorder(category.color.opacity(0.35))
                }
        } else {
            tile
                .foregroundStyle(.white)
                .background(category.color, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                .shadow(color: category.color.opacity(0.31), radius: 6, y: 5)
        }
    }

    private var textColumn: some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack(spacing: 7) {
                Text(category.label)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(category.color)
                provenance
                if status == .active && !event.closed {
                    Spacer(minLength: 4)
                    HStack(spacing: 4) {
                        Circle()
                            .fill(UNESColor.accent)
                            .frame(width: 5, height: 5)
                            .modifier(CalendarPulse(active: true, duration: 1.6))
                        Text(.calendarEventOpen)
                            .font(.system(size: 10.5, weight: .semibold))
                    }
                    .foregroundStyle(UNESColor.accent)
                }
            }
            Text(event.title)
                .font(.system(size: 14.5, weight: .semibold))
                .tracking(-0.15)
                .lineSpacing(2)
                .foregroundStyle(UNESColor.ink)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
            Text(CalendarMath.countdown(event, today: today).phrase)
                .font(.system(size: 11.5, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(status == .past ? UNESColor.ink4 : UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// A linked class reads as its own tinted code chip; everything else is
    /// the plain "· âmbito" (or "· meu") caption.
    @ViewBuilder
    private var provenance: some View {
        if let discipline = event.personal?.discipline {
            Text(discipline.code)
                .font(.system(size: 10.5, weight: .bold))
                .foregroundStyle(category.color)
                .padding(EdgeInsets(top: 2, leading: 6, bottom: 2, trailing: 6))
                .background(category.color.opacity(0.14), in: RoundedRectangle(cornerRadius: 6, style: .continuous))
        } else {
            Text(verbatim: "· \(event.provenanceLabel)")
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
        }
    }
}

/// Long-press edit/delete, on the student's own rows only. `contextMenu` is
/// deprecated on watchOS, which never mounts this screen.
private struct CalendarPersonalRowMenu: ViewModifier {
    let personal: PersonalEvent?
    let actions: CalendarRowActions

    @ViewBuilder
    func body(content: Content) -> some View {
        #if os(iOS)
        if let personal {
            content.contextMenu {
                Button {
                    actions.onEdit(personal)
                } label: {
                    Label(String.localized(.commonEdit), systemImage: "pencil")
                }
                Button(role: .destructive) {
                    actions.onDelete(personal)
                } label: {
                    Label(String.localized(.commonDelete), systemImage: "trash")
                }
            }
        } else {
            content
        }
        #else
        content
        #endif
    }
}

/// Nothing matches the filters. Under "Meus" it turns into the invitation to
/// create the first entry.
struct CalendarEmptyState: View {
    var isPersonalScope = false
    var onAdd: (Date?) -> Void = { _ in }

    var body: some View {
        VStack(spacing: 0) {
            Image(systemName: isPersonalScope ? "calendar.badge.plus" : "clock")
                .font(.system(size: 22, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(width: 52, height: 52)
                .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .padding(.bottom, 14)
            Text(isPersonalScope ? .calendarPersonalEmptyTitle : .calendarEmptyTitle)
                .font(.system(size: 16, weight: .bold))
                .tracking(-0.32)
                .foregroundStyle(UNESColor.ink)
            Text(isPersonalScope ? .calendarPersonalEmptySubtitle : .calendarEmptySubtitle)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .multilineTextAlignment(.center)
                .padding(.top, 4)
            if isPersonalScope {
                Button {
                    onAdd(nil)
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "plus")
                            .font(.system(size: 14, weight: .semibold))
                        Text(.calendarPersonalEmptyAction)
                            .font(.system(size: 14.5, weight: .semibold))
                    }
                    .foregroundStyle(.white)
                    .padding(EdgeInsets(top: 12, leading: 20, bottom: 12, trailing: 20))
                    .background(UNESColor.accent, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(TilePressStyle())
                .padding(.top, 18)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(EdgeInsets(top: 48, leading: 24, bottom: 48, trailing: 24))
    }
}

#Preview {
    let events: [CalendarEvent] = .preview()
    ScrollView {
        CalendarAgendaList(
            groups: events.groupedByMonth(),
            today: .now,
            actions: CalendarRowActions(onOpen: { _ in })
        )
    }
    .background(UNESColor.surface)
}

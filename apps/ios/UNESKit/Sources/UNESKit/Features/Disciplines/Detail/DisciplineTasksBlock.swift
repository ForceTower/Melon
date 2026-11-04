import SwiftUI

// MARK: - Model

enum DisciplineTasks {
    /// The discipline's own entries: pending first, then the checked-off
    /// ones, each by due day.
    static func tasks(for disciplineId: String, in events: [PersonalEvent]) -> [PersonalEvent] {
        events
            .filter { $0.discipline?.id == disciplineId }
            .sorted { lhs, rhs in
                (lhs.isCompleted ? 1 : 0, lhs.start, lhs.createdAt, lhs.id)
                    < (rhs.isCompleted ? 1 : 0, rhs.start, rhs.createdAt, rhs.id)
            }
    }
}

/// When a task is due, from its start day. A range that is still running
/// reads as due today; it only turns overdue once its last day is behind.
enum DisciplineTaskDue: Equatable {
    case done
    case overdue(days: Int)
    case today
    case tomorrow
    case inDays(Int)
    case on(Date)

    init(_ event: PersonalEvent, today: Date, calendar: Calendar = .current) {
        if event.isCompleted {
            self = .done
            return
        }
        let start = CalendarFormat.parse(event.start, calendar: calendar) ?? today
        let end = event.end.flatMap { CalendarFormat.parse($0, calendar: calendar) } ?? start
        let toStart = CalendarMath.daysBetween(today, start, calendar: calendar)
        let toEnd = CalendarMath.daysBetween(today, end, calendar: calendar)
        self = if toEnd < 0 {
            .overdue(days: -toEnd)
        } else if toStart <= 0 {
            .today
        } else if toStart == 1 {
            .tomorrow
        } else if toStart <= 7 {
            .inDays(toStart)
        } else {
            .on(start)
        }
    }

    var label: String {
        switch self {
        case .done: .localized(.calendarPersonalCompleted)
        case let .overdue(days): .localized(.disciplinesTasksOverdue(days))
        case .today: .localized(.disciplinesTasksDueToday)
        case .tomorrow: .localized(.disciplinesTasksDueTomorrow)
        case let .inDays(days): .localized(.disciplinesTasksDueInDays(days))
        case let .on(date): .localized(.disciplinesTasksDueOn(CalendarFormat.dateShort(date)))
        }
    }
}

// MARK: - Block

/// "Tarefas": the student's own entries tagged with this discipline, as a
/// checklist. The rows are the same entries the Calendário shows.
struct DisciplineTasksBlock: View {
    let tasks: [PersonalEvent]
    let today: Date
    var canAdd: Bool
    var onToggle: (PersonalEvent) -> Void
    var onOpen: (PersonalEvent) -> Void
    var onAdd: () -> Void

    private var summary: String? {
        guard !tasks.isEmpty else { return nil }
        let pending = tasks.count { !$0.isCompleted }
        return pending == 0
            ? .localized(.disciplinesTasksAllDone)
            : .localized(.disciplinesTasksPendingCount(pending))
    }

    var body: some View {
        VStack(spacing: 0) {
            DisciplineSectionHeader(title: .disciplinesTasksTitle, trailing: summary)

            VStack(spacing: 0) {
                if tasks.isEmpty {
                    Text(.disciplinesTasksEmpty)
                        .font(.system(size: 13.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(EdgeInsets(top: 20, leading: 22, bottom: 16, trailing: 22))
                } else {
                    ForEach(Array(tasks.enumerated()), id: \.element.id) { index, task in
                        DisciplineTaskRow(
                            task: task,
                            due: DisciplineTaskDue(task, today: today),
                            showsDivider: canAdd || index < tasks.count - 1,
                            onToggle: { onToggle(task) },
                            onOpen: { onOpen(task) }
                        )
                    }
                }
                if canAdd {
                    addRow
                }
            }
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
        }
    }

    private var addRow: some View {
        Button(action: onAdd) {
            HStack(spacing: 8) {
                Image(systemName: "plus")
                    .font(.system(size: 14, weight: .semibold))
                Text(.disciplinesTasksAdd)
                    .font(.system(size: 14.5, weight: .semibold))
                    .tracking(-0.15)
            }
            .foregroundStyle(UNESColor.accent)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 13)
            .contentShape(Rectangle())
        }
        .buttonStyle(TaskRowPressStyle())
    }
}

private struct DisciplineTaskRow: View {
    let task: PersonalEvent
    let due: DisciplineTaskDue
    var showsDivider: Bool
    var onToggle: () -> Void
    var onOpen: () -> Void

    private var category: CalendarCategory { task.category.calendarCategory }

    private var dueColor: Color {
        if case .overdue = due { return UNESColor.coral }
        return UNESColor.ink4
    }

    var body: some View {
        HStack(spacing: 0) {
            Button(action: onToggle) {
                Image(systemName: task.isCompleted ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 22, weight: .regular))
                    .foregroundStyle(task.isCompleted ? category.color : category.color.opacity(0.7))
                    .contentTransition(.symbolEffect(.replace))
                    .frame(width: 52, height: 52)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(task.isCompleted ? .calendarPersonalMarkPending : .calendarPersonalMarkDone))
            .accessibilityValue(Text(task.title))

            Button(action: onOpen) {
                HStack(spacing: 10) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(task.title)
                            .font(.system(size: 14.5, weight: .semibold))
                            .tracking(-0.15)
                            .strikethrough(task.isCompleted)
                            .foregroundStyle(task.isCompleted ? UNESColor.ink3 : UNESColor.ink)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                        HStack(spacing: 5) {
                            // A task's kind is already what the due label says.
                            if task.category != .task {
                                Text(task.category.label)
                                    .fontWeight(.semibold)
                                    .foregroundStyle(category.color)
                                Text(verbatim: "·")
                                    .foregroundStyle(UNESColor.ink4)
                            }
                            Text(due.label)
                                .monospacedDigit()
                                .foregroundStyle(dueColor)
                        }
                        .font(.system(size: 12, weight: .medium))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(UNESColor.ink4)
                }
                .padding(EdgeInsets(top: 11, leading: 0, bottom: 11, trailing: 15))
                .contentShape(Rectangle())
            }
            .buttonStyle(TaskRowPressStyle())
        }
        .opacity(task.isCompleted ? 0.6 : 1)
        .overlay(alignment: .bottom) {
            if showsDivider {
                Rectangle()
                    .fill(UNESColor.line)
                    .frame(height: 0.5)
                    .padding(.leading, 52)
            }
        }
    }
}

private struct TaskRowPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? UNESColor.surface2 : .clear)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

#Preview {
    let now = Date.now
    var events: [PersonalEvent] = .preview(now: now)
    events[0].completedAt = now
    let tag = PersonalEvent.DisciplineTag(id: "d3", code: "EXA412", name: "Física II")
    events[1].discipline = tag
    events[2].discipline = tag
    return ScrollView {
        VStack(spacing: 22) {
            DisciplineTasksBlock(
                tasks: DisciplineTasks.tasks(for: "d3", in: events),
                today: Calendar.current.startOfDay(for: now),
                canAdd: true,
                onToggle: { _ in },
                onOpen: { _ in },
                onAdd: {}
            )
            DisciplineTasksBlock(
                tasks: [],
                today: now,
                canAdd: true,
                onToggle: { _ in },
                onOpen: { _ in },
                onAdd: {}
            )
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}

import ComposableArchitecture
import Foundation

/// The composer behind "Novo evento" / "Editar evento". It owns the write:
/// saving persists through the repository and the Calendário screen picks the
/// change up from its own observation, so no result has to travel back up.
@Reducer
struct CalendarPersonalEventFeature {
    @ObservableState
    struct State: Equatable {
        /// The entry being edited; nil while composing a new one.
        let editing: PersonalEvent?
        var disciplines: [PersonalEventDisciplineOption]
        var title: String
        var start: Date
        /// nil while the entry is single-day — the "Período" switch.
        var end: Date?
        var category: PersonalEvent.Category
        var discipline: PersonalEvent.DisciplineTag?
        var reminder: PersonalEvent.Reminder
        var notes: String
        @Presents var confirmDelete: ConfirmationDialogState<Action.Delete>?

        var isNew: Bool { editing == nil }

        var canSave: Bool {
            !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        }

        init(day: Date, disciplines: [PersonalEventDisciplineOption], calendar: Calendar = .current) {
            editing = nil
            self.disciplines = disciplines
            title = ""
            start = calendar.startOfDay(for: day)
            end = nil
            category = .task
            discipline = nil
            reminder = .dayBefore
            notes = ""
        }

        init(
            editing event: PersonalEvent,
            disciplines: [PersonalEventDisciplineOption],
            calendar: Calendar = .current
        ) {
            editing = event
            self.disciplines = disciplines
            title = event.title
            start = CalendarFormat.parse(event.start, calendar: calendar) ?? calendar.startOfDay(for: .now)
            end = event.end.flatMap { CalendarFormat.parse($0, calendar: calendar) }
            category = event.category
            discipline = event.discipline
            reminder = event.reminder
            notes = event.notes
        }
    }

    enum Action: Equatable, BindableAction {
        case cancelTapped
        case saveTapped
        case deleteTapped
        case periodToggled(Bool)
        case startPicked(Date)
        case endPicked(Date)
        case confirmDelete(PresentationAction<Delete>)
        case binding(BindingAction<State>)

        enum Delete: Equatable {
            case confirmed
        }
    }

    @Dependency(\.personalEventsRepository) var personalEvents
    @Dependency(\.calendar) var calendar
    @Dependency(\.date.now) var now
    @Dependency(\.uuid) var uuid
    @Dependency(\.analytics) var analytics
    @Dependency(\.dismiss) var dismiss

    private let log = Log.scoped("CalendarPersonalEvent")

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .cancelTapped:
                return .run { _ in await dismiss() }

            case .saveTapped:
                guard state.canSave else { return .none }
                let event = state.composed(id: uuid().uuidString, now: now)
                log.info("save id=\(event.id) new=\(state.isNew)")
                analytics.selectContent(
                    contentType: ContentTypes.calendarEvent,
                    itemId: event.id,
                    properties: [
                        "action": state.isNew ? "create" : "update",
                        "category": event.category.rawValue,
                    ]
                )
                return .run { [log] _ in
                    do {
                        try await personalEvents.save(event)
                    } catch {
                        log.error("save failed id=\(event.id)", error: error)
                    }
                    await dismiss()
                }

            case .deleteTapped:
                guard let event = state.editing else { return .none }
                state.confirmDelete = .deletePersonalEvent(titled: event.title)
                return .none

            case .confirmDelete(.presented(.confirmed)):
                guard let event = state.editing else { return .none }
                log.info("delete id=\(event.id)")
                analytics.selectContent(
                    contentType: ContentTypes.calendarEvent,
                    itemId: event.id,
                    properties: ["action": "delete"]
                )
                return .run { [log] _ in
                    do {
                        try await personalEvents.delete(event.id)
                    } catch {
                        log.error("delete failed id=\(event.id)", error: error)
                    }
                    await dismiss()
                }

            case .confirmDelete:
                return .none

            case let .periodToggled(isOn):
                state.end = isOn ? calendar.date(byAdding: .day, value: 2, to: state.start) : nil
                return .none

            case let .startPicked(day):
                state.start = calendar.startOfDay(for: day)
                // A start that overtook the end collapses the range rather
                // than storing an interval that runs backwards.
                if let end = state.end, end <= state.start {
                    state.end = calendar.date(byAdding: .day, value: 2, to: state.start)
                }
                return .none

            case let .endPicked(day):
                state.end = calendar.startOfDay(for: day)
                return .none

            case .binding:
                return .none
            }
        }
        .ifLet(\.$confirmDelete, action: \.confirmDelete)
    }
}

extension CalendarPersonalEventFeature.State {
    /// An edit keeps its own id, so the reminder request and the row identity
    /// survive the rewrite.
    func composed(id newId: String, now: Date) -> PersonalEvent {
        PersonalEvent(
            id: editing?.id ?? newId,
            title: title.trimmingCharacters(in: .whitespacesAndNewlines),
            start: start.dayStamp,
            end: end.flatMap { $0 > start ? $0.dayStamp : nil },
            category: category,
            discipline: discipline,
            reminder: reminder,
            notes: notes.trimmingCharacters(in: .whitespacesAndNewlines),
            createdAt: editing?.createdAt ?? now
        )
    }
}

extension ConfirmationDialogState where Action == CalendarPersonalEventFeature.Action.Delete {
    static func deletePersonalEvent(titled title: String) -> Self {
        ConfirmationDialogState {
            TextState(String.localized(.calendarPersonalDeleteConfirmTitle))
        } actions: {
            ButtonState(role: .destructive, action: .confirmed) {
                TextState(String.localized(.commonDelete))
            }
            ButtonState(role: .cancel) {
                TextState(String.localized(.commonCancel))
            }
        } message: {
            TextState(title)
        }
    }
}

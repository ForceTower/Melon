import ComposableArchitecture
import Foundation

/// The academic calendar — every UEFS date the student should know about
/// (deadlines, exams, holidays) plus the entries they add themselves, as an
/// agenda or a month grid. Pushed from the "Calendário" shortcut in the Eu hub.
@Reducer
struct CalendarFeature {
    /// How the body renders. Persisted so the screen reopens the way the
    /// student left it.
    enum ViewMode: String, Equatable, Sendable {
        case agenda, grid

        var toggled: ViewMode {
            self == .agenda ? .grid : .agenda
        }
    }

    @ObservableState
    struct State: Equatable {
        var events: [CalendarEvent] = []
        var personalEvents: [PersonalEvent] = []
        /// Class picker choices for the composer, from the running semester.
        var disciplines: [PersonalEventDisciplineOption] = []
        var category: CalendarCategoryFilter = .all
        var scopeFilter: CalendarScopeFilter = .all
        /// Local midnight — statuses and countdowns are derived against it.
        /// Resolved on `task`; the placeholder keeps `State()` deterministic.
        var today = Date.distantPast
        /// The grid's focused day.
        var selectedDay = Date.distantPast
        /// When the feed landed — drives the sync footer; nil while the
        /// first fetch is in flight.
        var fetchedAt: Date?
        /// Presents the detail sheet while non-nil.
        var detail: CalendarEvent?
        @Presents var composer: CalendarPersonalEventFeature.State?
        @Presents var confirmDelete: ConfirmationDialogState<Action.Delete>?
        @Shared(.appStorage("calendarViewMode")) var viewMode: ViewMode = .agenda

        /// Both feeds on one timeline.
        var allEvents: [CalendarEvent] {
            (events + personalEvents.compactMap { CalendarEvent($0) })
                .sorted { ($0.start, $0.title) < ($1.start, $1.title) }
        }

        var filtered: [CalendarEvent] {
            allEvents.filter { category.matches($0) && scopeFilter.matches($0) }
        }

        /// Agenda body: hide what's over, keep multi-day events still running.
        var agendaGroups: [CalendarMonthGroup] {
            filtered
                .filter { CalendarMath.status($0, today: today) != .past }
                .groupedByMonth()
        }

        var hero: CalendarEvent? {
            CalendarMath.nextDeadline(in: filtered, today: today)
        }

        var selectedDayEvents: [CalendarEvent] {
            CalendarMath.events(on: selectedDay, in: filtered)
        }

        /// Drives the header subtitle.
        var upcomingPersonalCount: Int {
            personalEvents
                .compactMap { CalendarEvent($0) }
                .count { CalendarMath.status($0, today: today) != .past }
        }
    }

    enum Action: Equatable {
        case task
        case eventsLoaded([CalendarEvent])
        case eventsFailed
        case personalEventsLoaded([PersonalEvent])
        case disciplinesLoaded([PersonalEventDisciplineOption])
        case categorySelected(CalendarCategoryFilter)
        case scopeSelected(CalendarScopeFilter)
        case viewModeToggled
        case daySelected(Date)
        case eventTapped(CalendarEvent)
        case detailDismissed
        case addToCalendarTapped(CalendarEvent)
        case addTapped(day: Date?)
        case editTapped(PersonalEvent)
        case deleteTapped(PersonalEvent)
        case composer(PresentationAction<CalendarPersonalEventFeature.Action>)
        case confirmDelete(PresentationAction<Delete>)

        enum Delete: Equatable {
            case confirmed(String)
        }
    }

    @Dependency(\.eventsRepository) var eventsRepository
    @Dependency(\.personalEventsRepository) var personalEventsRepository
    @Dependency(\.disciplinesRepository) var disciplinesRepository
    @Dependency(\.calendar) var calendar
    @Dependency(\.date.now) var now
    @Dependency(\.continuousClock) var clock
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("CalendarFeature")

    private enum CancelID { case personalEvents }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(Screens.calendar)
                state.today = calendar.startOfDay(for: now)
                if state.fetchedAt == nil {
                    state.selectedDay = state.today
                }
                return .merge(
                    .run { [log, now] send in
                        do {
                            let events = try await eventsRepository.calendar(now)
                            await send(.eventsLoaded(events.compactMap { CalendarEvent($0) }))
                        } catch {
                            log.warn("calendar events fetch failed", error: error)
                            await send(.eventsFailed)
                        }
                    },
                    // `.task` runs again on every foreground; without the
                    // cancel a second observation would stack on the first.
                    .run { send in
                        for await events in personalEventsRepository.observe() {
                            await send(.personalEventsLoaded(events))
                        }
                    }
                    .cancellable(id: CancelID.personalEvents, cancelInFlight: true),
                    .run { [now] send in
                        let overview = try? await disciplinesRepository.cached(now)
                        await send(.disciplinesLoaded(overview?.composerOptions ?? []))
                    },
                    // A day crossed since the last write moves which reminders
                    // are still ahead, and the observation won't re-emit
                    // without one.
                    .run { _ in await personalEventsRepository.reconcileReminders() }
                )

            case let .eventsLoaded(events):
                state.events = events
                state.fetchedAt = now
                return .none

            case .eventsFailed:
                // No dedicated error state — an empty feed reads as the
                // filtered empty state, and the next appearance retries.
                return .none

            case let .personalEventsLoaded(events):
                state.personalEvents = events
                return .none

            case let .disciplinesLoaded(disciplines):
                state.disciplines = disciplines
                return .none

            case let .categorySelected(category):
                state.category = category
                return .none

            case let .scopeSelected(scope):
                state.scopeFilter = scope
                return .none

            case .viewModeToggled:
                state.$viewMode.withLock { $0 = $0.toggled }
                return .none

            case let .daySelected(day):
                state.selectedDay = calendar.startOfDay(for: day)
                return .none

            case let .eventTapped(event):
                analytics.selectContent(
                    contentType: ContentTypes.calendarEvent,
                    itemId: event.id,
                    properties: ["category": event.category.analyticsValue]
                )
                state.detail = event
                return .none

            case .detailDismissed:
                state.detail = nil
                return .none

            case let .addToCalendarTapped(event):
                analytics.selectContent(
                    contentType: ContentTypes.calendarEvent,
                    itemId: event.id,
                    properties: ["action": "save"]
                )
                return .none

            case let .addTapped(day):
                if let wait = closeDetailFirst(&state, then: .addTapped(day: day)) { return wait }
                state.composer = CalendarPersonalEventFeature.State(
                    day: day ?? state.today,
                    disciplines: state.disciplines,
                    calendar: calendar
                )
                return .none

            case let .editTapped(event):
                if let wait = closeDetailFirst(&state, then: .editTapped(event)) { return wait }
                state.composer = CalendarPersonalEventFeature.State(
                    editing: event,
                    disciplines: state.disciplines,
                    calendar: calendar
                )
                return .none

            case let .deleteTapped(event):
                if let wait = closeDetailFirst(&state, then: .deleteTapped(event)) { return wait }
                state.confirmDelete = .deletePersonalEvent(id: event.id, titled: event.title)
                return .none

            case let .confirmDelete(.presented(.confirmed(id))):
                log.info("delete id=\(id)")
                analytics.selectContent(
                    contentType: ContentTypes.calendarEvent,
                    itemId: id,
                    properties: ["action": "delete"]
                )
                return .run { [log] _ in
                    do {
                        try await personalEventsRepository.delete(id)
                    } catch {
                        log.error("delete failed id=\(id)", error: error)
                    }
                }

            case .confirmDelete, .composer:
                return .none
            }
        }
        .ifLet(\.$composer, action: \.composer) {
            CalendarPersonalEventFeature()
        }
        .ifLet(\.$confirmDelete, action: \.confirmDelete)
    }

    /// Presenting the composer (or the confirm dialog) in the same frame the
    /// detail sheet dismisses loses it — UIKit drops the second presentation.
    /// Dismiss first, replay the action once the sheet is gone.
    private func closeDetailFirst(_ state: inout State, then action: Action) -> Effect<Action>? {
        guard state.detail != nil else { return nil }
        state.detail = nil
        return .run { send in
            try await clock.sleep(for: .milliseconds(320))
            await send(action)
        }
    }
}

extension DisciplinesOverview {
    /// The running semester's disciplines as class-picker choices.
    var composerOptions: [PersonalEventDisciplineOption] {
        (current?.disciplines ?? []).map { discipline in
            PersonalEventDisciplineOption(
                tag: PersonalEvent.DisciplineTag(
                    id: discipline.id,
                    code: discipline.code,
                    name: discipline.name
                ),
                colorIndex: discipline.colorIndex
            )
        }
    }
}

extension ConfirmationDialogState where Action == CalendarFeature.Action.Delete {
    static func deletePersonalEvent(id: String, titled title: String) -> Self {
        ConfirmationDialogState {
            TextState(String.localized(.calendarPersonalDeleteConfirmTitle))
        } actions: {
            ButtonState(role: .destructive, action: .confirmed(id)) {
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

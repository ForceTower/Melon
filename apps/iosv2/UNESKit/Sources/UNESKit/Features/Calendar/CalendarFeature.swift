import ComposableArchitecture
import Foundation

/// The academic calendar — every UEFS date the student should know about
/// (deadlines, exams, holidays), as an agenda or a month grid. Pushed from
/// the "Calendário" shortcut in the Eu hub.
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
        @Shared(.appStorage("calendarViewMode")) var viewMode: ViewMode = .agenda

        var filtered: [CalendarEvent] {
            events.filter { category.matches($0) && scopeFilter.matches($0) }
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
    }

    enum Action: Equatable {
        case task
        case eventsLoaded([CalendarEvent])
        case eventsFailed
        case categorySelected(CalendarCategoryFilter)
        case scopeSelected(CalendarScopeFilter)
        case viewModeToggled
        case daySelected(Date)
        case eventTapped(CalendarEvent)
        case detailDismissed
    }

    @Dependency(\.eventsRepository) var eventsRepository
    @Dependency(\.calendar) var calendar
    @Dependency(\.date.now) var now

    private let log = Log.scoped("CalendarFeature")

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                state.today = calendar.startOfDay(for: now)
                if state.fetchedAt == nil {
                    state.selectedDay = state.today
                }
                return .run { [log] send in
                    do {
                        let events = try await eventsRepository.calendar(now)
                        await send(.eventsLoaded(events.compactMap { CalendarEvent($0) }))
                    } catch {
                        log.warn("calendar events fetch failed", error: error)
                        await send(.eventsFailed)
                    }
                }

            case let .eventsLoaded(events):
                state.events = events
                state.fetchedAt = now
                return .none

            case .eventsFailed:
                // No dedicated error state — an empty feed reads as the
                // filtered empty state, and the next appearance retries.
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
                state.detail = event
                return .none

            case .detailDismissed:
                state.detail = nil
                return .none
            }
        }
    }
}

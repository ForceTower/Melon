import ComposableArchitecture
import Foundation

/// The event hub: welcome reveal on first entry, phase-aware hero, quick
/// links and the day-by-day schedule. Receives the already-fetched event from
/// Home, so it renders offline; pull-to-refresh re-fetches.
@Reducer
struct CampusEventFeature {
    /// Remembers which event edition already played the welcome reveal.
    static let welcomeSeenKey = "campus_event_welcome_seen_id"

    @ObservableState
    struct State: Equatable {
        var event: CampusEvent
        var selectedDay: Date?
        var filter: CampusEventAudience = .everyone
        var isShowingWelcome: Bool

        @ObservationStateIgnored
        @Shared(.appStorage(CampusEventFeature.welcomeSeenKey)) var welcomeSeenEventId = ""

        init(event: CampusEvent) {
            self.event = event
            @Shared(.appStorage(CampusEventFeature.welcomeSeenKey)) var seenEventId = ""
            isShowingWelcome = seenEventId != event.id
        }
    }

    enum Action: Equatable {
        case task
        case refreshPulled
        case eventRefreshed(CampusEvent)
        case welcomeContinueTapped
        case dayTapped(Date)
        case filterChanged(CampusEventAudience)
        case activityTapped(CampusEventActivity)
        case speakersTapped
        case workshopsTapped
        case venuesTapped
        case organizationsTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openActivity(CampusEventActivity, CampusEvent)
            case openSpeakers(CampusEvent)
            case openWorkshops(CampusEvent)
            case openVenues(CampusEvent)
            case openOrganizations(CampusEvent)
        }
    }

    @Dependency(\.campusEventRepository) var campusEventRepository
    @Dependency(\.date.now) var now

    private let log = Log.scoped("CampusEventFeature")

    private enum CancelID { case observation, refresh }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                if state.selectedDay == nil {
                    state.selectedDay = state.event.currentDayDate(now: now)
                }
                // Mid-event admin edits (a room change!) land on the open hub
                // through the mirror. Un-featuring keeps the pushed payload —
                // blanking an open screen helps no one.
                return .run { send in
                    for await event in campusEventRepository.observe() {
                        guard let event else { continue }
                        await send(.eventRefreshed(event))
                    }
                }
                .cancellable(id: CancelID.observation, cancelInFlight: true)

            case .refreshPulled:
                return .run { _ in
                    // Rewrites the mirror; the update arrives through the
                    // observation. Failures keep the offline payload.
                    try? await campusEventRepository.refresh()
                }
                .cancellable(id: CancelID.refresh, cancelInFlight: true)

            case let .eventRefreshed(event):
                guard event.id != state.event.id || event.revision != state.event.revision else {
                    return .none
                }
                state.event = event
                if let selected = state.selectedDay,
                   !event.days().contains(where: { $0.date == selected }) {
                    state.selectedDay = event.currentDayDate(now: now)
                }
                return .none

            case .welcomeContinueTapped:
                log.info("welcome dismissed event=\(state.event.id)")
                let eventId = state.event.id
                state.$welcomeSeenEventId.withLock { $0 = eventId }
                state.isShowingWelcome = false
                return .none

            case let .dayTapped(day):
                state.selectedDay = day
                return .none

            case let .filterChanged(filter):
                state.filter = filter
                return .none

            case let .activityTapped(activity):
                log.info("open activity id=\(activity.id)")
                return .send(.delegate(.openActivity(activity, state.event)))

            case .speakersTapped:
                log.info("open speakers")
                return .send(.delegate(.openSpeakers(state.event)))

            case .workshopsTapped:
                log.info("open workshops")
                return .send(.delegate(.openWorkshops(state.event)))

            case .venuesTapped:
                log.info("open venues")
                return .send(.delegate(.openVenues(state.event)))

            case .organizationsTapped:
                log.info("open organizations")
                return .send(.delegate(.openOrganizations(state.event)))

            case .delegate:
                return .none
            }
        }
    }
}

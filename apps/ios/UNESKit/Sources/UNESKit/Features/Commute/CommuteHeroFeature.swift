import ComposableArchitecture
import Foundation

/// Today's class span, in minutes into the day — what decides whether the
/// hero asks about getting to campus or going home.
struct CommuteDaySchedule: Equatable, Hashable, Sendable {
    var firstStartMinute: Int?

    init(today: [TodayClass]) {
        firstStartMinute = today.map(\.startMinute).min()
    }

    init(firstStartMinute: Int? = nil) {
        self.firstStartMinute = firstStartMinute
    }

    /// The leg the student needs next: getting in until the first class
    /// starts (or noon on a day without classes), going home after that.
    func direction(for plan: CommutePlan, at now: Date, calendar: Calendar = .current) -> TransitDirection? {
        let directions = plan.directions
        guard directions.count > 1 else { return directions.first }
        let parts = calendar.dateComponents([.hour, .minute], from: now)
        let minute = (parts.hour ?? 0) * 60 + (parts.minute ?? 0)
        return minute < (firstStartMinute ?? 12 * 60) ? .toCampus : .fromCampus
    }
}

/// The single line under the hero class: when the student's bus passes their
/// stop. Polls only while Home is on screen.
@Reducer
struct CommuteHeroFeature {
    @ObservableState
    struct State: Equatable {
        @Shared(.commutePlan) var plan
        @Shared(.appStorage(FeatureFlags.commuteEnabledKey)) var isEnabled = false
        var direction: TransitDirection?
        var board: TransitStopBoard?
        var loadFailed = false

        var leg: CommuteLeg? {
            direction.flatMap { plan.leg(for: $0) }
        }

        /// The next arrival worth a glance, skipping buses already gone.
        func headline(now: Date) -> TransitArrival? {
            board?.arrivals.first { $0.latest > now || $0.phase == .atStop }
        }
    }

    enum Action: Equatable {
        case task(CommuteDaySchedule)
        case directionResolved(TransitDirection?)
        case boardLoaded(TransitStopBoard)
        case boardFailed
        case tapped
        case setupTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openBoard(TransitDirection)
            case openSetup
        }
    }

    @Dependency(\.transitRepository) var transitRepository
    @Dependency(\.date) var date
    @Dependency(\.continuousClock) var clock

    static let refreshInterval: Duration = .seconds(30)

    private enum CancelID { case poll }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case let .task(schedule):
                guard state.isEnabled, !state.plan.isEmpty else { return .none }
                return poll(schedule: schedule)

            case let .directionResolved(direction):
                if direction != state.direction {
                    state.direction = direction
                    state.board = nil
                    state.loadFailed = false
                }
                return .none

            case let .boardLoaded(board):
                guard board.direction == state.direction else { return .none }
                state.loadFailed = false
                state.board = board
                return .none

            case .boardFailed:
                state.loadFailed = true
                return .none

            case .tapped:
                guard let direction = state.direction else { return .send(.delegate(.openSetup)) }
                return .send(.delegate(.openBoard(direction)))

            case .setupTapped:
                return .send(.delegate(.openSetup))

            case .delegate:
                return .none
            }
        }
    }

    /// One loop for as long as Home is visible; each tick re-resolves the leg,
    /// so the line flips from going in to going home on its own.
    private func poll(schedule: CommuteDaySchedule) -> Effect<Action> {
        .run { send in
            @Shared(.commutePlan) var plan
            while true {
                let direction = schedule.direction(for: plan, at: date.now)
                await send(.directionResolved(direction))
                if let direction, let leg = plan.leg(for: direction) {
                    do {
                        let board = try await transitRepository.arrivals(leg.stopId, direction, leg.lineNumbers)
                        await send(.boardLoaded(board))
                    } catch is CancellationError {
                        return
                    } catch {
                        await send(.boardFailed)
                    }
                }
                try await clock.sleep(for: Self.refreshInterval)
            }
        }
        .cancellable(id: CancelID.poll, cancelInFlight: true)
    }
}

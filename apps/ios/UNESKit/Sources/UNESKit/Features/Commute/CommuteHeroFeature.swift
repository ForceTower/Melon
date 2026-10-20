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
    /// The student's own switch (in Ajustes) for those who don't take the bus.
    static let onHomeKey = "commuteOnHome"

    @ObservableState
    struct State: Equatable {
        @Shared(.appStorage(FeatureFlags.commuteEnabledKey)) var isEnabled = false
        @Shared(.appStorage(CommuteHeroFeature.onHomeKey)) var isOnHome = true
        var plan = CommutePlan()
        var schedule = CommuteDaySchedule()
        var direction: TransitDirection?
        var board: TransitStopBoard?
        /// When the board arrived on this device, so a dead connection can't
        /// keep a countdown running.
        var boardReceivedAt: Date?
        var loadFailed = false

        var isShown: Bool { isEnabled && isOnHome }

        var leg: CommuteLeg? {
            direction.flatMap { plan.leg(for: $0) }
        }

        func isOutdated(at now: Date) -> Bool {
            boardReceivedAt.map { now.timeIntervalSince($0) > CommuteFormat.outdatedAfter } ?? false
        }

        /// The only way a plan enters the hero, so a board never outlives the
        /// stop and lines it was asked for — whether the change was observed
        /// here or handed over by Home after a save.
        mutating func apply(_ plan: CommutePlan) {
            guard plan != self.plan else { return }
            self.plan = plan
            board = nil
            boardReceivedAt = nil
            loadFailed = false
            if plan.isEmpty { direction = nil }
        }

        /// The next arrival worth a glance, skipping buses already gone.
        func headline(now: Date) -> TransitArrival? {
            let outdated = isOutdated(at: now)
            return board?.arrivals.first { $0.latest > now || ($0.phase == .atStop && !outdated) }
        }
    }

    enum Action: Equatable {
        case task(CommuteDaySchedule)
        case planLoaded(CommutePlan)
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

    @Dependency(\.commutePlanRepository) var commutePlanRepository
    @Dependency(\.transitRepository) var transitRepository
    @Dependency(\.date) var date
    @Dependency(\.continuousClock) var clock

    static let refreshInterval: Duration = .seconds(30)

    private enum CancelID { case plan, poll }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case let .task(schedule):
                state.schedule = schedule
                guard state.isShown else { return .none }
                return .merge(observePlan(), refreshPlan())

            case let .planLoaded(plan):
                state.apply(plan)
                guard !plan.isEmpty else { return .cancel(id: CancelID.poll) }
                return poll(plan: plan, schedule: state.schedule)

            case let .directionResolved(direction):
                if direction != state.direction {
                    state.direction = direction
                    state.board = nil
                    state.boardReceivedAt = nil
                    state.loadFailed = false
                }
                return .none

            case let .boardLoaded(board):
                guard board.direction == state.direction, board.stopId == state.leg?.stopId else { return .none }
                state.loadFailed = false
                state.board = board
                state.boardReceivedAt = date.now
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

    private func observePlan() -> Effect<Action> {
        .run { send in
            for await plan in commutePlanRepository.observe() {
                await send(.planLoaded(plan))
            }
        }
        .cancellable(id: CancelID.plan, cancelInFlight: true)
    }

    /// Another device may have changed the plan; the answer lands through the
    /// observation, and a failure leaves the mirrored plan in place.
    private func refreshPlan() -> Effect<Action> {
        .run { _ in try? await commutePlanRepository.refresh() }
    }

    /// One loop for as long as Home is visible; each tick re-resolves the leg,
    /// so the line flips from going in to going home on its own.
    private func poll(plan: CommutePlan, schedule: CommuteDaySchedule) -> Effect<Action> {
        .run { send in
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

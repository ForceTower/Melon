import ComposableArchitecture
import Foundation

/// "Meu trajeto": the saved stop's next buses ("Meu ponto" on the way in,
/// "Saindo da UEFS" on the way out), polled while on screen.
@Reducer
struct CommuteBoardFeature {
    @ObservableState
    struct State: Equatable {
        var plan: CommutePlan
        var direction: TransitDirection
        var board: TransitStopBoard?
        /// When the board arrived on this device, so a dead connection can't
        /// keep a countdown running.
        var boardReceivedAt: Date?
        var network: TransitNetwork?
        var loadFailed = false
        /// The arrival "Como calculamos" explains.
        var explained: TransitArrival?
        var isExplainerPresented = false

        init(direction: TransitDirection, plan: CommutePlan) {
            self.direction = direction
            self.plan = plan
        }

        var leg: CommuteLeg? { plan.leg(for: direction) }

        func isOutdated(at now: Date) -> Bool {
            boardReceivedAt.map { now.timeIntervalSince($0) > CommuteFormat.outdatedAfter } ?? false
        }
    }

    enum Action: Equatable {
        case task
        case planLoaded(CommutePlan)
        case networkLoaded(TransitNetwork)
        case boardLoaded(TransitStopBoard)
        case boardFailed
        case directionPicked(TransitDirection)
        case arrivalTapped(TransitArrival)
        case explainTapped(TransitArrival?)
        case explainerDismissed
        case settingsTapped
        case startTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case follow(TransitArrival, TransitDirection, stopId: String)
            case editPlan(CommutePlan)
        }
    }

    @Dependency(\.commutePlanRepository) var commutePlanRepository
    @Dependency(\.transitRepository) var transitRepository
    @Dependency(\.date.now) var now
    @Dependency(\.continuousClock) var clock
    @Dependency(\.analytics) var analytics

    static let refreshInterval: Duration = .seconds(20)

    private enum CancelID { case plan, poll }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                return .merge(observePlan(), refreshPlan(), loadNetwork(), poll(state))

            case let .planLoaded(plan):
                guard plan != state.plan else { return .none }
                let previous = state.leg
                state.plan = plan
                guard state.leg != previous else { return .none }
                state.board = nil
                state.boardReceivedAt = nil
                return poll(state)

            case let .networkLoaded(network):
                state.network = network
                return .none

            case let .boardLoaded(board):
                guard board.direction == state.direction else { return .none }
                state.loadFailed = false
                state.board = board
                state.boardReceivedAt = now
                return .none

            case .boardFailed:
                state.loadFailed = true
                return .none

            case let .directionPicked(direction):
                guard direction != state.direction else { return .none }
                state.direction = direction
                state.board = nil
                state.boardReceivedAt = nil
                state.loadFailed = false
                return poll(state)

            case let .arrivalTapped(arrival):
                guard let leg = state.leg else { return .none }
                analytics.selectContent(contentType: ContentTypes.tile, itemId: "commute_follow")
                return .send(.delegate(.follow(arrival, state.direction, stopId: leg.stopId)))

            case let .explainTapped(arrival):
                state.explained = arrival ?? state.board?.arrivals.first { $0.basis != nil } ?? state.board?.arrivals.first
                state.isExplainerPresented = true
                return .none

            case .explainerDismissed:
                state.isExplainerPresented = false
                return .none

            case .settingsTapped, .startTapped:
                return .send(.delegate(.editPlan(state.plan)))

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

    /// The answer lands through the observation; a failure keeps the mirrored plan.
    private func refreshPlan() -> Effect<Action> {
        .run { _ in try? await commutePlanRepository.refresh() }
    }

    private func loadNetwork() -> Effect<Action> {
        .run { send in
            guard let network = try? await transitRepository.network() else { return }
            await send(.networkLoaded(network))
        }
    }

    private func poll(_ state: State) -> Effect<Action> {
        guard let leg = state.leg else { return .cancel(id: CancelID.poll) }
        return .run { [direction = state.direction] send in
            while true {
                do {
                    let board = try await transitRepository.arrivals(leg.stopId, direction, leg.lineNumbers)
                    await send(.boardLoaded(board))
                } catch is CancellationError {
                    return
                } catch {
                    await send(.boardFailed)
                }
                try await clock.sleep(for: Self.refreshInterval)
            }
        }
        .cancellable(id: CancelID.poll, cancelInFlight: true)
    }
}

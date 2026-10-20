import ComposableArchitecture
import Foundation

/// "Meu trajeto": the saved stop's next buses ("Meu ponto" on the way in,
/// "Saindo da UEFS" on the way out), polled while on screen.
@Reducer
struct CommuteBoardFeature {
    @ObservableState
    struct State: Equatable {
        @Shared(.commutePlan) var plan
        var direction: TransitDirection
        var board: TransitStopBoard?
        var network: TransitNetwork?
        var loadFailed = false
        /// The arrival "Como calculamos" explains.
        var explained: TransitArrival?
        var isExplainerPresented = false

        init(direction: TransitDirection) {
            self.direction = direction
        }

        var leg: CommuteLeg? { plan.leg(for: direction) }
    }

    enum Action: Equatable {
        case task
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
            case editPlan
        }
    }

    @Dependency(\.transitRepository) var transitRepository
    @Dependency(\.continuousClock) var clock
    @Dependency(\.analytics) var analytics

    static let refreshInterval: Duration = .seconds(20)

    private enum CancelID { case poll }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                guard let leg = state.leg else { return .none }
                return .merge(loadNetwork(), poll(leg: leg, direction: state.direction))

            case let .networkLoaded(network):
                state.network = network
                return .none

            case let .boardLoaded(board):
                guard board.direction == state.direction else { return .none }
                state.loadFailed = false
                state.board = board
                return .none

            case .boardFailed:
                state.loadFailed = true
                return .none

            case let .directionPicked(direction):
                guard direction != state.direction else { return .none }
                state.direction = direction
                state.board = nil
                state.loadFailed = false
                guard let leg = state.leg else { return .cancel(id: CancelID.poll) }
                return poll(leg: leg, direction: direction)

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
                return .send(.delegate(.editPlan))

            case .delegate:
                return .none
            }
        }
    }

    private func loadNetwork() -> Effect<Action> {
        .run { send in
            guard let network = try? await transitRepository.network() else { return }
            await send(.networkLoaded(network))
        }
    }

    private func poll(leg: CommuteLeg, direction: TransitDirection) -> Effect<Action> {
        .run { send in
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

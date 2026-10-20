import ComposableArchitecture
import Foundation

/// "Seguindo o 087": only this line's route and buses, the followed bus's
/// band to the student's stop, refreshed while on screen. Following a bus
/// never means the student is on it.
@Reducer
struct CommuteFollowFeature {
    @ObservableState
    struct State: Equatable {
        var lineNumber: String
        var direction: TransitDirection
        var stopId: String
        var vehicleId: String?
        /// What the board showed, so the screen opens with a number.
        var arrival: TransitArrival?
        var follow: TransitFollow?
        var route: TransitRoute?
        var loadFailed = false
        var showsMyLocation = false
        var locationDenied = false
        var isExplainerPresented = false

        init(arrival: TransitArrival, direction: TransitDirection, stopId: String) {
            lineNumber = arrival.lineNumber
            self.direction = direction
            self.stopId = stopId
            vehicleId = arrival.vehicleId
            self.arrival = arrival
        }

        var followedVehicle: TransitVehicle? {
            follow?.vehicles.first { $0.id == vehicleId }
        }

        /// Buses running this route, plus the followed one even while it is
        /// still finishing the opposite trip.
        var routeVehicles: [TransitVehicle] {
            follow?.vehicles.filter { $0.progressMeters != nil || $0.id == vehicleId } ?? []
        }
    }

    enum Action: Equatable {
        case task
        case routeLoaded(TransitRoute?)
        case followLoaded(TransitFollow)
        case followFailed
        case myLocationTapped
        case locationResolved(LocationAuthorization)
        case explainTapped
        case explainerDismissed
    }

    @Dependency(\.transitRepository) var transitRepository
    @Dependency(\.locationClient) var locationClient
    @Dependency(\.continuousClock) var clock

    static let refreshInterval: Duration = .seconds(10)

    private enum CancelID { case poll }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                return .merge(loadRoute(state), poll(state))

            case let .routeLoaded(route):
                state.route = route
                return .none

            case let .followLoaded(follow):
                state.loadFailed = false
                state.follow = follow
                if let arrival = follow.arrival {
                    state.arrival = arrival
                    state.vehicleId = arrival.vehicleId ?? state.vehicleId
                } else if state.vehicleId != nil {
                    // Gone past the stop: keep the map, drop the countdown.
                    state.arrival = nil
                }
                return .none

            case .followFailed:
                state.loadFailed = true
                return .none

            case .myLocationTapped:
                if state.showsMyLocation {
                    state.showsMyLocation = false
                    return .none
                }
                return .run { send in
                    do {
                        _ = try await locationClient.currentLocation()
                        await send(.locationResolved(.authorized))
                    } catch {
                        await send(.locationResolved(await locationClient.authorization()))
                    }
                }

            case let .locationResolved(authorization):
                state.locationDenied = authorization == .denied
                state.showsMyLocation = authorization == .authorized
                return .none

            case .explainTapped:
                state.isExplainerPresented = true
                return .none

            case .explainerDismissed:
                state.isExplainerPresented = false
                return .none
            }
        }
    }

    private func loadRoute(_ state: State) -> Effect<Action> {
        .run { [line = state.lineNumber, direction = state.direction] send in
            let network = try? await transitRepository.network()
            await send(.routeLoaded(network?.route(line: line, direction: direction)))
        }
    }

    private func poll(_ state: State) -> Effect<Action> {
        .run { [line = state.lineNumber, direction = state.direction, stopId = state.stopId, vehicleId = state.vehicleId] send in
            while true {
                do {
                    await send(.followLoaded(try await transitRepository.follow(line, direction, stopId, vehicleId)))
                } catch is CancellationError {
                    return
                } catch {
                    await send(.followFailed)
                }
                try await clock.sleep(for: Self.refreshInterval)
            }
        }
        .cancellable(id: CancelID.poll, cancelInFlight: true)
    }
}

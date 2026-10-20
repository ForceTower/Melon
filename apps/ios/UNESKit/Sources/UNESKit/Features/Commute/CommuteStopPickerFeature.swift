import ComposableArchitecture
import Foundation

/// Setup, step 2 (going in only): where the student actually boards. Search
/// by name, tap a stop on the map, or sort by distance after an explicit
/// location request.
@Reducer
struct CommuteStopPickerFeature {
    @ObservableState
    struct State: Equatable {
        var draft: CommuteSetupDraft
        var network: TransitNetwork?
        var loadFailed = false
        var query = ""
        var selectedStopId: String?
        var userLocation: GeoPoint?
        var location = LocationStatus.idle

        enum LocationStatus: Equatable {
            case idle, locating, denied, unavailable
        }

        init(draft: CommuteSetupDraft) {
            self.draft = draft
            selectedStopId = draft.existing.toCampus?.stopId
        }

        var stops: [TransitStop] {
            guard let network else { return [] }
            var stops = network.boardingStops(direction: .toCampus)
            let needle = Self.normalize(query)
            if !needle.isEmpty {
                stops = stops.filter { stop in
                    Self.normalize("\(stop.name) \(stop.area ?? "")").contains(needle)
                }
            }
            if let userLocation {
                stops.sort { $0.location.distance(to: userLocation) < $1.location.distance(to: userLocation) }
            } else if let selected = stops.firstIndex(where: { $0.id == selectedStopId }) {
                stops.insert(stops.remove(at: selected), at: 0)
            }
            return stops
        }

        var selectedStop: TransitStop? {
            network?.boardingStops(direction: .toCampus).first { $0.id == selectedStopId }
        }

        private static func normalize(_ text: String) -> String {
            text.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "pt-BR"))
                .trimmingCharacters(in: .whitespaces)
        }
    }

    enum Action: Equatable {
        case task
        case networkLoaded(TransitNetwork)
        case networkFailed
        case queryChanged(String)
        case stopSelected(String?)
        case useLocationTapped
        case locationLoaded(GeoPoint)
        case locationFailed(LocationAuthorization)
        case continueTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case next(CommuteSetupDraft, TransitStop)
        }
    }

    @Dependency(\.transitRepository) var transitRepository
    @Dependency(\.locationClient) var locationClient

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                guard state.network == nil else { return .none }
                state.loadFailed = false
                return .run { send in
                    do {
                        await send(.networkLoaded(try await transitRepository.network()))
                    } catch {
                        await send(.networkFailed)
                    }
                }

            case let .networkLoaded(network):
                state.network = network
                return .none

            case .networkFailed:
                state.loadFailed = true
                return .none

            case let .queryChanged(query):
                state.query = query
                return .none

            case let .stopSelected(stopId):
                state.selectedStopId = stopId
                return .none

            case .useLocationTapped:
                state.location = .locating
                return .run { send in
                    do {
                        await send(.locationLoaded(try await locationClient.currentLocation()))
                    } catch {
                        await send(.locationFailed(await locationClient.authorization()))
                    }
                }

            case let .locationLoaded(point):
                state.userLocation = point
                state.location = .idle
                state.query = ""
                return .none

            case let .locationFailed(authorization):
                state.location = authorization == .denied ? .denied : .unavailable
                return .none

            case .continueTapped:
                guard let stop = state.selectedStop else { return .none }
                return .send(.delegate(.next(state.draft, stop)))

            case .delegate:
                return .none
            }
        }
    }
}

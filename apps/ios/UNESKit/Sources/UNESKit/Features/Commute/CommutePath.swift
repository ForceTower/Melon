import ComposableArchitecture
import SwiftUI

/// Meu trajeto's screens, pushed on whichever stack opened the flow (Hoje or
/// Eu). `route` turns their delegates into stack changes, so both hosts move
/// through the flow the same way.
@Reducer
enum CommutePath {
    case board(CommuteBoardFeature)
    case follow(CommuteFollowFeature)
    case direction(CommuteDirectionFeature)
    case stopPicker(CommuteStopPickerFeature)
    case lines(CommuteLinesFeature)

    enum Route: Equatable {
        case push(State)
        /// The plan was saved: the setup steps give way to its board.
        case saved(CommutePlan, board: State)
        case removed
    }

    static func route(_ action: Action) -> Route? {
        switch action {
        case let .direction(.delegate(.next(draft))):
            if draft.directions.contains(.toCampus) {
                return .push(.stopPicker(CommuteStopPickerFeature.State(draft: draft)))
            }
            return .push(.lines(CommuteLinesFeature.State(draft: draft, direction: .fromCampus)))
        case .direction(.delegate(.removed)):
            return .removed
        case let .stopPicker(.delegate(.next(draft, stop))):
            return .push(.lines(CommuteLinesFeature.State(draft: draft, direction: .toCampus, stop: stop)))
        case let .lines(.delegate(.next(draft))):
            return .push(.lines(CommuteLinesFeature.State(draft: draft, direction: .fromCampus)))
        case let .lines(.delegate(.finished(plan, direction))):
            return .saved(plan, board: .board(CommuteBoardFeature.State(direction: direction, plan: plan)))
        case let .board(.delegate(.follow(arrival, direction, stopId, receivedAt))):
            return .push(.follow(CommuteFollowFeature.State(
                arrival: arrival,
                direction: direction,
                stopId: stopId,
                receivedAt: receivedAt
            )))
        case let .board(.delegate(.editPlan(plan))):
            return .push(.direction(CommuteDirectionFeature.State(plan: plan)))
        default:
            return nil
        }
    }
}

extension CommutePath.State: Equatable {}
extension CommutePath.Action: Equatable {}

struct CommutePathView: View {
    let store: StoreOf<CommutePath>

    var body: some View {
        switch store.case {
        case let .board(store):
            CommuteBoardView(store: store)
        case let .follow(store):
            CommuteFollowView(store: store)
        case let .direction(store):
            CommuteDirectionView(store: store)
        case let .stopPicker(store):
            CommuteStopPickerView(store: store)
        case let .lines(store):
            CommuteLinesView(store: store)
        }
    }
}

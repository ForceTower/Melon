import ComposableArchitecture
import Foundation

/// Explains why Hoje stopped syncing while the network re-signs the API's TLS.
/// Nothing here can fix it; the retry only checks whether the student already
/// moved to another network.
@Reducer
struct InterceptionFeature {
    @ObservableState
    struct State: Equatable {
        @Shared(.tlsInterception) var interception
        var isRetrying = false
        var isStillBlocked = false
    }

    enum Action: Equatable {
        case retryTapped
        case retryFinished
        case closeTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case connectionRestored
        }
    }

    @Dependency(\.homeRepository) var homeRepository
    @Dependency(\.date.now) var now
    @Dependency(\.dismiss) var dismiss

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .retryTapped:
                guard !state.isRetrying else { return .none }
                state.isRetrying = true
                state.isStillBlocked = false
                return .run { send in
                    try? await homeRepository.refresh(now: now)
                    await send(.retryFinished)
                }

            case .retryFinished:
                state.isRetrying = false
                // Judged by the interception, not the refresh: any response
                // clears it, even if the sync itself fails for another reason.
                guard state.interception == nil else {
                    state.isStillBlocked = true
                    return .none
                }
                return .send(.delegate(.connectionRestored))

            case .closeTapped:
                return .run { _ in await dismiss() }

            case .delegate:
                return .none
            }
        }
    }
}

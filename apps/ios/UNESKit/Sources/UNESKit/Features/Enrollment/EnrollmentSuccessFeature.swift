import ComposableArchitecture
import Foundation

/// The full-screen confirmation after a submit: the proposal is registered,
/// seat confirmation arrives later by message.
@Reducer
struct EnrollmentSuccessFeature {
    @ObservableState
    struct State: Equatable {
        @Shared(.enrollmentSession) var session

        init(session: EnrollmentSession? = nil) {
            if let session {
                $session.withLock { $0 = session }
            }
        }

        var allowsOtherCount: Int {
            session.picks.count(where: \.allowsOther)
        }
    }

    enum Action: Equatable {
        case doneTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case finished
        }
    }

    var body: some ReducerOf<Self> {
        Reduce { _, action in
            switch action {
            case .doneTapped:
                return .send(.delegate(.finished))
            case .delegate:
                return .none
            }
        }
    }
}

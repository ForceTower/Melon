import ComposableArchitecture
import Foundation

/// Proposal review and submit — or the read-only comprovante once the
/// window reports the proposal as sent.
@Reducer
struct EnrollmentReviewFeature {
    @ObservableState
    struct State: Equatable {
        @Shared(.enrollmentSession) var session
        var isSubmitting = false
        @Presents var alert: AlertState<Never>?

        init(session: EnrollmentSession? = nil) {
            if let session {
                $session.withLock { $0 = session }
            }
        }

        var isReadonly: Bool {
            session.window?.state == .closed
        }

        var canSubmit: Bool {
            session.blockers.isEmpty && !isReadonly
        }
    }

    enum Action: Equatable {
        case removeTapped(Int64)
        case allowsOtherChanged(Int64, Bool)
        case waitlistChanged(Int64, Bool)
        case saveTapped
        case submitTapped
        case submitSucceeded
        case submitFailed(String)
        case timetableTapped
        case alert(PresentationAction<Never>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openTimetable
            case submitted
        }
    }

    @Dependency(\.enrollmentRepository) var enrollmentRepository
    @Dependency(\.dismiss) var dismiss

    private enum CancelID { case submit }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case let .removeTapped(disciplineId):
                state.$session.withLock { $0.remove(disciplineId: disciplineId) }
                return .none

            case let .allowsOtherChanged(disciplineId, value):
                state.$session.withLock { $0.setAllowsOther(value, disciplineId: disciplineId) }
                return .none

            case let .waitlistChanged(disciplineId, value):
                state.$session.withLock { $0.setWaitlist(value, disciplineId: disciplineId) }
                return .none

            case .saveTapped:
                // The draft already lives in the shared session; "salvar
                // rascunho" just steps back out of the review.
                return .run { _ in await dismiss() }

            case .submitTapped:
                guard state.canSubmit, !state.isSubmitting else { return .none }
                state.isSubmitting = true
                return .run { [selections = state.session.selections] send in
                    try await enrollmentRepository.submit(selections)
                    await send(.submitSucceeded)
                } catch: { error, send in
                    await send(.submitFailed(EnrollmentFormat.message(for: error)))
                }
                .cancellable(id: CancelID.submit, cancelInFlight: true)

            case .submitSucceeded:
                state.isSubmitting = false
                // The backend finalized the step; mirror it locally so the
                // entry screen flips to "proposta enviada" without a refetch.
                state.$session.withLock { $0.window?.state = .closed }
                return .send(.delegate(.submitted))

            case let .submitFailed(message):
                state.isSubmitting = false
                state.alert = AlertState {
                    TextState("Não deu para enviar a proposta")
                } message: {
                    TextState(message)
                }
                return .none

            case .timetableTapped:
                return .send(.delegate(.openTimetable))

            case .alert:
                return .none

            case .delegate:
                return .none
            }
        }
        .ifLet(\.$alert, action: \.alert)
    }
}

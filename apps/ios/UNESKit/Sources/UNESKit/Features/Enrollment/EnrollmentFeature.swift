import ComposableArchitecture
import Foundation

/// The matrícula entry screen: resolves the window, loads the offers tree
/// behind it, and seeds the shared session every other step edits. Pushes
/// stay on the Me stack — this feature only emits routing delegates.
@Reducer
struct EnrollmentFeature {
    @ObservableState
    struct State: Equatable {
        @Shared(.enrollmentSession) var session
        /// Identity strip garnish; seeded from the Me hub, fetched if absent.
        var profile: Profile?
        var isLoading = false
        var errorMessage: String?
        /// "Now" pinned at the latest appearance — the countdown's anchor.
        var referenceDate = Date.distantPast
        var hasLoaded = false

        init(profile: Profile? = nil, session: EnrollmentSession? = nil) {
            self.profile = profile
            if let session {
                $session.withLock { $0 = session }
                hasLoaded = true
            }
        }
    }

    enum Action: Equatable {
        case task
        case retryTapped
        case windowLoaded(EnrollmentWindow?)
        case offersLoaded([EnrollmentDiscipline])
        case loadFailed(String)
        case profileLoaded(Profile)
        /// Montar / continuar / reabrir — every path into the catalogue.
        case startTapped
        /// Revisar or "Ver comprovante" once the proposal is sent.
        case reviewTapped
        case proposalRowTapped(Int64)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openOffers
            case openReview
            case openDiscipline(Int64)
        }
    }

    @Dependency(\.enrollmentRepository) var enrollmentRepository
    @Dependency(\.profileRepository) var profileRepository
    @Dependency(\.date.now) var now

    private let log = Log.scoped("EnrollmentFeature")

    private enum CancelID { case load }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                state.referenceDate = now
                guard !state.hasLoaded else { return .none }
                state.hasLoaded = true
                state.isLoading = true
                state.errorMessage = nil
                // A previous run's session is upstream truth no longer.
                state.$session.withLock { $0 = EnrollmentSession() }
                return .merge(load(), loadProfileIfNeeded(state))

            case .retryTapped:
                state.isLoading = true
                state.errorMessage = nil
                log.info("enrollment retry tapped")
                return load()

            case let .windowLoaded(window):
                state.$session.withLock { $0.window = window }
                if window == nil {
                    state.isLoading = false
                }
                return .none

            case let .offersLoaded(disciplines):
                state.isLoading = false
                state.$session.withLock {
                    $0.disciplines = disciplines
                    $0.preseedFromSavedProposal()
                }
                return .none

            case let .loadFailed(message):
                state.isLoading = false
                state.errorMessage = message
                return .none

            case let .profileLoaded(profile):
                state.profile = profile
                return .none

            case .startTapped:
                return .send(.delegate(.openOffers))

            case .reviewTapped:
                return .send(.delegate(.openReview))

            case let .proposalRowTapped(disciplineId):
                return .send(.delegate(.openDiscipline(disciplineId)))

            case .delegate:
                return .none
            }
        }
    }

    /// Window first (cheap gate), then the heavy offers tree only when a
    /// window actually exists.
    private func load() -> Effect<Action> {
        .run { send in
            let window = try await enrollmentRepository.window()
            await send(.windowLoaded(window))
            guard window != nil else { return }
            let disciplines = try await enrollmentRepository.offers()
            await send(.offersLoaded(disciplines))
        } catch: { [log] error, send in
            log.warn("enrollment load failed err=\(EnrollmentFormat.message(for: error))", error: error)
            await send(.loadFailed(EnrollmentFormat.message(for: error)))
        }
        .cancellable(id: CancelID.load, cancelInFlight: true)
    }

    private func loadProfileIfNeeded(_ state: State) -> Effect<Action> {
        guard state.profile == nil else { return .none }
        return .run { [log] send in
            // Only feeds the identity strip — ignore failures.
            do {
                let profile = try await profileRepository.current()
                await send(.profileLoaded(profile))
            } catch {
                log.debug("enrollment profile load failed")
            }
        }
    }
}

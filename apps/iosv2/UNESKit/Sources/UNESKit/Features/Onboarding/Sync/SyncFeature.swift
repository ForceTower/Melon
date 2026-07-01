import ComposableArchitecture
import Foundation

/// Runs the initial sync as six user-visible steps. Steps are tolerant of
/// transient failures (marked done and skipped); a 401 aborts to login.
@Reducer
struct SyncFeature {
    enum Step: Int, CaseIterable, Equatable, Sendable {
        case auth, profile, schedule, classes, grades, messages

        var label: String {
            switch self {
            case .auth: "Verificando matrícula"
            case .profile: "Carregando seu perfil"
            case .schedule: "Montando seu horário"
            case .classes: "Conectando às turmas"
            case .grades: "Baixando notas do semestre"
            case .messages: "Sincronizando recados"
            }
        }
    }

    @ObservableState
    struct State: Equatable {
        /// Typed SAGRES username, or "estudante" after a passkey login.
        let greeting: String
        var completedSteps = 0
        var profile: Profile?
        var overview: ReadyOverview?

        var progress: Double {
            Double(completedSteps) / Double(Step.allCases.count)
        }

        func phase(of step: Step) -> StepPhase {
            if step.rawValue < completedSteps { .done }
            else if step.rawValue == completedSteps { .active }
            else { .pending }
        }

        enum StepPhase {
            case done, active, pending
        }
    }

    enum Action: Equatable {
        case task
        case stepCompleted(Step)
        case profileLoaded(Profile)
        case overviewLoaded(ReadyOverview)
        case finished
        case authFailed
        case delegate(Delegate)

        enum Delegate: Equatable {
            case done(profile: Profile?, overview: ReadyOverview?)
            case authFailed
        }
    }

    @Dependency(\.syncRepository) var sync
    @Dependency(\.profileRepository) var profileRepository
    @Dependency(\.continuousClock) var clock
    @Dependency(\.date.now) var now

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                guard state.completedSteps == 0 else { return .none }
                return .run { send in
                    do {
                        try await runSync(send: send)
                    } catch {
                        await send(.authFailed)
                    }
                }

            case let .stepCompleted(step):
                state.completedSteps = step.rawValue + 1
                return .none

            case let .profileLoaded(profile):
                state.profile = profile
                return .none

            case let .overviewLoaded(overview):
                state.overview = overview
                return .none

            case .finished:
                return .send(.delegate(.done(profile: state.profile, overview: state.overview)))

            case .authFailed:
                return .send(.delegate(.authFailed))

            case .delegate:
                return .none
            }
        }
    }

    /// Throws only on authentication failure; anything else marks the step
    /// done and moves on.
    private func runSync(send: Send<Action>) async throws {
        func step<T>(_ step: Step, _ work: () async throws -> T) async throws -> T? {
            do {
                let value = try await work()
                await send(.stepCompleted(step))
                return value
            } catch let error as APIError where error.isAuthFailure {
                throw error
            } catch {
                await send(.stepCompleted(step))
                return nil
            }
        }

        // 1. auth — validates the token server-side
        _ = try await step(.auth) { try await sync.ping() }

        // 2. profile
        if let profile = try await step(.profile, { try await profileRepository.current() }) {
            await send(.profileLoaded(profile))
        }

        // 3. schedule — wait for phase 1 to reach a terminal state, then
        // resolve the active semester
        let semester = try await step(.schedule) { () -> Semester? in
            try await poll(upTo: 20) { $0.initial.state.isTerminal }
            return try await sync.semesters().active(today: now.dayStamp)
        } ?? nil

        // 4. classes — gate until class data is actually applied
        _ = try await step(.classes) {
            try await poll(upTo: 200) { $0.initial.appliedSemesters > 0 || $0.initial.state == .failed }
        }

        // 5. grades — pull the full active-semester snapshot (grades included)
        if let semester {
            if let overview = try await step(.grades, { try await sync.readyOverview(semester, now) }) {
                await send(.overviewLoaded(overview))
            }
        } else {
            await send(.stepCompleted(.grades))
        }

        // 6. messages
        _ = try await step(.messages) { try await sync.fetchFirstMessagesPage() }

        try await clock.sleep(for: .milliseconds(750))
        await send(.finished)
    }

    /// Polls the onboarding status until `until` is satisfied or `upTo` ticks
    /// elapse. Transient errors keep polling; auth failures abort.
    private func poll(upTo ticks: Int, until: (OnboardingStatus) -> Bool) async throws {
        for _ in 0..<ticks {
            do {
                let status = try await sync.onboardingStatus()
                if until(status) { return }
            } catch let error as APIError where error.isAuthFailure {
                throw error
            } catch {}
            try await clock.sleep(for: .milliseconds(1500))
        }
    }
}

extension APIError {
    var isAuthFailure: Bool {
        if case .server(status: 401, _) = self { return true }
        return false
    }
}

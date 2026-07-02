import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

private let activeSemester = Semester(
    id: "sem-1",
    code: "2026.1",
    description: "Semestre 2026.1",
    startDate: "2020-01-01",
    endDate: "2030-12-31"
)

@MainActor
struct SyncFeatureTests {
    @Test
    func happyPathCompletesEveryStep() async {
        let clock = TestClock()
        let store = TestStore(initialState: SyncFeature.State(greeting: "mariana.souza")) {
            SyncFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.date = .constant(Date(timeIntervalSince1970: 1_782_864_000))
            $0.profileRepository.current = { @Sendable in .preview }
            $0.push.registerStoredToken = {}
            $0.syncRepository = SyncRepository(
                ping: {},
                onboardingStatus: {
                    OnboardingStatus(
                        courseLinked: true,
                        initial: .init(state: .done, appliedSemesters: 1),
                        semesters: .init(state: .running),
                        messages: .init(state: .done),
                        activeSemesterReady: true
                    )
                },
                semesters: { [activeSemester] },
                readyOverview: { semester, _ in
                    #expect(semester == activeSemester)
                    return .preview
                },
                fetchFirstMessagesPage: {}
            )
        }

        await store.send(.task)
        await store.receive(.stepCompleted(.auth)) {
            $0.completedSteps = 1
        }
        await store.receive(.stepCompleted(.profile)) {
            $0.completedSteps = 2
        }
        await store.receive(.profileLoaded(.preview)) {
            $0.profile = .preview
        }
        await store.receive(.stepCompleted(.schedule)) {
            $0.completedSteps = 3
        }
        await store.receive(.stepCompleted(.classes)) {
            $0.completedSteps = 4
        }
        await store.receive(.stepCompleted(.grades)) {
            $0.completedSteps = 5
        }
        await store.receive(.overviewLoaded(.preview)) {
            $0.overview = .preview
        }
        await store.receive(.stepCompleted(.messages)) {
            $0.completedSteps = 6
        }

        await clock.advance(by: .milliseconds(750))
        await store.receive(.finished)
        await store.receive(.delegate(.done(profile: .preview, overview: .preview)))
    }

    @Test
    func expiredSessionAbortsToLogin() async {
        let store = TestStore(initialState: SyncFeature.State(greeting: "mariana.souza")) {
            SyncFeature()
        } withDependencies: {
            $0.syncRepository.ping = { @Sendable in
                throw APIError.server(status: 401, message: "Unauthorized")
            }
        }

        await store.send(.task)
        await store.receive(.authFailed)
        await store.receive(.delegate(.authFailed))
    }

    @Test
    func transientFailuresStillCompleteTheFlow() async {
        let clock = TestClock()
        let store = TestStore(initialState: SyncFeature.State(greeting: "mariana.souza")) {
            SyncFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.date = .constant(Date(timeIntervalSince1970: 1_782_864_000))
            $0.profileRepository.current = { @Sendable in throw APIError.server(status: 500, message: nil) }
            $0.push.registerStoredToken = {}
            $0.syncRepository = SyncRepository(
                ping: {},
                onboardingStatus: { throw APIError.server(status: 500, message: nil) },
                semesters: { throw APIError.server(status: 500, message: nil) },
                readyOverview: { _, _ in .preview },
                fetchFirstMessagesPage: {}
            )
        }

        await store.send(.task)
        await store.receive(.stepCompleted(.auth)) {
            $0.completedSteps = 1
        }
        await store.receive(.stepCompleted(.profile)) {
            $0.completedSteps = 2
        }

        // The schedule step polls through its 20-tick budget…
        await clock.advance(by: .seconds(30))
        await store.receive(.stepCompleted(.schedule)) {
            $0.completedSteps = 3
        }

        // …and the classes gate through its 200-tick budget.
        await clock.advance(by: .seconds(300))
        await store.receive(.stepCompleted(.classes)) {
            $0.completedSteps = 4
        }

        // No active semester resolved — grades step has nothing to fetch.
        await store.receive(.stepCompleted(.grades)) {
            $0.completedSteps = 5
        }
        await store.receive(.stepCompleted(.messages)) {
            $0.completedSteps = 6
        }

        await clock.advance(by: .milliseconds(750))
        await store.receive(.finished)
        await store.receive(.delegate(.done(profile: nil, overview: nil)))
    }
}

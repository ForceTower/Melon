import ComposableArchitecture
import Foundation

/// The initial-sync surface of apps/api: session ping, backfill status
/// polling, and the active-semester snapshot the Ready screen presents.
@DependencyClient
struct SyncRepository: Sendable {
    var ping: @Sendable () async throws -> Void
    var onboardingStatus: @Sendable () async throws -> OnboardingStatus
    var semesters: @Sendable () async throws -> [Semester]
    var readyOverview: @Sendable (_ semester: Semester, _ now: Date) async throws -> ReadyOverview
    var fetchFirstMessagesPage: @Sendable () async throws -> Void
    var backfillMirror: @Sendable () async throws -> Void
}

extension SyncRepository: TestDependencyKey {
    static let testValue = SyncRepository()

    static let previewValue = SyncRepository(
        ping: {},
        onboardingStatus: {
            OnboardingStatus(
                courseLinked: true,
                initial: .init(state: .done, appliedSemesters: 1),
                semesters: .init(state: .running, total: 8, done: 3, failed: 0),
                messages: .init(state: .done),
                activeSemesterReady: true
            )
        },
        semesters: {
            [Semester(id: "preview", code: "2026.1", description: "Semestre 2026.1", startDate: "2026-03-01", endDate: "2026-08-01")]
        },
        readyOverview: { _, _ in .preview },
        fetchFirstMessagesPage: {},
        backfillMirror: {}
    )
}

extension DependencyValues {
    var syncRepository: SyncRepository {
        get { self[SyncRepository.self] }
        set { self[SyncRepository.self] = newValue }
    }
}

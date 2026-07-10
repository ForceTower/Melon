/// Server-side progress of the two-phase initial backfill, polled from
/// `api/sync/onboarding-status` while the sync screen is up.
struct OnboardingStatus: Equatable, Sendable {
    var courseLinked: Bool
    var initial: InitialStatus
    var semesters: PhaseStatus
    var messages: PhaseStatus
    var activeSemesterReady: Bool

    enum PhaseState: Equatable, Sendable {
        case pending, running, done, partial, failed, unknown

        var isTerminal: Bool {
            switch self {
            case .done, .partial, .failed: true
            case .pending, .running, .unknown: false
            }
        }
    }

    /// Phase 1 — current semester classes + first messages page.
    struct InitialStatus: Equatable, Sendable {
        var state: PhaseState
        /// Distinct semesters that already have class data applied. The grades
        /// step gates on this going above zero.
        var appliedSemesters: Int
    }

    /// Phase 2 — historical semesters / full messages backfill.
    struct PhaseStatus: Equatable, Sendable {
        var state: PhaseState
        var total = 0
        var done = 0
        var failed = 0
    }
}

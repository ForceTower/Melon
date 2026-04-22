package dev.forcetower.melon.core.sync.domain.model

// Snapshot of the server-side backfill progress for a freshly-linked student.
// Polled by SyncView's retry gates to decide whether an empty profile/list
// reflects "still warming up" vs. "genuinely empty / failed".
data class OnboardingStatus(
    val courseLinked: Boolean,
    // Phase 1 (current semester + first 20 messages). The iOS classes/msgs
    // steps gate on this — Phase 2 historicals don't block the client.
    val initial: InitialStatus,
    val semesters: PhaseStatus,
    val messages: PhaseStatus,
    // True once Phase 1 is terminal (done or failed). Equivalent to
    // `initial.state in {Done, Failed}` but kept as a top-level flag so
    // the gate's single-field read doesn't need to know about enums.
    val activeSemesterReady: Boolean,
) {
    // Phase 1 is singular (one job per student), so no total/done/failed —
    // just the state the gate cares about plus the number of semesters
    // the student has actually synced class data in. The iOS `grades`
    // step waits for appliedSemesters > 0 before advancing.
    data class InitialStatus(
        val state: PhaseStatus.State,
        val appliedSemesters: Int,
    )

    data class PhaseStatus(
        val state: State,
        val total: Int = 0,
        val done: Int = 0,
        val failed: Int = 0,
    ) {
        // Mirrors the server's OnboardingPhaseStatus union. Messages never
        // emits Partial — collapsed to Done for the iOS gate's purposes.
        enum class State { Pending, Running, Done, Partial, Failed, Unknown }
    }
}

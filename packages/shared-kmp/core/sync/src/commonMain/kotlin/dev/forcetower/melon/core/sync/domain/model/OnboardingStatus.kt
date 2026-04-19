package dev.forcetower.melon.core.sync.domain.model

// Snapshot of the server-side backfill progress for a freshly-linked student.
// Polled by SyncView's retry gates to decide whether an empty profile/list
// reflects "still warming up" vs. "genuinely empty / failed".
data class OnboardingStatus(
    val courseLinked: Boolean,
    val semesters: PhaseStatus,
    val messages: PhaseStatus,
) {
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

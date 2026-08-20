package dev.forcetower.unes.review

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

internal data class ReviewState(
    // Distinct calendar days the student reached the authenticated shell.
    val activeDays: Int = 0,
    val lastActiveDay: Long = 0L,
    val lastPromptedAtMs: Long = 0L,
    val lastTroubleAtMs: Long = 0L,
)

internal object ReviewPolicy {
    // Just above Play's own (undocumented, ~monthly) quota, so a sheet Play
    // swallowed silently still gets another chance the same semester.
    val Cooldown: Duration = 45.days
    val TroubleWindow: Duration = 7.days
    const val MinActiveDays: Int = 3
}

// Blank means every trigger — the shipped state, and what an unpublished lever
// key resolves to. Unknown tokens are ignored.
internal fun parseReviewTriggers(raw: String): Set<ReviewTrigger> {
    if (raw.isBlank()) return ReviewTrigger.entries.toSet()
    return raw.split(',').mapNotNull(ReviewTrigger::fromTag).toSet()
}

// `now` is wall-clock: a clock moved backwards makes the windows negative,
// which reads as "too recent" and suppresses — the safe direction.
internal fun shouldRequestReview(
    trigger: ReviewTrigger,
    state: ReviewState,
    now: Long,
    enabled: Boolean,
    allowedTriggers: Set<ReviewTrigger>,
): Boolean {
    if (!enabled) return false
    if (trigger !in allowedTriggers) return false
    if (state.activeDays < ReviewPolicy.MinActiveDays) return false
    if (now - state.lastTroubleAtMs < ReviewPolicy.TroubleWindow.inWholeMilliseconds) return false
    if (now - state.lastPromptedAtMs < ReviewPolicy.Cooldown.inWholeMilliseconds) return false
    return true
}

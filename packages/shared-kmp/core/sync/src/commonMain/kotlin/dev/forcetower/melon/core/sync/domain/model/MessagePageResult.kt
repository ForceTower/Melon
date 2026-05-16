package dev.forcetower.melon.core.sync.domain.model

// Result of a single page pull. The repo has already applied the page to the
// local mirror by the time this returns; the caller uses `nextCursor` to
// decide whether to keep paging on a background task.
data class MessagePageResult(
    val appliedCount: Int,
    val nextCursor: String?,
)

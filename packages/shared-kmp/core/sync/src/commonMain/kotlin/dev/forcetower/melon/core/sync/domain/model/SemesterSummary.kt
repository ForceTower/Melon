package dev.forcetower.melon.core.sync.domain.model

// Domain projection of a semester list row. `dirtyAt` is carried through as
// debug / observability info; clients use it for display or audit, not as the
// primary freshness gate.
data class SemesterSummary(
    val id: String,
    val code: String,
    val desc: String,
    val startDate: String,
    val endDate: String,
    val track: String?,
    val dirtyAt: String?,
)

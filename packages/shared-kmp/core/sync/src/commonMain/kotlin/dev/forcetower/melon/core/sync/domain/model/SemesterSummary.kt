package dev.forcetower.melon.core.sync.domain.model

// Domain projection of a semester list row. `dirtyAt` is the server-side
// freshness signal — the worker bumps it whenever it applies this student's
// semester subtree, and refresh re-pulls a mirrored semester whose value
// moved past the one last applied (see MirrorRepository).
data class SemesterSummary(
    val id: String,
    val code: String,
    val desc: String,
    val startDate: String,
    val endDate: String,
    val track: String?,
    val dirtyAt: String?,
)

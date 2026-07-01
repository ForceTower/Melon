package dev.forcetower.melon.feature.schedule.domain.internal

import dev.forcetower.melon.core.database.entity.SemesterEntity

// Picks the semester whose [startDate, endDate] contains `todayIso`, falling
// back to the most recently started one. Mirrors the overview rule so "current
// semester" stays consistent across features.
internal fun pickActiveSemester(
    all: List<SemesterEntity>,
    todayIso: String,
): SemesterEntity? {
    if (all.isEmpty()) return null
    val active = all.firstOrNull { it.startDate <= todayIso && todayIso <= it.endDate }
    if (active != null) return active
    return all.maxByOrNull { it.startDate }
}

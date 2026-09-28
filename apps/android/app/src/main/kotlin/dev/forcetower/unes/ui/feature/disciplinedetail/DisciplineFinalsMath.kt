package dev.forcetower.unes.ui.feature.disciplinedetail

import kotlin.math.ceil
import kotlin.math.floor

// Prova Final math — mirrors iOS `DisciplineRules`. The final mean is
// 0,6 × semester mean + 0,4 × final exam, and 5,0 closes the discipline.
// The university truncates grades to one decimal, so the semester mean is
// floored before the formula and the required grade is ceiled after — a
// student scoring the shown value must still clear the cutoff post-truncation.
internal object DisciplineFinalsMath {
    private const val FINAL_CUTOFF = 5.0
    private const val SEMESTER_WEIGHT = 0.6
    private const val EXAM_WEIGHT = 0.4

    const val DIRECT_PASS_THRESHOLD = 7.0

    fun neededFinalGrade(partialAverage: Double): Double {
        val avg = floorToTenth(partialAverage)
        val needed = (FINAL_CUTOFF - SEMESTER_WEIGHT * avg) / EXAM_WEIGHT
        return ceilToTenth(needed).coerceIn(0.0, 10.0)
    }

    // 6.95 → 6.9. The epsilon absorbs IEEE artifacts (8.7 * 10 == 86.999…).
    fun floorToTenth(value: Double): Double = floor(value * 10 + 1e-9) / 10.0

    // 6.47 → 6.5.
    fun ceilToTenth(value: Double): Double = ceil(value * 10 - 1e-9) / 10.0
}

package dev.forcetower.unes.ui.feature.materials

import android.icu.text.CompactDecimalFormat
import java.time.LocalDate
import java.time.Month
import java.util.Locale

internal object MaterialsFormat {
    // "2026.1" through June, "2026.2" after — mirrors iOS
    // `MaterialsFormat.currentSemester`.
    fun currentSemester(today: LocalDate = LocalDate.now()): String {
        val half = if (today.month <= Month.JUNE) 1 else 2
        return "${today.year}.$half"
    }

    // Five semesters newest-first starting at the current one — the upload
    // wizard's chip options.
    fun uploadSemesters(count: Int = 5): List<String> {
        var (year, half) = currentSemester().split(".").map { it.toInt() }
        return List(count) {
            val label = "$year.$half"
            if (half == 1) { year -= 1; half = 2 } else half = 1
            label
        }
    }

    // Locale-aware "1,2 mil" style compact count for the download tally.
    fun compactCount(value: Int): String {
        if (value < 1000) return value.toString()
        return CompactDecimalFormat.getInstance(
            Locale.getDefault(),
            CompactDecimalFormat.CompactStyle.SHORT,
        ).format(value)
    }
}

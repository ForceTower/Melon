package dev.forcetower.melon.feature.paradoxo.domain.model

import kotlin.math.roundToInt
import kotlin.math.sqrt

// Grade-severity band keyed by a 0–10 mean. Thresholds mirror iOS
// `ParadoxoTier(mean:)` — the labels (anjo/justo/equilibrado/exigente/
// implacável) and tones live on the presentation side.
enum class ParadoxoTier {
    Angel,
    Fair,
    Balanced,
    Demanding,
    Relentless,
    ;

    companion object {
        fun of(mean: Double): ParadoxoTier = when {
            mean >= 8.5 -> Angel
            mean >= 7.0 -> Fair
            mean >= 5.5 -> Balanced
            mean >= 3.5 -> Demanding
            else -> Relentless
        }
    }
}

enum class ParadoxoShapeKind {
    Bimodal,
    Strict,
    Lenient,
    Balanced,
    Regular,
}

// Client-side statistics over the server aggregates. Everything else
// (pulse selection, rankings, myPercentile) is server-computed. Mirrors
// iOS `ParadoxoStats`.
object ParadoxoStats {

    // Classifies an 11-bucket (grade 0…10) share distribution by where its
    // mass sits: two interior peaks above 8% → bimodal; single peak at the
    // low/high end → strict/lenient; weighted mean in the 6–7.5 band →
    // balanced; anything else → regular.
    fun shapeKind(distribution: List<Double>): ParadoxoShapeKind {
        if (distribution.size < 3) return ParadoxoShapeKind.Regular
        var peaks = 0
        for (i in 1 until distribution.size - 1) {
            val share = distribution[i]
            if (share > distribution[i - 1] && share > distribution[i + 1] && share > 0.08) {
                peaks++
            }
        }
        val peakIndex = distribution.indices.maxBy { distribution[it] }
        val weightedMean = distribution.withIndex().sumOf { (i, share) -> share * i }
        return when {
            peaks >= 2 -> ParadoxoShapeKind.Bimodal
            peakIndex <= 2 -> ParadoxoShapeKind.Strict
            peakIndex >= 8 -> ParadoxoShapeKind.Lenient
            weightedMean in 6.0..7.5 -> ParadoxoShapeKind.Balanced
            else -> ParadoxoShapeKind.Regular
        }
    }

    // Share of students scoring strictly below the bucket `grade` rounds
    // into, as a whole percent. Callers display the inverse ("top X%").
    fun percentile(distribution: List<Double>, grade: Double): Int {
        if (distribution.isEmpty()) return 0
        val bucket = grade.roundToInt().coerceIn(0, distribution.size - 1)
        val below = distribution.take(bucket).sum()
        return (below * 100).roundToInt()
    }

    // Semester-to-semester steadiness: 100 at zero deviation, hitting 0 once
    // the population stddev of the means reaches 2.5 grade points. Null when
    // there aren't at least two semesters to compare.
    fun consistency(history: List<Double>): Int? {
        if (history.size < 2) return null
        val mean = history.sum() / history.size
        val variance = history.sumOf { (it - mean) * (it - mean) } / history.size
        val score = (1.0 - sqrt(variance) / 2.5) * 100
        return score.roundToInt().coerceIn(0, 100)
    }

    fun approvalPercent(approved: Int, failed: Int, quit: Int): Int {
        val total = approved + failed + quit
        if (total == 0) return 0
        return (approved * 100.0 / total).roundToInt()
    }
}

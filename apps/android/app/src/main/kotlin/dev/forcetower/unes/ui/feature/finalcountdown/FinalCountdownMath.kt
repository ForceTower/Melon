package dev.forcetower.unes.ui.feature.finalcountdown

import kotlin.math.ceil
import kotlin.math.floor

// Grade-calculator math, ported from `FinalCountdownMath.swift` (iOS) which
// in turn ports `screens-final-countdown.jsx`.
//
// UEFS-style rules:
// - média ≥ 7,0           → aprovação direta
// - 3,0 ≤ média < 7,0     → Final; precisa de F tal que 0,6·m + 0,4·F ≥ 5
// - média < 3,0           → reprovação direta, sem direito a Final
internal object FinalCountdownMath {
    const val PassThreshold: Double = 7.0
    const val FailThreshold: Double = 3.0
    const val FinalCutoff: Double = 5.0
    const val FinalAvgWeight: Double = 0.6
    const val FinalExamWeight: Double = 0.4

    // Simple or weighted mean of the rows whose score is set. Returns null
    // when nothing's been filled.
    fun average(rows: List<FCRow>, weighted: Boolean): Double? =
        mean(rows.mapNotNull { row -> row.score?.let { it to row.weight } }, weighted)

    // What would the average be if every missing score came back as
    // `wildcardValue`? Used for best- and worst-case projections.
    fun projectAverage(rows: List<FCRow>, weighted: Boolean, wildcardValue: Double): Double? =
        mean(rows.map { (it.score ?: wildcardValue) to it.weight }, weighted)

    private fun mean(entries: List<Pair<Double, Int>>, weighted: Boolean): Double? {
        if (entries.isEmpty()) return null
        if (!weighted) {
            return entries.sumOf { it.first } / entries.size
        }
        val wsum = entries.sumOf { it.second }
        if (wsum <= 0) return null
        return entries.sumOf { it.first * it.second } / wsum
    }

    // Grade required on the Final to close at `FinalCutoff` (5). Solved from
    // 0,6·m + 0,4·F ≥ 5 → F ≥ (5 − 0,6·m) / 0,4.
    fun neededFinal(avg: Double): Double = (FinalCutoff - FinalAvgWeight * avg) / FinalExamWeight

    // Truncate to one decimal. Matches how the university records grades — a
    // raw 6,95 becomes 6,9, not 7,0. The epsilon absorbs IEEE noise so a
    // clean tenth (6.6 * 10 == 65.999…) doesn't floor down (iOS parity).
    fun floorToTenth(value: Double): Double = floor(value * 10 + 1e-9) / 10

    // Round up to one decimal. Used for "grade needed" values so that a raw
    // requirement of 6,47 surfaces as 6,5 — the student scoring exactly the
    // displayed value still clears the cutoff after truncation. The epsilon
    // keeps an exact tenth (2.6 * 10 == 26.000…004) from ceiling up to 2,7.
    fun ceilToTenth(value: Double): Double = ceil(value * 10 - 1e-9) / 10

    // If exactly one row is missing, the score it would need to hit `target`
    // overall. Returns null when the count doesn't match the single-empty
    // shape — the UI shows a projection range instead.
    fun neededForPass(rows: List<FCRow>, weighted: Boolean, target: Double = PassThreshold): Double? {
        val empties = rows.filter { it.score == null }
        if (empties.size != 1) return null
        if (!weighted) {
            val known = rows.filter { it.score != null }
            val n = rows.size.toDouble()
            val sumKnown = known.sumOf { it.score ?: 0.0 }
            return target * n - sumKnown
        }
        val wsum = rows.sumOf { it.weight }.toDouble()
        val sumKnown = rows.sumOf { (it.score ?: 0.0) * it.weight }
        val emptyWeight = empties[0].weight.toDouble()
        return (target * wsum - sumKnown) / emptyWeight
    }

    fun verdict(rows: List<FCRow>, weighted: Boolean): FCVerdict {
        val allFilled = rows.isNotEmpty() && rows.all { it.score != null }
        val rawAvg = average(rows, weighted)
            ?: return FCVerdict(kind = FCVerdictKind.Empty, avg = null)

        // Everything compared or displayed uses the truncated grade — a raw
        // 6,95 never gets rounded up to 7,0 and sneaks past the cutoff.
        val avg = floorToTenth(rawAvg)

        if (allFilled) {
            if (avg >= PassThreshold) {
                return FCVerdict(kind = FCVerdictKind.Passed, avg = avg)
            }
            if (avg < FailThreshold) {
                return FCVerdict(kind = FCVerdictKind.Failed, avg = avg)
            }
            val need = ceilToTenth(neededFinal(avg))
            return if (need > 10) {
                FCVerdict(kind = FCVerdictKind.Impossible, avg = avg, need = need)
            } else {
                FCVerdict(kind = FCVerdictKind.Final, avg = avg, need = need)
            }
        }

        // Partial: project best/worst and solve for the single-empty case.
        val best = projectAverage(rows, weighted, 10.0)?.let(::floorToTenth)
        val worst = projectAverage(rows, weighted, 0.0)?.let(::floorToTenth)
        val wildcardNeeded = neededForPass(rows, weighted)?.let(::ceilToTenth)

        if (best != null && best < FailThreshold) {
            return FCVerdict(
                kind = FCVerdictKind.FailingTrack,
                avg = avg,
                best = best,
                worst = worst,
            )
        }
        if (worst != null && worst >= PassThreshold) {
            return FCVerdict(
                kind = FCVerdictKind.Passed,
                avg = worst,
                best = best,
                worst = worst,
            )
        }
        if (wildcardNeeded == null) {
            return FCVerdict(kind = FCVerdictKind.OnTrack, avg = avg, best = best, worst = worst)
        }
        return if (wildcardNeeded <= 10) {
            FCVerdict(
                kind = FCVerdictKind.Borderline,
                avg = avg,
                best = best,
                worst = worst,
                wildcardNeeded = wildcardNeeded,
            )
        } else {
            FCVerdict(
                kind = FCVerdictKind.BorderlineFinal,
                avg = avg,
                best = best,
                worst = worst,
                wildcardNeeded = wildcardNeeded,
            )
        }
    }

    // pt-BR grade formatting — one decimal, comma separator, `—` for null.
    fun formatGrade(value: Double?): String {
        if (value == null || value.isNaN()) return "—"
        return "%.1f".format(value).replace('.', ',')
    }
}

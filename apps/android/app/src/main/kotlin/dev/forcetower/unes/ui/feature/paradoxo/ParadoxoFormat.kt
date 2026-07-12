package dev.forcetower.unes.ui.feature.paradoxo

import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoPulseFact
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoPulseKind
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.floor

// Number formatting for the aggregates. Grades truncate to one decimal
// (6,95 → 6,9 — never round toward approval, mirrors iOS `formatGrade`);
// counts group by the device locale ("6.868" in pt-BR).
internal object ParadoxoFormat {

    fun grade(value: Double?): String {
        if (value == null || value.isNaN()) return "—"
        val truncated = floor(value * 10) / 10.0
        return "%.1f".format(truncated).replace('.', ',')
    }

    // True minus sign, matching iOS `signedGrade` ("+1,8" / "−2,3").
    fun signedGrade(value: Double): String {
        val sign = if (value < 0) "−" else "+"
        return sign + grade(abs(value))
    }

    fun count(value: Int): String = NumberFormat.getIntegerInstance().format(value.toLong())

    fun percent(value: Int): String = "$value%"

    // Trend/rising pulse cards show a signed delta; every other kind shows a
    // plain grade.
    fun metric(fact: ParadoxoPulseFact): String = when (fact.kind) {
        ParadoxoPulseKind.Trend, ParadoxoPulseKind.Rising -> signedGrade(fact.metric)
        else -> grade(fact.metric)
    }
}

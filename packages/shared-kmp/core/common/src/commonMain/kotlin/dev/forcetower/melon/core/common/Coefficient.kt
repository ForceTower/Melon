package dev.forcetower.melon.core.common

// What one discipline adds to the CR: its posted mean, or a 0 when SAGRES
// records a failure without one (reprovado por falta). Null while there is no
// verdict, so a class still running never reads as a 0.
fun coefficientGrade(
    mean: Double?,
    approved: Boolean?,
): Double? = mean ?: if (approved == false) 0.0 else null

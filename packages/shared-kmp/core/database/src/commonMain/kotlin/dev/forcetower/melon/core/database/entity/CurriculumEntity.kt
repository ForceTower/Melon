package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// The curriculum version ("matriz curricular") the student is bound to, fed by
// `GET /api/curriculum`. One row at a time: the payload always carries the
// single version the student is enrolled under, and the refresh replaces it
// wholesale. Everything else in the curriculum mirror cascades off this row.
//
// `asOf` is `YYYY-MM-DD` (the transcription date of the source document) so it
// sorts lexically and matches the wire format; the native side parses it at
// the use-case boundary.
@Entity(tableName = "Curriculum")
data class CurriculumEntity(
    @PrimaryKey val id: String,
    // SAGRES's own version identifier, a semester code: "20232".
    val code: String,
    // Verbatim upstream label, e.g. "BACHAREL E FORMAÇÃO DE PSICÓLOGO".
    val label: String,
    val asOf: String,
    val minPeriods: Int?,
    val maxPeriods: Int?,
    // The source document is old enough that required hours may have moved.
    val stale: Boolean,
)

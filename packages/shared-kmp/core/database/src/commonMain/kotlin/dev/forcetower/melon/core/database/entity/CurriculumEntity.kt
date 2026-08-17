package dev.forcetower.melon.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// One curriculum version ("matriz curricular") of the student's course, fed by
// `GET /api/curriculum`: every version on file lands here in the server's
// order (newest first), each scored against the student's own history so the
// picker can say which grid is theirs. The bound one is named by
// [CurriculumProgressEntity.curriculumId]; requirements, entries and edges
// only ever exist for that one, and the refresh replaces the whole set.
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
    // The newest version of the course.
    @ColumnInfo(defaultValue = "0") val current: Boolean = false,
    // The version that replaced this one, and the semester it took over. Null
    // on the current version.
    val supersededByCode: String? = null,
    val supersededByEffectiveFrom: String? = null,
    // How the student came to be bound to this version ("resolved" /
    // "manual" / "upstream"); null on every version they are not bound to.
    val source: String? = null,
    // The student's numbers against this version's own grid.
    val completedHours: Int? = null,
    val requiredHours: Int? = null,
    val percent: Double? = null,
    // 0…100: the share of the student's approved hours this version lists.
    val fit: Double? = null,
    // The server's order — newest first.
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
)

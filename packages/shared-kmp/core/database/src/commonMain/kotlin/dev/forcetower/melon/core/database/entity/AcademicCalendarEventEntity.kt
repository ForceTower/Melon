package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Academic-calendar feed mirror — fed by `GET /api/sync/events`. Server emits
// the canonical 90-day window (prior month start → next month end) every
// sync; the client replaces the table contents wholesale to honor server-side
// deletes.
//
// `start`/`end` are stored as `YYYY-MM-DD` strings so they sort lexically and
// align with the wire format. The native side parses them at the use-case
// boundary so view code works in Date/LocalDate.
@Entity(
    tableName = "AcademicCalendarEvent",
    indices = [Index(value = ["start"])],
)
data class AcademicCalendarEventEntity(
    @PrimaryKey val id: String,
    val platformId: String,
    val description: String,
    val start: String,
    val end: String?,
    val fixed: Boolean,
    val closed: Boolean,
    // GENERAL | FACULTY | COURSE | CLASS | CAMPUS
    val scope: String,
    // MANUAL | EVALUATION | FINAL_EXAM | SECOND_CALL | SECOND_EPOCH
    val origin: String,
)

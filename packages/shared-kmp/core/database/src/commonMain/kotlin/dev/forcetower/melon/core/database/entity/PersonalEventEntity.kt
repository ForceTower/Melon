package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Calendar entries the student created themselves. The only table here with no
// upstream counterpart — sync never writes it and never clears it, so the
// wholesale replace `CalendarEventDao` does for the academic feed must not
// reach these rows.
//
// `start`/`end` are `YYYY-MM-DD` like the academic feed, so both sort lexically
// and parse the same way at the use-case boundary. The discipline columns are
// denormalized on purpose: the picker only offers currently enrolled
// disciplines, and the chip has to keep reading right once that semester rolls
// over.
@Entity(
    tableName = "PersonalEvent",
    indices = [Index(value = ["start"])],
)
data class PersonalEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val start: String,
    val end: String?,
    // TASK | EXAM | STUDY | LIFE
    val category: String,
    val disciplineId: String?,
    val disciplineCode: String?,
    val disciplineName: String?,
    // Days before `start`; 0 means no reminder.
    val reminderDays: Int,
    val notes: String,
    // Epoch millis — ties the row order when two entries start the same day.
    val createdAt: Long,
)

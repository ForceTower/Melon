package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// One slot in the curriculum grid — a discipline the course expects, with the
// student's situation against it. Deliberately NOT joined to `Discipline`: the
// grid describes the course, not the student's enrolments, and slots the
// student has never touched have no discipline row to point at.
//
// `period` is null for the elective pool (not scheduled in any período), and
// `position` keeps the payload's own ordering so a período's entries stay
// grouped without a second sort key.
@Entity(
    tableName = "CurriculumEntry",
    primaryKeys = ["curriculumId", "code"],
    foreignKeys = [
        ForeignKey(
            entity = CurriculumEntity::class,
            parentColumns = ["id"],
            childColumns = ["curriculumId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("curriculumId"), Index("curriculumId", "period")],
)
data class CurriculumEntryEntity(
    val curriculumId: String,
    val code: String,
    // Arrives abbreviated and upper-case from the university system.
    val name: String,
    val hours: Int,
    val credits: Int?,
    val period: Int?,
    // Entries sharing a group must be taken in the same período.
    val coreqGroup: Int?,
    val requirementCode: String?,
    // completed | in_progress | withdrawn | failed | available | blocked | not_taken
    val status: String,
    val position: Int,
)

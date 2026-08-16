package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// One hour-type bucket ("natureza") of the curriculum with the student's
// progress against it — núcleo comum, estágios, optativas, atividades
// complementares. `position` preserves the order the portal listed them in,
// which is the order the screen renders.
@Entity(
    tableName = "CurriculumRequirement",
    primaryKeys = ["curriculumId", "code"],
    foreignKeys = [
        ForeignKey(
            entity = CurriculumEntity::class,
            parentColumns = ["id"],
            childColumns = ["curriculumId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("curriculumId")],
)
data class CurriculumRequirementEntity(
    val curriculumId: String,
    // Stable slug, e.g. "nucleo-comum".
    val code: String,
    // required | elective | complementary | internship | capstone | extension | other
    val kind: String,
    // The university's own pt-BR wording.
    val label: String,
    // Abbreviated for narrow rows; equals `label` when none was authored.
    val shortLabel: String,
    // First período this bucket appears in — explains a legitimate 0%.
    val startsAtPeriod: Int?,
    val hoursRequired: Int,
    val hoursCompleted: Int,
    // False when completion lives outside anything observable (atividades
    // complementares): the row reads "not counted yet", not "zero progress".
    val derivable: Boolean,
    val percent: Double?,
    val position: Int,
)

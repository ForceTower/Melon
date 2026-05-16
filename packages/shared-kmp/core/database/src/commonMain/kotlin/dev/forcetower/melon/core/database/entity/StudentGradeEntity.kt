package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "StudentGrade",
    foreignKeys = [
        ForeignKey(
            entity = StudentClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentClassId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ClassEvaluationEntity::class,
            parentColumns = ["id"],
            childColumns = ["evaluationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("studentClassId"), Index("evaluationId")],
)
data class StudentGradeEntity(
    @PrimaryKey val id: String,
    val studentClassId: String,
    val evaluationId: String,
    val platformId: String,
    val name: String,
    val nameShort: String?,
    val ordinal: Int,
    val weight: String,
    val value: String?,
    val date: String?,
)

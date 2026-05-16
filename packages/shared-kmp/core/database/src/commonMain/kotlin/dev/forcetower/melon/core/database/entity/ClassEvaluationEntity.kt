package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ClassEvaluation",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("classId")],
)
data class ClassEvaluationEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val platformId: String,
    val name: String?,
    val position: Int,
)

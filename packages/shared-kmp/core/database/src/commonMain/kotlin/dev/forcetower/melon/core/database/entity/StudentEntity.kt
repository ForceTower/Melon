package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Student",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("courseId")],
)
data class StudentEntity(
    @PrimaryKey val id: String,
    val platformId: Long,
    val name: String,
    val courseId: String?,
)

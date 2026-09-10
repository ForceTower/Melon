package dev.forcetower.melon.core.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
    // When the server last successfully fetched this student's data from
    // upstream. Stored as ISO-8601; nullable until the first completed sync.
    val lastSyncCompletedAt: String? = null,
)

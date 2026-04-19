package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Course")
data class CourseEntity(
    @PrimaryKey val id: String,
    val platformId: Long,
    val name: String,
    val resumedName: String,
)

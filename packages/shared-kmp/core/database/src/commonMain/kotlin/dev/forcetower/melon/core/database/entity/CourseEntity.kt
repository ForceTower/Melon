package dev.forcetower.melon.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "Course")
data class CourseEntity(
    @PrimaryKey val id: String,
    val platformId: Long,
    val name: String,
    val resumedName: String,
)

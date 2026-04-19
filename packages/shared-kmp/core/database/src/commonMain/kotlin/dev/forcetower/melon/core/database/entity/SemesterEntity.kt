package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Semester")
data class SemesterEntity(
    @PrimaryKey val id: String,
    val platformId: Long,
    val code: String,
    val description: String,
    val startDate: String,
    val endDate: String,
    val track: String?,
)

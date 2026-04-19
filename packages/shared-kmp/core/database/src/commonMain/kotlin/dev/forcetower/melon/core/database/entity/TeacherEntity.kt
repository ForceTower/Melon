package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Teacher")
data class TeacherEntity(
    @PrimaryKey val id: String,
    val platformId: Long,
    val name: String,
)

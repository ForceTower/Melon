package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Discipline")
data class DisciplineEntity(
    @PrimaryKey val id: String,
    val code: String,
    val platformId: Long?,
    val name: String,
    val hours: Int,
    val department: String?,
    val program: String?,
)

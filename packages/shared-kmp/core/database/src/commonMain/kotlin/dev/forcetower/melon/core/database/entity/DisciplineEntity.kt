package dev.forcetower.melon.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

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

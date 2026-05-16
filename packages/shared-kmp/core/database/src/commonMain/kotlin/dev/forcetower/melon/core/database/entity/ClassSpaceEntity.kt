package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ClassSpace")
data class ClassSpaceEntity(
    @PrimaryKey val id: String,
    val platformId: Long,
    val type: String?,
    val campus: String,
    val location: String,
    val modulo: String,
)

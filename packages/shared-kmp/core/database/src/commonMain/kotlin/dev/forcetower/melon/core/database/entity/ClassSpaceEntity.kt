package dev.forcetower.melon.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "ClassSpace")
data class ClassSpaceEntity(
    @PrimaryKey val id: String,
    val platformId: Long,
    val type: String?,
    val campus: String,
    val location: String,
    val modulo: String,
)

package com.forcetower.unes.core.model

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import java.util.*

@Entity(indices = [
    Index(value = ["sagres_id"], unique = true),
    Index(value = ["uuid"], unique = true)
])
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val name: String?,
    val email: String?,
    val score: Double = -1.0,
    val course: Long? = null,
    val imageUrl: String? = null,
    @ColumnInfo(name = "sagres_id")
    val sagresId: Long,
    val uuid: String = UUID.randomUUID().toString(),
    val me: Boolean = false
)
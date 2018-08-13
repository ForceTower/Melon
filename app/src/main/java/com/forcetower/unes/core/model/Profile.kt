package com.forcetower.unes.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val name: String?,
    val email: String?,
    val score: Double = -1.0,
    val course: Long? = null,
    val imageUrl: String? = null,
    val sagresId: Long,
    val uuid: String = UUID.randomUUID().toString()
)
package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EdgeAccessToken(
    val accessToken: String,
    @PrimaryKey(autoGenerate = false)
    val id: Int = 1
)

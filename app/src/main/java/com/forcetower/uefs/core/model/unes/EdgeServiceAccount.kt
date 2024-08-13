package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EdgeServiceAccount(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String?,
    val imageUrl: String?,
    val me: Boolean
)

package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UserSession(
    // This field is actually a UUID
    @PrimaryKey(autoGenerate = false)
    val uid: String,
    val started: Long,
    val lastInteraction: Long? = null,
    val synced: Boolean = false
)
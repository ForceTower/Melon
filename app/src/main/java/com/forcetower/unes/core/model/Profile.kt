package com.forcetower.unes.core.model

import androidx.annotation.Nullable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Profile(
        @PrimaryKey(autoGenerate = true)
        val uid: Long,
        @Nullable val name: String,
        @Nullable val email: String,
        val score: Double,
        val course: Long
)
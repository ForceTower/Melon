package com.forcetower.unes.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Access(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val username: String,
    val password: String
)
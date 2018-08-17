package com.forcetower.unes.core.model

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AccessToken(
    @PrimaryKey(autoGenerate = true)
    val uid: Int,
    @NonNull
    val type: String,
    @NonNull
    val token: String,
    @Nullable
    val refreshToken: String
)
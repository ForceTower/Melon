package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity
data class Account(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val name: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    val username: String,
    val email: String?,
    @SerializedName("dark_theme_enabled")
    val darkThemeEnabled: Boolean,
    @SerializedName("dark_theme_invites")
    val darkThemeInvites: Int
)
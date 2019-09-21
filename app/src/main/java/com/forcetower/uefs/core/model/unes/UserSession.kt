package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity
data class UserSession(
    // This field is actually a UUID
    @PrimaryKey(autoGenerate = false)
    val uid: String,
    @SerializedName("start_time")
    val started: Long,
    @SerializedName("last_interaction")
    val lastInteraction: Long? = null,
    val synced: Boolean = false,
    @SerializedName("ad_click")
    val clickedAd: Boolean = false,
    @SerializedName("ad_impression")
    val impressionAd: Boolean = false
)
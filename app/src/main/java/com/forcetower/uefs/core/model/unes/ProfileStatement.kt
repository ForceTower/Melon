package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import org.threeten.bp.ZonedDateTime

@Entity
data class ProfileStatement(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    @SerializedName("receiver_id")
    val receiverId: Long,
    @SerializedName("sender_id")
    val senderId: Long,
    @SerializedName("sender_name")
    val senderName: String?,
    @SerializedName("sender_picture")
    val senderPicture: String?,
    val text: String,
    val likes: Int,
    val approved: Boolean,
    @SerializedName("created_at")
    val createdAt: ZonedDateTime,
    @SerializedName("updated_at")
    val updatedAt: ZonedDateTime
)

package com.forcetower.uefs.core.model.edge.sync

import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

data class PublicPlatformMessage(
    @SerializedName("id")
    val id: String,
    @SerializedName("platformId")
    val platformId: Long,
    @SerializedName("content")
    val content: String,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("senderProfile")
    val senderProfile: Long,
    @SerializedName("senderName")
    val senderName: String,
    @SerializedName("discipline")
    val discipline: String?,
    @SerializedName("codeDiscipline")
    val codeDiscipline: String?,
    @SerializedName("html")
    val html: Boolean,
    @SerializedName("date")
    val date: ZonedDateTime,
    @SerializedName("attachmentName")
    val attachmentName: String?,
    @SerializedName("attachmentLink")
    val attachmentLink: String?,
)
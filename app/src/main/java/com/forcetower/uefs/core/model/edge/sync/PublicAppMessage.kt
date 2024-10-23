package com.forcetower.uefs.core.model.edge.sync

import com.google.gson.annotations.SerializedName
import java.time.OffsetDateTime

data class PublicAppMessage(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("imageUrl")
    val imageUrl: String?,
    @SerializedName("clickableLink")
    val clickableLink: String?,
    @SerializedName("createdAt")
    val createdAt: OffsetDateTime
)
package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

@Entity
data class EdgeAppMessage(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val clickableLink: String?,
    val createdAt: ZonedDateTime
)
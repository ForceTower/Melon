package com.forcetower.unes.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
data class SagresMessage(
        val message: String,
        val received: Long,
        val notified: Boolean,
        val sender: String,
        @ColumnInfo(name = "class_id") val classId: Long,
        @ColumnInfo(name = "class_received") val classReceived: String
) : Identifiable()
package com.forcetower.unes.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["sagres_id"], unique = true)])
data class Message(
        @PrimaryKey(autoGenerate = true)
        val uid: Long,
        val content: String,
        @ColumnInfo(name = "sagres_id")
        val sagresId: Long,
        val timestamp: Long,
        @ColumnInfo(name = "sender_profile")
        val senderProfile: Int,
        @ColumnInfo(name = "sender_name")
        val senderName: String
)
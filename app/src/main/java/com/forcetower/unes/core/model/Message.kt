package com.forcetower.unes.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.Message

@Entity(indices = [Index(value = ["sagres_id"], unique = true)])
data class Message(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val content: String,
    @ColumnInfo(name = "sagres_id")
    val sagresId: Long,
    val timestamp: Long,
    @ColumnInfo(name = "sender_profile")
    val senderProfile: Int,
    @ColumnInfo(name = "sender_name")
    val senderName: String,
    val notified: Boolean = false) {

    companion object {
        fun fromMessage(me: Message) = Message(content = me.message, sagresId = me.sagresId, senderName = me.senderName, senderProfile = me.senderProfile, timestamp = me.timeStampInMillis)
    }
}
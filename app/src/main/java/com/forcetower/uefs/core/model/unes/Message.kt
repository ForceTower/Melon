/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.core.model.unes

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.SagresMessage
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.service.NotificationCreator
import java.time.OffsetDateTime
import java.util.Locale
import java.util.UUID

@Entity(
    indices = [
        Index(value = ["hash_message"], unique = true),
        Index(value = ["sagres_id"], unique = true),
        Index(value = ["uuid"], unique = true)
    ]
)
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
    val senderName: String?,
    val notified: Boolean = false,
    val discipline: String? = null,
    val uuid: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "code_discipline")
    val codeDiscipline: String? = null,
    val html: Boolean = false,
    @ColumnInfo(name = "date_string")
    val dateString: String? = null,
    @ColumnInfo(name = "processing_time")
    val processingTime: Long? = null,
    @ColumnInfo(name = "hash_message")
    val hashMessage: Long? = null,
    val attachmentName: String? = null,
    val attachmentLink: String? = null
) {
    @Ignore
    var disciplineResume: String? = null

    companion object {
        fun fromMessage(me: SagresMessage, notified: Boolean) =
            Message(
                content = me.message ?: "",
                sagresId = me.sagresId,
                senderName = me.senderName,
                senderProfile = me.senderProfile,
                timestamp = if (me.isFromHtml) me.processingTime else me.timeStampInMillis,
                notified = notified,
                discipline = me.discipline,
                codeDiscipline = me.disciplineCode,
                html = me.isFromHtml,
                dateString = me.dateString,
                processingTime = me.processingTime,
                hashMessage = me.message?.toLowerCase(Locale.getDefault())?.trim().hashCode().toLong(),
                attachmentName = me.attachmentName,
                attachmentLink = me.attachmentLink
            ).apply { disciplineResume = me.objective }

        fun fromMessage(me: dev.forcetower.breaker.model.Message, notified: Boolean): Message {
            val timestamp = OffsetDateTime.parse(me.timestamp).toInstant().toEpochMilli()
            return Message(
                content = me.content.replace("\\n", "\n").replace("\\r", "\r"),
                sagresId = me.id,
                senderName = me.sender,
                senderProfile = me.senderType,
                timestamp = timestamp,
                notified = notified,
                html = false,
                processingTime = System.currentTimeMillis(),
                hashMessage = me.content.replace("\\n", "\n").toLowerCase(Locale.getDefault()).trim().hashCode().toLong(),
                discipline = me.discipline?.discipline,
                codeDiscipline = me.discipline?.code
            )
        }
    }
}

fun Message.notify(context: Context) {
    NotificationCreator.showSagresMessageNotification(this, context)
}

fun List<SagresMessage>?.defineInDatabase(database: UDatabase, notified: Boolean = false) {
    val values = this?.map { Message.fromMessage(it, notified) } ?: emptyList()
    database.messageDao().insertIgnoring(values)
}

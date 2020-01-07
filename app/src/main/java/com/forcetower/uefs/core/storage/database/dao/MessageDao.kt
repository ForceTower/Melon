/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.crashlytics.android.Crashlytics
import com.forcetower.uefs.core.model.unes.Message
import java.util.Locale

@Dao
abstract class MessageDao {

    fun insertIgnoring(messages: List<Message>) {
        updateOldMessages()
        for (message in messages) {
            val direct = getMessageByHashDirect(message.hashMessage)
            if (direct != null) {
                if (message.senderName != null) {
                    if (direct.senderName.isNullOrBlank()) {
                        updateSenderName(message.sagresId, message.senderName)
                    }
                }

                if (message.discipline != null) {
                    if (direct.discipline.isNullOrBlank()) {
                        updateDisciplineName(message.sagresId, message.discipline)
                    }
                }

                if (message.attachmentLink != null) {
                    updateAttachmentLink(message.sagresId, message.attachmentLink)
                }

                if (message.attachmentName != null) {
                    updateAttachmentName(message.sagresId, message.attachmentName)
                }

                if (message.html && direct.html) {
                    updateDateString(message.sagresId, message.dateString)
                }
            }
            val resume = message.disciplineResume?.trim()
            val code = message.codeDiscipline?.trim()
            if (!resume.isNullOrBlank() && !code.isNullOrBlank()) {
                updateDisciplineResume(code, resume)
            }
        }

        insertIgnore(messages)
    }

    @Query("SELECT * FROM Message WHERE hash_message = :hashMessage")
    protected abstract fun getMessageByHashDirect(hashMessage: Long?): Message?

    private fun updateOldMessages() {
        val messages = getAllUndefinedMessages()
        messages.forEach { message ->
            val hash = message.content.toLowerCase(Locale.getDefault()).trim().hashCode().toLong()
            val existing = getMessageByHashDirect(hash)
            if (existing == null) setMessageHash(message.uid, hash)
            else {
                deleteMessage(message.uid)
                Crashlytics.logException(Exception("Collision of messages ${existing.senderName} and ${message.codeDiscipline}"))
            }
        }
    }

    @Query("DELETE FROM Message WHERE uid = :uid")
    protected abstract fun deleteMessage(uid: Long)

    @Query("UPDATE Message SET hash_message = :hash WHERE uid = :uid")
    protected abstract fun setMessageHash(uid: Long, hash: Long)

    @Query("SELECT * FROM Message WHERE hash_message IS NULL")
    protected abstract fun getAllUndefinedMessages(): List<Message>

    @Query("UPDATE Message SET date_string = :dateString WHERE sagres_id = :sagresId")
    protected abstract fun updateDateString(sagresId: Long, dateString: String?)

    @Query("UPDATE Discipline SET resume = :resume WHERE LOWER(code) = LOWER(:code)")
    protected abstract fun updateDisciplineResume(code: String, resume: String)

    @Query("UPDATE Message SET discipline = :discipline WHERE sagres_id = :sagresId")
    protected abstract fun updateDisciplineName(sagresId: Long, discipline: String)

    @Query("UPDATE Message SET sender_name = :senderName WHERE sagres_id = :sagresId")
    protected abstract fun updateSenderName(sagresId: Long, senderName: String)

    @Query("UPDATE Message SET attachmentLink = :attachmentLink WHERE sagres_id = :sagresId")
    protected abstract fun updateAttachmentLink(sagresId: Long, attachmentLink: String)

    @Query("UPDATE Message SET attachmentName = :attachmentName WHERE sagres_id = :sagresId")
    protected abstract fun updateAttachmentName(sagresId: Long, attachmentName: String)

    @Insert(onConflict = IGNORE)
    protected abstract fun insertIgnore(messages: List<Message>)

    @Insert(onConflict = REPLACE)
    protected abstract fun insertReplace(messages: List<Message>)

    @Query("SELECT * FROM Message WHERE sagres_id = :sagresId")
    abstract fun getMessageDirect(sagresId: Long): Message?

    @Query("SELECT * FROM Message ORDER BY timestamp DESC")
    abstract fun getAllMessages(): LiveData<List<Message>>

    @Query("SELECT * FROM Message ORDER BY timestamp DESC LIMIT 1")
    abstract fun getLastMessage(): LiveData<Message?>

    @Query("SELECT * FROM Message ORDER BY timestamp DESC")
    abstract fun getAllMessagesPaged(): DataSource.Factory<Int, Message>

    @Query("SELECT * FROM Message WHERE notified = 0")
    abstract fun getNewMessages(): List<Message>

    @Query("UPDATE Message SET notified = 1")
    abstract fun setAllNotified()

    @Insert(onConflict = REPLACE)
    abstract fun insert(message: Message): Long

    @Query("DELETE FROM Message")
    abstract fun deleteAll()
}
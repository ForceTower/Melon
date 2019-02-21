/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.uefs.core.storage.eventdatabase.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.uefs.core.model.siecomp.ServerSession
import com.forcetower.uefs.core.model.siecomp.Session
import com.forcetower.uefs.core.model.siecomp.SessionSpeaker
import com.forcetower.uefs.core.model.siecomp.SessionStar
import com.forcetower.uefs.core.model.siecomp.SessionTag
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.core.model.siecomp.Tag
import com.forcetower.uefs.core.storage.eventdatabase.accessors.SessionWithData

@Dao
abstract class EventDao {
    @Insert(onConflict = REPLACE)
    abstract fun insert(session: Session): Long

    @Transaction
    @Query("SELECT * FROM Session WHERE day_id = :day")
    abstract fun getSessions(day: Int): LiveData<List<Session>>

    @Transaction
    @Query("SELECT * FROM Session WHERE day_id = :day ORDER BY start_time ASC")
    abstract fun getSessionsFromDay(day: Int): LiveData<List<SessionWithData>>

    @Transaction
    @Query("SELECT * FROM Session WHERE uid = :id")
    abstract fun getSessionWithId(id: Long): LiveData<SessionWithData>

    @Transaction
    open fun insertServerSessions(value: List<ServerSession>) {
        deleteTags()
        deleteSpeakers()

        value.forEach {
            insertTags(it.tags)
            insertSpeakers(it.speakers)
            val session = it.toSession()
            deleteIfPresent(session.uuid)
            val id = insert(session)

            it.tags.forEach { t ->
                assocTag(SessionTag(session = id, tag = t.uid))
            }

            it.speakers.forEach { s ->
                assocSpeaker(SessionSpeaker(session = id, speaker = s.uid))
            }
        }
    }

    @Query("DELETE FROM Tag")
    protected abstract fun deleteTags()

    @Query("DELETE FROM Speaker")
    abstract fun deleteSpeakers()

    @Query("SELECT t.* FROM Tag t INNER JOIN SessionTag st ON t.uid = st.tag_id WHERE st.session_id = :sessionId")
    protected abstract fun getSessionTagsDirect(sessionId: Long): List<Tag>

    @Query("SELECT s.* FROM Speaker s INNER JOIN SessionSpeaker ss ON s.uid = ss.speaker_id WHERE ss.session_id = :sessionId")
    protected abstract fun getSessionSpeakerDirect(sessionId: Long): List<Speaker>

    @Query("SELECT * FROM Session WHERE uuid = :uuid")
    protected abstract fun getSessionWithUUID(uuid: String): Session?

    @Insert(onConflict = IGNORE)
    protected abstract fun insertTags(tags: List<Tag>)

    @Insert(onConflict = IGNORE)
    protected abstract fun insertSpeakers(speakers: List<Speaker>)

    @Insert(onConflict = REPLACE)
    protected abstract fun assocTag(tagged: SessionTag)

    @Insert(onConflict = REPLACE)
    protected abstract fun assocSpeaker(speaker: SessionSpeaker)

    @Query("DELETE FROM Session WHERE uuid = :sessionUUID")
    protected abstract fun deleteIfPresent(sessionUUID: String)

    @Transaction
    @Query("SELECT * FROM Session")
    abstract fun getAllSessions(): LiveData<List<SessionWithData>>

    @Query("SELECT * FROM Speaker WHERE uid = :speakerId")
    abstract fun getSpeakerWithId(speakerId: Long): LiveData<Speaker>

    @Transaction
    open fun markSessionStar(sessionId: Long, star: Boolean) {
        if (star) {
            markStar(SessionStar(0, sessionId))
        } else {
            unstarSession(sessionId)
        }
    }

    @Query("DELETE FROM SessionStar WHERE session_id = :sessionId")
    abstract fun unstarSession(sessionId: Long)

    @Insert
    protected abstract fun markStar(star: SessionStar)
}
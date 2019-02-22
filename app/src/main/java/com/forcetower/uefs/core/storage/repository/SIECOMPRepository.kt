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

package com.forcetower.uefs.core.storage.repository

import androidx.lifecycle.LiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.siecomp.ServerSession
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.core.storage.eventdatabase.EventDatabase
import com.forcetower.uefs.core.storage.eventdatabase.accessors.SessionWithData
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SIECOMPRepository @Inject constructor(
    private val database: EventDatabase,
    private val executors: AppExecutors,
    private val service: UService
) {
    fun getSessionsFromDayLocal(day: Int) = database.eventDao().getSessionsFromDay(day)

    fun getAllSessions() =
            object : NetworkBoundResource<List<SessionWithData>, List<ServerSession>>(executors) {
                override fun loadFromDb() = database.eventDao().getAllSessions()
                override fun shouldFetch(it: List<SessionWithData>?) = true
                override fun createCall() = service.siecompSessions()
                override fun saveCallResult(value: List<ServerSession>) {
                    database.eventDao().insertServerSessions(value)
                }
            }.asLiveData()

    fun getSessionDetails(id: Long): LiveData<SessionWithData> {
        return database.eventDao().getSessionWithId(id)
    }

    fun getSpeaker(speakerId: Long): LiveData<Speaker> {
        return database.eventDao().getSpeakerWithId(speakerId)
    }

    fun markSessionStar(sessionId: Long, star: Boolean) {
        executors.diskIO().execute {
            database.eventDao().markSessionStar(sessionId, star)
        }
    }
}

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

package dev.forcetower.event.core.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.forcetower.uefs.core.model.unes.Event
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import kotlinx.coroutines.Dispatchers
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import javax.inject.Inject

class EventRepository @Inject constructor(
    private val database: UDatabase,
    private val service: UService
) {
    fun getEvent(eventId: Long): LiveData<Event> {
        return database.eventDao().get(eventId)
    }

    val events = liveData(Dispatchers.IO) {
        emitSource(database.eventDao().all())
        try {
            val result = service.events()
            val data = result.data
            if (data != null) {
                database.eventDao().insert(data)
            }
        } catch (error: Throwable) {
            Timber.i("An error ${error.message}")
        }
    }

    fun create(
        name: String,
        location: String,
        description: String,
        image: Uri,
        start: ZonedDateTime,
        end: ZonedDateTime,
        offeredBy: String,
        price: Double?,
        courseId: Int?,
        certificateHours: Int?
    ): LiveData<Long> = liveData(Dispatchers.IO) {
        val event = Event(
            0,
            name,
            description,
            image.toString(),
            "Seu nome aqui",
            1,
            offeredBy,
            start,
            end,
            location,
            price,
            certificateHours,
            courseId,
            featured = false,
            canModify = false,
            participating = false,
            approved = false,
            fakeTemp = true,
            createdAt = ZonedDateTime.now()
        )
        emit(database.eventDao().insertSingle(event))
    }
}
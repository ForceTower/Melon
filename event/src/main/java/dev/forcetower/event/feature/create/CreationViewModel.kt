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

package dev.forcetower.event.feature.create

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.Event
import dev.forcetower.event.core.repository.EventRepository
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.Calendar
import javax.inject.Inject

class CreationViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {
    private val utc: Calendar = Calendar.getInstance()
    var imageUri: Uri? = null
    var start: Long = utc.timeInMillis
    var end: Long = utc.timeInMillis
    var createdId: Long? = null
    var selectedCourse: Course? = null

    fun loadModel(eventId: Long): LiveData<Event> {
        return repository.getEvent(eventId)
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
        certificateHours: Int?,
        registerPage: String?
    ): LiveData<Long> {
        return repository.create(name, location, description, image, start, end, offeredBy, price, courseId, certificateHours, registerPage)
    }

    fun confirmCreate(id: Long) = viewModelScope.launch {
        repository.scheduleCreate(id)
    }
}

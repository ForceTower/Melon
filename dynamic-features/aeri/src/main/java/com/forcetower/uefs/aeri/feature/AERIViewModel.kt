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

package com.forcetower.uefs.aeri.feature

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.aeri.core.model.Announcement
import com.forcetower.uefs.aeri.core.storage.repository.AERIRepository
import javax.inject.Inject

class AERIViewModel @Inject constructor(
    private val repository: AERIRepository
) : ViewModel(), AnnouncementInteractor {
    val announcements = repository.getAnnouncements()

    private val _refreshing = MediatorLiveData<Boolean>()
    val refreshing: LiveData<Boolean>
        get() = _refreshing

    private val _announcementClick = MutableLiveData<Event<Announcement>>()
    val announcementClick: LiveData<Event<Announcement>>
        get() = _announcementClick

    override fun onAnnouncementClick(announcement: Announcement) {
        _announcementClick.value = Event(announcement)
    }

    fun onRefresh() {
        val fetchMessages = repository.refreshNewsAsync()
        _refreshing.value = true
        _refreshing.addSource(fetchMessages) {
            _refreshing.removeSource(fetchMessages)
            _refreshing.value = false
        }
    }
}

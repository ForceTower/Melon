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

package com.forcetower.uefs.aeri.core.storage.repository

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.forcetower.core.interfaces.notification.NotifyMessage
import com.forcetower.uefs.aeri.R
import com.forcetower.uefs.aeri.core.model.Announcement
import com.forcetower.uefs.aeri.core.storage.database.AERIDatabase
import com.google.android.play.core.splitcompat.SplitCompat
import dagger.Reusable
import dev.forcetower.oversee.Oversee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Reusable
class AERIRepository @Inject constructor(
    context: Context,
    private val database: AERIDatabase
) {
    private val notificationTitle: String
    init {
        val success = SplitCompat.install(context)
        notificationTitle = if (!success) {
            Timber.e("Failed to install split compat")
            "AERI"
        } else {
            context.getString(R.string.aeri_notification_title)
        }
    }

    fun refreshNewsAsync() = liveData(Dispatchers.IO) {
        refreshNews()
        database.news().markAllNotified()
        emit(true)
    }

    @WorkerThread
    suspend fun refreshNews() = withContext(Dispatchers.IO) {
        Oversee.initialize()
        val news = Oversee.instance.getAERINews().reversed()
        database.news().insert(news)
    }

    fun getAnnouncements(): LiveData<PagedList<Announcement>> {
        return LivePagedListBuilder(database.news().getAnnouncementsPaged(), 20).build()
    }

    suspend fun update(): Int {
        refreshNews()
        return 0
    }

    suspend fun getNotifyMessages(): List<NotifyMessage> = withContext(Dispatchers.IO) {
        val notify = database.news().getNewAnnouncements()
        database.news().markAllNotified()
        notify.map { NotifyMessage(notificationTitle, it.title, it.imageUrl, it.link) }
    }
}

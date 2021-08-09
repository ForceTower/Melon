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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.room.withTransaction
import com.forcetower.uefs.core.model.unes.Event
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.util.ImgurUploader
import com.google.android.play.core.splitcompat.SplitCompat
import dev.forcetower.event.core.work.CreateEventWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject

class EventRepository @Inject constructor(
    private val database: UDatabase,
    private val service: UService,
    private val context: Context,
    private val client: OkHttpClient
) {
    init {
        SplitCompat.install(context)
    }

    fun getEvent(eventId: Long): LiveData<Event> {
        return database.eventDao().get(eventId)
    }

    val events = liveData(Dispatchers.IO) {
        emitSource(database.eventDao().all())
        fetchEvents()
    }

    private suspend fun fetchEvents() {
        try {
            val result = service.events()
            val data = result.data
            if (data != null) {
                database.eventDao().insert(data)
                Timber.d("Information saved!")
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
        certificateHours: Int?,
        registerPage: String?
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
            createdAt = ZonedDateTime.now(),
            registerPage = registerPage
        )
        emit(database.eventDao().insertSingle(event))
    }

    suspend fun scheduleCreate(id: Long) {
        CreateEventWorker.createWorker(context, id)
        database.withTransaction {
            database.eventDao().clearTemps()
            database.eventDao().setSending(id, 1)
        }
    }

    suspend fun sendEvent(eventId: Long): Int {
        val event = database.eventDao().getDirect(eventId) ?: return -1

        val url = if (event.imageUrl.startsWith("http")) {
            event.imageUrl
        } else {
            // TODO make this a dependent work
            val uri = Uri.parse(event.imageUrl)
            val resolver = context.applicationContext.contentResolver
            val stream: InputStream
            try {
                stream = resolver.openInputStream(uri) ?: return -1
            } catch (exception: Throwable) {
                return -1
            }

            val image = BitmapFactory.decodeStream(stream)
            image ?: return -1
            val baos = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val data = baos.toByteArray()
            val encoded = Base64.encodeToString(data, Base64.DEFAULT)
            val upload = ImgurUploader.upload(client, encoded, "event-unes-${UUID.randomUUID().toString().substring(0, 4)}") ?: return 1
            withContext(Dispatchers.IO) {
                database.eventDao().updateImageUrl(eventId, upload.link)
            }
            Timber.d("After save...")
            upload.link
        }

        Timber.d("Using image link $url")
        return try {
            service.sendEvent(event.copy(imageUrl = url))
            Timber.d("set not sending anymore: exec")
            withContext(Dispatchers.IO) {
                database.eventDao().setSending(eventId, 0)
                fetchEvents()
            }
            Timber.d("set not sending anymore: completed")
            0
        } catch (error: Throwable) {
            1
        }
    }

    suspend fun approve(eventId: Long) {
        withContext(Dispatchers.IO) {
            try {
                service.approveEvent(eventId)
                database.eventDao().setApproved(eventId, true)
            } catch (error: Throwable) {
                Timber.e(error, "Failed to approve event")
            }
        }
    }

    suspend fun delete(eventId: Long) {
        withContext(Dispatchers.IO) {
            try {
                service.deleteEvent(eventId)
                database.eventDao().deleteSingle(eventId)
            } catch (error: Throwable) {
                Timber.e(error, "Failed to delete event")
            }
        }
    }
}

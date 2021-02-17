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

package com.forcetower.uefs.core.work.image

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.util.Base64
import androidx.annotation.WorkerThread
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.util.ImgurUploader
import com.forcetower.uefs.core.work.enqueue
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

@HiltWorker
class UploadImageToStorage @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val client: OkHttpClient,
    private val database: UDatabase,
    private val service: UService
) : Worker(context, params) {

    @SuppressLint("WrongThread")
    @WorkerThread
    override fun doWork(): Result {
        Timber.d("Started picture upload")
        val tUri = inputData.getString(URI) ?: return Result.failure()

        val uri = Uri.parse(tUri)

        val resolver = applicationContext.contentResolver
        val stream: InputStream
        try {
            stream = resolver.openInputStream(uri) ?: return Result.failure()
        } catch (exception: Throwable) {
            return Result.failure()
        }

        val image = BitmapFactory.decodeStream(stream)
        image ?: return Result.failure()

        val bitmap = ThumbnailUtils.extractThumbnail(image, 1080, 1080)

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        val name = database.accessDao().getAccessDirect()?.username ?: "user-unes-${UUID.randomUUID().toString().substring(0, 4)}"
        val encoded = Base64.encodeToString(data, Base64.DEFAULT)
        val upload = ImgurUploader.upload(client, encoded, name) ?: return Result.retry()

        return try {
            val response = service.updateProfileImage(upload).execute()
            if (response.isSuccessful) {
                Timber.d("Success setting on account!!")
                try {
                    val acc = service.getAccount().execute()
                    acc.body()?.run {
                        database.accountDao().insert(this)
                    }
                } catch (e: Throwable) { }
                Result.success()
            } else {
                Timber.d("Unsucessful response ${response.code()}")
                Result.retry()
            }
        } catch (t: Throwable) {
            Timber.e(t, "Error")
            Result.retry()
        }
    }

    companion object {
        private const val URI = "image_uri"
        private const val TAG = "upload_profile_image"

        fun createWorker(context: Context, uri: Uri) {
            val data = workDataOf(URI to uri.toString())
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            OneTimeWorkRequestBuilder<UploadImageToStorage>()
                .setInputData(data)
                .addTag(TAG)
                .setConstraints(constraints)
                .build()
                .enqueue(context)
        }
    }
}

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

package com.forcetower.uefs.core.work.image

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.util.Base64
import androidx.annotation.WorkerThread
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.util.ImgurUploader
import com.forcetower.uefs.core.work.enqueue
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

class UploadImageToStorage(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    @Inject
    lateinit var client: OkHttpClient
    @Inject
    lateinit var database: UDatabase
    @Inject
    lateinit var service: UService

    @SuppressLint("WrongThread")
    @WorkerThread
    override fun doWork(): Result {
        (applicationContext as UApplication).component.inject(this)
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
                Timber.d("Success!!")
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

        fun createWorker(uri: Uri) {
            val data = workDataOf(URI to uri.toString())
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            OneTimeWorkRequestBuilder<UploadImageToStorage>()
                .setInputData(data)
                .addTag(TAG)
                .setConstraints(constraints)
                .build()
                .enqueue()
        }
    }
}
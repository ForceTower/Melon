/*
 * Copyright (c) 2018.
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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.SystemClock
import androidx.core.graphics.scale
import androidx.work.*
import com.forcetower.uefs.core.work.enqueue
import com.google.firebase.storage.FirebaseStorage
import timber.log.Timber
import java.io.ByteArrayOutputStream

class UploadImageToStorage(
    context : Context, params : WorkerParameters
): Worker(context, params) {

    override fun doWork(): Result {
        Timber.d("Started picture upload")
        val tUri = inputData.getString(URI)?: return Result.FAILURE
        val tRef = inputData.getString(REFERENCE)?: return Result.FAILURE

        val uri = Uri.parse(tUri)
        val storage = FirebaseStorage.getInstance()
        val ref = storage.getReference(tRef)

        val resolver = applicationContext.contentResolver
        val image = BitmapFactory.decodeStream(resolver.openInputStream(uri))
        image?: return Result.FAILURE

        val bitmap = ThumbnailUtils.extractThumbnail(image, 450, 450)

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val data = baos.toByteArray()

        var completed = false
        var success = false

        ref.putBytes(data).addOnCompleteListener {task ->
            if (task.isSuccessful) {
                success = true
                Timber.d("Image Uploaded!")
            } else {
                Timber.d("Failed to upload image")
                Timber.d("Exception: ${task.exception?.message}")
            }
            completed = true
        }

        var count = 0
        while (!completed && count < 200) {
            SystemClock.sleep(1000)
            count++
        }

        return if (success) Result.SUCCESS else Result.RETRY
    }

    companion object {
        private const val URI       = "image_uri"
        private const val REFERENCE = "storage_reference"
        private const val TAG       = "upload_profile_image"

        fun createWorker(uri: Uri, reference: String) {
            val data = workDataOf(URI to uri.toString(), REFERENCE to reference)
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            OneTimeWorkRequestBuilder<UploadImageToStorage>()
                    .setInputData(data)
                    .setConstraints(constraints)
                    .build()
                    .enqueue()
        }
    }
}
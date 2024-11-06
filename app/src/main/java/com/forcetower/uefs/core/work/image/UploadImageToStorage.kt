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

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.forcetower.uefs.core.work.enqueue
import com.forcetower.uefs.domain.usecase.account.ChangeProfilePictureUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class UploadImageToStorage @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val useCase: ChangeProfilePictureUseCase
) : CoroutineWorker(context, params) {
    @WorkerThread
    override suspend fun doWork(): Result {
        Timber.d("Started picture upload")
        val tUri = inputData.getString(URI) ?: return Result.failure()

        val uri = Uri.parse(tUri)

        return try {
            useCase(uri)
            Result.success()
        } catch (t: Throwable) {
            Timber.e(t, "Error uploading image")
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

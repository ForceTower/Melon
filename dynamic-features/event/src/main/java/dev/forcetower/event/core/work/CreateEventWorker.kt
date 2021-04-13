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

package dev.forcetower.event.core.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.forcetower.uefs.core.injection.dependencies.EventModuleDependencies
import com.forcetower.uefs.core.work.enqueueUnique
import dagger.hilt.android.EntryPointAccessors
import dev.forcetower.event.core.injection.DaggerEventComponent
import dev.forcetower.event.core.repository.EventRepository
import timber.log.Timber
import javax.inject.Inject

class CreateEventWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    @Inject
    lateinit var repository: EventRepository

    override suspend fun doWork(): Result {
        DaggerEventComponent.builder()
            .context(context)
            .dependencies(
                EntryPointAccessors.fromApplication(
                    applicationContext,
                    EventModuleDependencies::class.java
                )
            )
            .build()
            .inject(this)

        val eventId = inputData.getLong(EVENT_ID, 0)
        if (eventId == 0L) return Result.failure()

        val result = repository.sendEvent(eventId)
        return when {
            result == 0 -> Result.success()
            result > 0 -> Result.retry()
            else -> Result.failure()
        }
    }

    companion object {
        private const val EVENT_ID = "event_id"
        private const val TAG = "create-event-on-cloud"

        fun createWorker(context: Context, id: Long) {
            val data = workDataOf(EVENT_ID to id)
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            OneTimeWorkRequestBuilder<CreateEventWorker>()
                .setInputData(data)
                .addTag(TAG)
                .setConstraints(constraints)
                .build()
                .enqueueUnique(context, "$TAG-$id", true)
            Timber.d("Scheduled create worker")
        }
    }
}

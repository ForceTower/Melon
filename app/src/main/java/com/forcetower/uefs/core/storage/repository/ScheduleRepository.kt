/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.core.storage.repository

import androidx.annotation.WorkerThread
import com.crashlytics.android.Crashlytics
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.storage.database.UDatabase
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    @Named(Profile.COLLECTION)
    private val collection: CollectionReference,
    private val database: UDatabase
) {

    @WorkerThread
    fun saveSchedule(userId: String) {
        val schedule = database.classLocationDao().getCurrentScheduleDirect()
        val semester = database.semesterDao().getSemestersDirect().maxBy { it.sagresId }
        if (schedule.isEmpty() || semester == null) {
            Timber.d("It's too late to apologize")
            return
        }

        val reference = collection.document(userId)
                .collection("schedule")
                .document(semester.sagresId.toString())

        val mapped = mapOf(
            "locations" to schedule
        )

        try {
            Tasks.await(reference.set(mapped, SetOptions.merge()))
        } catch (t: Throwable) {
            Crashlytics.logException(t)
        }
    }
}
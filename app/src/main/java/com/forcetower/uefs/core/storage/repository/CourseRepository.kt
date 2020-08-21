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

package com.forcetower.uefs.core.storage.repository

import android.content.Context
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.network.adapter.asLiveData
import com.forcetower.uefs.core.storage.resource.NetworkBoundResource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val context: Context,
    private val gson: Gson,
    private val database: UDatabase,
    private val executors: AppExecutors,
    private val service: UService
) {
    fun getCourses() = object : NetworkBoundResource<List<Course>, List<Course>>(executors) {
        override fun loadFromDb() = database.courseDao().selectAll()
        override fun shouldFetch(it: List<Course>?) = true
        override fun createCall() = service.getCourses().asLiveData()
        override fun saveCallResult(value: List<Course>) = database.courseDao().insert(value)
        override fun onErrorCallback() {
            val direct = database.courseDao().selectAllDirect()
            if (direct.isNotEmpty()) return

            try {
                val stream = context.assets.open("courses.json")
                val size = stream.available()
                val buffer = ByteArray(size)
                stream.read(buffer)
                stream.close()
                val json = String(buffer, Charset.forName("UTF-8"))
                val type = object : TypeToken<List<Course>>() { }.type
                val list = gson.fromJson<List<Course>>(json, type)
                database.courseDao().insert(list)
            } catch (error: Throwable) {
                Timber.e(error, "Failed parsing json...")
            }
        }
    }.asLiveData()
}
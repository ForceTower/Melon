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
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.task.UCaseResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val context: Context,
    private val gson: Gson,
    private val database: UDatabase,
    private val service: UService
) {
    suspend fun loadInitialCourses() {
        val loaded = loadLocalCourses()
        if (!loaded) downloadCourses()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun loadLocalCourses(): Boolean {
        val count = database.courseDao().count()
        if (count > 0) return true
        return try {
            val stream = context.assets.open("courses.json")
            val size = stream.available()
            val buffer = ByteArray(size)
            stream.read(buffer)
            stream.close()
            val json = String(buffer, Charset.forName("UTF-8"))
            val type = object : TypeToken<List<Course>>() { }.type
            val list = gson.fromJson<List<Course>>(json, type)
            database.courseDao().insert(list)
            true
        } catch (error: Throwable) {
            Timber.e(error, "Failed parsing json...")
            false
        }
    }

    suspend fun downloadCourses(): List<Course> {
        val courses = service.getCourses()
        database.courseDao().insert(courses)
        return courses
    }

    suspend fun getCoursesDirectly() = database.courseDao().selectAllDirect()

    fun getCourses(): Flow<UCaseResult<List<Course>>> {
        return flow {
            emit(UCaseResult.Loading)
            val count = database.courseDao().count()
            if (count == 0) loadLocalCourses()

            val snapshot = database.courseDao().selectAllDirect()
            if (snapshot.isNotEmpty()) emit(UCaseResult.Success(snapshot))

            downloadCourses()

            val source = database.courseDao().selectAll().map { UCaseResult.Success(it) }
            emitAll(source)
        }
    }
}

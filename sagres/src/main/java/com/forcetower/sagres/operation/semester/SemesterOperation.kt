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

package com.forcetower.sagres.operation.semester

import com.forcetower.sagres.database.model.SSemester
import com.forcetower.sagres.operation.Dumb
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.request.SagresCalls
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.util.concurrent.Executor

class SemesterOperation(executor: Executor?, private val userId: Long) : Operation<SemesterCallback>(executor) {
    init {
        this.perform()
    }

    override fun execute() {
        val call = SagresCalls.getSemesters(userId)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body!!.string()
                successMeasures(body)
            } else {
                publishProgress(SemesterCallback(Status.NETWORK_ERROR).code(response.code).message(response.message))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            publishProgress(SemesterCallback(Status.NETWORK_ERROR).throwable(e))
        }
    }

    private fun successMeasures(body: String) {
        val type = object : TypeToken<Dumb<MutableList<SSemester>>>() {}.type
        try {
            val dSemesters = gson.fromJson<Dumb<MutableList<SSemester>>>(body, type)
            val semesters = dSemesters.items
            semesters.forEach {
                it.name = it.name.trim()
                it.codename = it.codename.trim()
                it.endClasses = it.endClasses.trim()
                it.end = it.end.trim()
                it.startClasses = it.startClasses.trim()
                it.start = it.start.trim()
            }

            val callback = SemesterCallback(Status.SUCCESS).semesters(semesters)
            this.finished = callback
            this.success = true

            publishProgress(callback)
        } catch (t: Throwable) {
            val callback = SemesterCallback(Status.UNKNOWN_FAILURE).message(t.message)
            this.finished = callback
            this.success = false
            publishProgress(callback)
        }
    }
}

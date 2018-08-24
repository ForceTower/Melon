/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

class SemesterOperation(private val userId: Long, executor: Executor): Operation<SemesterCallback>(executor) {
    init {
        this.perform()
    }

    override fun execute() {
        val call = SagresCalls.getSemesters(userId)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()!!.string()
                successMeasures(body)
            } else {
                result.postValue(SemesterCallback(Status.NETWORK_ERROR).code(response.code()).message(response.message()))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            result.postValue(SemesterCallback(Status.NETWORK_ERROR).throwable(e))
        }
    }

    private fun successMeasures(body: String) {
        val type = object: TypeToken<Dumb<MutableList<SSemester>>>(){}.type
        val dSemesters = gson.fromJson<Dumb<MutableList<SSemester>>>(body, type)
        val semesters = dSemesters.items

        val callback = SemesterCallback(Status.SUCCESS).semesters(semesters)
        this.finished = callback
        this.success = true

        result.postValue(callback)
    }
}

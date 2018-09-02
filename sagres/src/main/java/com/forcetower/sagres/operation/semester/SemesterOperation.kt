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

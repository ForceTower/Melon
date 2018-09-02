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

package com.forcetower.sagres.operation.person

import com.forcetower.sagres.database.model.SPerson
import com.forcetower.sagres.operation.Operation
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.request.SagresCalls

import java.io.IOException
import java.util.concurrent.Executor

class PersonOperation(private val userId: Long?, executor: Executor?) : Operation<PersonCallback>(executor) {
    init {
        this.perform()
    }

    override fun execute() {
        result.postValue(PersonCallback(Status.STARTED))
        val call = if (userId == null) SagresCalls.me else SagresCalls.getPerson(userId)
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body()!!.string()
                val user = gson.fromJson(body, SPerson::class.java)
                successMeasures(user)
            } else {
                result.postValue(PersonCallback(Status.RESPONSE_FAILED).code(response.code()).message(response.message()))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            result.postValue(PersonCallback(Status.NETWORK_ERROR).throwable(e))
        }

    }

    private fun successMeasures(user: SPerson) {
        result.postValue(PersonCallback(Status.SUCCESS).person(user))
    }
}
